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

                    const isParentFirst = $from.index(0) === 0;
                    const isNodeFirst = $from.index($from.depth - 1) === 0;
                    const isAtStart = $from.parentOffset === 0;

                    if (isParentFirst && isNodeFirst && isAtStart) {
                        event.preventDefault();
                        return true;
                    }

                    const isLineStart = ($from.parentOffset === 0 || currentNode.textContent === "");

                    if (currentNodeType === "paragraph" && isLineStart) {
                        return handleParagraphToHeadingMerge(event, state, dispatch, $from)
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

    if (/^(#{1,6})\s/.test(headingText)) {
        const match = complexText.match(/^(#{1,6})\s/);
        if (match) {
            const newHeading = state.schema.nodes.heading.create(
                { level: match[1].length },
                state.schema.text(complexText.slice(match[1].length + 1))
            );

            const newHeadingBlock = NodeConverter.constructHeading(newHeading);

            const tr = state.tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newHeadingBlock
            );

            const cursorPos = notationBlockPos + headingSpecNode.nodeSize - 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

        } else {
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

            const cursorPos = notationBlockPos + headingSpecText.length + 2;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

            return true;
        }

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
    let headingSpecNode, headingSpecText, notationBlock, notationBlockPos, headingNode, headingText;
    let beforeText, afterText, deletedChar;

    const { parent, parentOffset } = $from;
    if (parentOffset === 0) {

        event.preventDefault();
        const prevNode = NodeSearch.findPrevious($from, 0).node;

        if (prevNode && prevNode.type.name === "paragraph") {
            return handleHeadingToParagraphMerge(state, dispatch, $from, prevNode);
        }
        return false;
    }

    headingSpecNode = parent;
    headingSpecText = parent.textContent;
    const { node, pos } = NodeSearch.getParent($from);
    notationBlock = node;
    notationBlockPos = pos;
    headingNode = notationBlock.child(1);
    headingText = headingNode.textContent;

    beforeText = headingSpecText.slice(0, parentOffset - 1);
    afterText = headingSpecText.slice(parentOffset);
    deletedChar = headingSpecText[parentOffset - 1];

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
        const newLevel = beforeText.length;

        if (newLevel === 0) {
            const combinedText = ' ' + afterText + headingText;
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

function handleHeadingToParagraphMerge(state, dispatch, $from, prevParagraph) {
    const notationBlock = $from.node($from.depth - 1);
    const notationBlockPos = $from.before($from.depth - 1);
    const prevParagraphPos = $from.before(1) - prevParagraph.nodeSize;

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
            const newParagraphText = prevParagraph.textContent + headingSpecText + headingText;
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

function handleParagraphToHeadingMerge(event, state, dispatch, $from,) {
    const currentIndex = $from.index(0);

    if (currentIndex > 0) {
        const doc = $from.node(0);
        const prevNode = doc.child(currentIndex - 1);

        if (prevNode && prevNode.type.name === "notation_block") {
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
    }
    return false;
}