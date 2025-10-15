import { Plugin } from "prosemirror-state";
import { NodeConverter } from "../utils/node-converter";
import { NodeSearch } from "../utils/node-search";

export function backspacePlugin() {
    return new Plugin({
        props: {
            handleKeyDown(view, event) {
                if (event.key === "Backspace") {
                    const { state, dispatch } = view;
                    const { $from, empty } = state.selection;

                    if (!empty) return false;

                    let currentNode = NodeSearch.getCurrent($from).node;
                    const currentNodeType = currentNode.type.name;

                    const isParentFirst = $from.index(0) === 0;
                    const isNodeFirst = $from.index($from.depth - 1) === 0;
                    const isAtStart = $from.parentOffset === 0;

                    if (isParentFirst && isNodeFirst && isAtStart) {
                        event.preventDefault();
                        return true;
                    }

                    const isLineStart = ($from.parentOffset === 0 || currentNode.textContent === "");

                    if (currentNodeType === "paragraph") {
                        if (isLineStart) {
                            return handleParagraphToHeadingMerge(event, state, dispatch, $from);
                        } else if (currentNode.textContent.length !== 0) {
                            const newText = currentNode.textContent.slice(0, $from.parentOffset - 1) + currentNode.textContent.slice($from.parentOffset);

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

                    if (currentNodeType === "heading" && isLineStart) {
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
    const { node: notationBlock, pos: notationBlockPos } = NodeSearch.getParent($from);

    event.preventDefault();

    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const headingSpecText = headingSpecNode.textContent.slice(0, -1);
    const headingText = headingNode.textContent;
    const complexText = headingSpecText + headingText;

    const match = complexText.match(/^(#{1,6})\s(.*)/);
    if (match) {
        const newHeading = state.schema.nodes.heading.create(
            { level: match[1].length },
            match[2] ? state.schema.text(match[2]) : null
        );

        const newHeadingBlock = NodeConverter.constructHeading(newHeading);

        const tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            newHeadingBlock
        );

        const cursorPos = notationBlockPos + headingSpecNode.nodeSize - 1;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

        return true;

    } else {
        const combinedText = headingSpecText + headingText;

        const newParagraph = state.schema.nodes.paragraph.create(
            null,
            state.schema.text(combinedText)
        );

        const tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            newParagraph
        );

        const cursorPos = notationBlockPos + 1 + headingSpecText.length;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
}

function handleHeadingSpecBackspace(event, state, dispatch, $from) {
    const { parent, parentOffset } = $from;
    if (parentOffset === 0) {

        event.preventDefault();
        const prevNode = NodeSearch.findPrevious($from, 0).node;

        if (prevNode) {
            if (prevNode && prevNode.type.name === "paragraph") {
                return handleHeadingToParagraphMerge(state, dispatch, $from, prevNode);
            }
            if (prevNode && prevNode.type.name === "notation_block" && prevNode.attrs.type === "heading") {
                return handleHeadingToHeadingMerge(state, dispatch, $from, prevNode);
            }
        }
        return false;
    }

    const headingSpecNode = parent;
    const headingSpecText = headingSpecNode.textContent;
    const { node, pos } = NodeSearch.getParent($from);
    const notationBlock = node;
    const notationBlockPos = pos;
    const headingNode = notationBlock.child(1);
    const headingText = headingNode.textContent;

    const beforeText = headingSpecText.slice(0, parentOffset - 1);
    const afterText = headingSpecText.slice(parentOffset);
    const deletedChar = headingSpecText[parentOffset - 1];

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
    const prevNode = NodeSearch.findPrevious($from, 0).node;

    if (prevNode && prevNode.type.name === "notation_block" && prevNode.attrs.type === "heading") {
        event.preventDefault();

        const paragraphNode = $from.parent;

        const notationBlock = prevNode;
        const notationBlockPos = $from.before(1) - prevNode.nodeSize;
        const paragraphPos = $from.before(1);

        const headingNode = notationBlock.child(1);

        const paragraphText = paragraphNode.textContent;
        const headingText = headingNode.textContent;

        const combinedText = headingText + paragraphText;

        const newHeading = state.schema.nodes.heading.create(
            { level: headingNode.attrs.level },
            state.schema.text(combinedText)
        );

        const headingBlock = NodeConverter.constructHeading(newHeading);

        const tr = state.tr.replaceWith(
            notationBlockPos,
            paragraphPos + paragraphNode.nodeSize,
            headingBlock
        );

        const cursorPos = notationBlockPos + notationBlock.nodeSize - 2;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    if (prevNode.type.name === "paragraph") {
        event.preventDefault();

        const prevParagraphNode = prevNode;
        const prevParagraphNodePos = $from.before(1) - prevNode.nodeSize;
        const prevParagraphText = prevParagraphNode.textContent;

        const paragraphNode = $from.parent;
        const paragraphPos = $from.before(1);

        const paragraphText = paragraphNode.textContent;

        const complexText = prevParagraphText + paragraphText;

        const match = complexText.match(/^(#{1,6})\s/);

        if (!match)
            return false

        const headingText = paragraphText.slice(1);

        const newHeading = state.schema.nodes.heading.create(
            { level: match[1].length },
            state.schema.text(headingText)
        );

        const headingBlock = NodeConverter.constructHeading(newHeading);

        const tr = state.tr.replaceWith(
            prevParagraphNodePos,
            paragraphPos + paragraphNode.nodeSize,
            headingBlock
        );

        const cursorPos = prevParagraphNodePos + prevParagraphNode.nodeSize;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
    return false;
}

function handleHeadingToParagraphMerge(state, dispatch, $from, prevParagraph) {
    const {node: notationBlock, pos: notationBlockPos} = NodeSearch.getParent($from);
    const prevParagraphPos = NodeSearch.findPrevious($from, 0).pos;

    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const headingSpecText = headingSpecNode.textContent;
    const headingText = headingNode.textContent;
    const prevParagraphText = prevParagraph.textContent;

    let tr = state.tr;

    if (prevParagraphText !== "") {
        if (/^#+/.test(prevParagraphText) && prevParagraphText.length + headingNode.attrs.level <= 6) {
            const newHeading = state.schema.nodes.heading.create(
                { level: prevParagraphText.length + headingNode.attrs.level },
                state.schema.text(headingText)
            );

            const newHeadingBlock = NodeConverter.constructHeading(newHeading);

            tr = tr.replaceWith(
                prevParagraphPos,
                notationBlockPos + notationBlock.nodeSize,
                newHeadingBlock
            );

            const cursorPos = prevParagraphPos + prevParagraph.nodeSize;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

        } else {
            const newParagraphText = prevParagraphText + headingSpecText + headingText;
            const newParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(newParagraphText)
            );

            tr = tr.replaceWith(
                prevParagraphPos,
                notationBlockPos + notationBlock.nodeSize,
                newParagraph
            );

            const cursorPos = prevParagraphPos + prevParagraph.nodeSize - 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        }
    } else {
        tr = tr.delete(prevParagraphPos, prevParagraphPos + prevParagraph.nodeSize);

        const cursorPos = notationBlockPos - prevParagraph.nodeSize;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
    }
    return true;
}

function handleHeadingToHeadingMerge(state, dispatch, $from, prevNotationBlock) {
    const notationBlock = NodeSearch.getParent($from).node;
    const prevNotationBlockPos = NodeSearch.findPrevious($from, 0).pos;

    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const headingSpecText = headingSpecNode.textContent;
    const headingText = headingNode.textContent;

    const prevHeadingNode = prevNotationBlock.child(1);

    const prevHeadingText = prevHeadingNode.textContent;

    let tr = state.tr;

    const newHeading = state.schema.nodes.heading.create(
        { level: prevHeadingNode.attrs.level },
        state.schema.text(prevHeadingText + headingSpecText + headingText)
    );

    const newHeadingBlock = NodeConverter.constructHeading(newHeading);

    tr = tr.replaceWith(
        prevNotationBlockPos,
        prevNotationBlockPos + prevNotationBlock.nodeSize + notationBlock.nodeSize,
        newHeadingBlock
    );

    const cursorPos = prevNotationBlockPos + prevNotationBlock.nodeSize - 2;
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

        const cursorPos = $from.parentOffset + 1;

        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    return false;
}