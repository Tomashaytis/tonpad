import { Plugin } from "prosemirror-state";
import { NodeConverter } from "../utils/node-converter";
import { NodeSearch } from "../utils/node-search";

export function deletePlugin() {
    return new Plugin({
        props: {
            handleKeyDown(view, event) {
                if (event.key === "Delete") {
                    const { state, dispatch } = view;
                    const { $from, empty } = state.selection;

                    if (!empty) return false;

                    let currentNode = NodeSearch.getCurrent($from).node;
                    const currentNodeType = currentNode.type.name;

                    const isLineEnd = ($from.parentOffset === $from.parent.textContent.length);

                    if (currentNodeType === "paragraph") {
                        if (isLineEnd) {
                            return handleParagraphToHeadingMerge(event, state, dispatch, $from);
                        } else if (currentNode.textContent.length !== 0) {
                            const newText = currentNode.textContent.slice(0, $from.parentOffset) + currentNode.textContent.slice($from.parentOffset + 1);

                            let isHandled = false;
                            if (/^(#{1,6})\s/.test(newText)) {
                                handleHeadingPattern(state, dispatch, $from, newText);
                                isHandled = true;
                            }
                            return isHandled;
                        }
                        return false;
                    }

                    if (currentNodeType === "heading_spec") {
                        return handleHeadingSpecBackspace(event, state, dispatch, $from);
                    }

                    if (currentNodeType === "heading" && isLineEnd) {
                        return handleHeadingBackspace(event, state, dispatch, $from);
                    }

                    if (currentNodeType === "notation_block" && $from.index() === 1) {
                        switch (currentNodeType.attrs.type) {
                            case "heading":
                                return handleHeadingSpecBackspace(event, state, dispatch, $from);
                            default:
                                return false;
                        }
                    }
                }
                return false;
            }
        }
    });
}

function handleHeadingBackspace(event, state, dispatch, $from) {
    const { parentOffset } = $from;
    const { node: notationBlock } = NodeSearch.getParent($from);

    event.preventDefault();

    const headingNode = notationBlock.child(1);

    const headingText = headingNode.textContent;

    if (parentOffset === headingText.length) {

        event.preventDefault();
        const nextNode = NodeSearch.findNext($from, 0).node;

        if (nextNode) {
            if (nextNode.type.name === "paragraph") {
                return handleHeadingToParagraphMerge(state, dispatch, $from, nextNode);
            }

            if (nextNode.type.name === "notation_block" && nextNode.attrs.type === "heading") {
                return handleHeadingToHeadingMerge(state, dispatch, $from, nextNode);
            }
        }


        return false;
    }
    return true;
}

function handleHeadingSpecBackspace(event, state, dispatch, $from) {
    const { parent, parentOffset } = $from;

    const headingSpecNode = parent;
    const headingSpecText = headingSpecNode.textContent;
    const { node, pos } = NodeSearch.getParent($from);
    const notationBlock = node;
    const notationBlockPos = pos;
    const headingNode = notationBlock.child(1);
    const headingText = headingNode.textContent;

    if (parentOffset === headingSpecText.length) {
        const newHeading = state.schema.nodes.heading.create(
            { level: headingNode.attrs.level },
            headingText.slice(1) ? state.schema.text(headingText.slice(1)) : null
        );

        const newHeadingBlock = NodeConverter.constructHeading(newHeading);

        const tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            newHeadingBlock
        );

        const cursorPos = notationBlockPos + headingSpecNode.nodeSize;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

        return true;
    }

    const beforeText = headingSpecText.slice(0, parentOffset);
    const afterText = headingSpecText.slice(parentOffset + 1);
    const deletedChar = headingSpecText[parentOffset];

    event.preventDefault();

    if (deletedChar === ' ') {
        if (headingText.length > 0 && headingText[0] === ' ') {
            const updatedHeading = state.schema.nodes.heading.create(
                { level: headingNode.attrs.level },
                headingText.slice(1) ? state.schema.text(headingText.slice(1)) : null
            );

            const newHeadingBlock = NodeConverter.constructHeading(updatedHeading);

            const tr = state.tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newHeadingBlock
            );

            const cursorPos = notationBlockPos + beforeText.length + 2;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;

        } else {
            const combinedText = beforeText + afterText + headingText;
            const newParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(combinedText)
            );

            const tr = state.tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newParagraph
            );

            const cursorPos = notationBlockPos + beforeText.length + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;
        }
    }

    if (deletedChar === '#') {
        const newLevel = beforeText.length + afterText.length - 1;

        if (newLevel === 0) {
            const combinedText = afterText + headingText;
            const newParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(combinedText)
            );

            const tr = state.tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newParagraph
            );

            const cursorPos = notationBlockPos + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;

        } else {
            const updatedHeading = state.schema.nodes.heading.create(
                { level: newLevel },
                headingText ? state.schema.text(headingText) : null
            );

            const newHeadingBlock = NodeConverter.constructHeading(updatedHeading);

            const tr = state.tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newHeadingBlock
            );

            const cursorPos = notationBlockPos + 2 + beforeText.length;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;
        }
    }

    return false;
}

function handleParagraphToHeadingMerge(event, state, dispatch, $from) {
    const currentIndex = $from.index(0);

    const doc = $from.node(0);
    const nextNode = doc.child(currentIndex + 1);

    if (nextNode && nextNode.type.name === "notation_block" && nextNode.attrs.type === "heading") {
        event.preventDefault();

        const paragraphNode = $from.parent;

        const notationBlock = nextNode;
        const paragraphPos = $from.before(1);

        const headingSpecNode = notationBlock.child(0);
        const headingNode = notationBlock.child(1);

        const paragraphText = paragraphNode.textContent;
        const headingSpecText = headingSpecNode.textContent;
        const headingText = headingNode.textContent;

        const combinedText = paragraphText + headingSpecText + headingText;

        const newParagraph = state.schema.nodes.paragraph.create(
            null,
            state.schema.text(combinedText)
        );

        const tr = state.tr.replaceWith(
            paragraphPos,
            paragraphPos + paragraphNode.nodeSize + notationBlock.nodeSize,
            newParagraph
        );

        const cursorPos = paragraphPos + paragraphNode.nodeSize - 1;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    if (nextNode.type.name === "paragraph") {
        event.preventDefault();

        const paragraphNode = $from.parent;
        const paragraphNodePos = $from.before(1);
        const paragraphText = paragraphNode.textContent;

        const nextParagraphNode = nextNode;
        const nextParagraphText = nextParagraphNode.textContent;

        const complexText = paragraphText + nextParagraphText;

        const match = complexText.match(/^(#{1,6})\s(.*)/);

        if (!match)
            return false

        const newHeading = state.schema.nodes.heading.create(
            { level: match[1].length },
            match[2] ? state.schema.text(match[2]) : null
        );

        const headingBlock = NodeConverter.constructHeading(newHeading);

        const tr = state.tr.replaceWith(
            paragraphNodePos,
            paragraphNodePos + paragraphNode.nodeSize + nextParagraphNode.nodeSize,
            headingBlock
        );

        const cursorPos = paragraphNodePos + paragraphNode.nodeSize;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
}

function handleHeadingToParagraphMerge(state, dispatch, $from, nextParagraph) {
    const { node: notationBlock, pos: notationBlockPos } = NodeSearch.getParent($from);
    const nextParagraphPos = NodeSearch.findNext($from, 0).pos;

    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const headingText = headingNode.textContent;
    const nextParagraphText = nextParagraph.textContent;

    let tr = state.tr;

    if (nextParagraphText !== "") {
        const newHeadingText = headingText + nextParagraphText;
        const newHeading = state.schema.nodes.heading.create(
            { level: headingNode.attrs.level },
            state.schema.text(newHeadingText)
        );

        const newHeadingBlock = NodeConverter.constructHeading(newHeading);

        tr = tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            newHeadingBlock
        );

        tr = tr.delete(nextParagraphPos, nextParagraphPos + nextParagraph.nodeSize);

        const cursorPos = nextParagraphPos - 2;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
    } else {
        tr = tr.delete(nextParagraphPos, nextParagraphPos + nextParagraph.nodeSize);

        const cursorPos = nextParagraphPos - 2;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
    }
    return true;
}

function handleHeadingToHeadingMerge(state, dispatch, $from, nextNotationBlock) {
    const { node: notationBlock, pos: notationBlockPos } = NodeSearch.getParent($from);

    const headingNode = notationBlock.child(1);

    const headingText = headingNode.textContent;

    const nextHeadingSpecNode = nextNotationBlock.child(0);
    const nextHeadingNode = nextNotationBlock.child(1);

    const nextHeadingSpecText = nextHeadingSpecNode.textContent;
    const nextHeadingText = nextHeadingNode.textContent;

    let tr = state.tr;

    const newHeading = state.schema.nodes.heading.create(
        { level: headingNode.attrs.level },
        state.schema.text(headingText + nextHeadingSpecText + nextHeadingText)
    );

    const newHeadingBlock = NodeConverter.constructHeading(newHeading);

    tr = tr.replaceWith(
        notationBlockPos,
        notationBlockPos + notationBlock.nodeSize + nextNotationBlock.nodeSize,
        newHeadingBlock
    );

    const cursorPos = notationBlockPos + notationBlock.nodeSize - 2;
    dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

    return true;
}


function handleHeadingPattern(state, dispatch, $from, paragraphText) {
    const headingMatch = paragraphText.match(/^(#{1,6})\s(.*)$/);

    if (headingMatch) {
        const level = headingMatch[1].length;
        const headingText = headingMatch[2];

        const headingNode = state.schema.nodes.heading.create(
            { level: level },
            headingText ? state.schema.text(headingText) : null
        );

        const headingBlock = NodeConverter.constructHeading(headingNode);
        const paragraphPos = $from.before(1);

        let tr = state.tr.replaceWith(
            paragraphPos,
            paragraphPos + $from.parent.nodeSize,
            headingBlock
        );

        const cursorPos = $from.parentOffset + 2;

        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    return false;
}