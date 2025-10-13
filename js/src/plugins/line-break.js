import { Plugin } from "prosemirror-state";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeSearch } from "../utils/node-search.js";

export function lineBreakPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: (view, event) => {
                if (event.key === "Enter") {
                    const { state, dispatch } = view;
                    const { $from, $to } = state.selection;

                    const currentNode = $from.parent;
                    if (currentNode.type.name === "paragraph")
                        return false;

                    const { node: notationBlock, pos: notationBlockPos } = NodeSearch.getParent($from);

                    if (notationBlock) {
                        const containerType = notationBlock.attrs.type;

                        switch (containerType) {
                            case "heading":
                                return handleHeadingBlock(event, state, dispatch, $from, $to, notationBlock, notationBlockPos);
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

function handleHeadingBlock(event, state, dispatch, $from, $to, notationBlock, notationBlockPos) {
    if ($from.parent.type.name === "heading")
        return handleHeadingEnter(event, state, dispatch, $from, $to, notationBlock, notationBlockPos);

    if ($from.parent.type.name === "heading_spec")
        return handleHeadingSpecEnter(event, state, dispatch, $from, $to, notationBlock, notationBlockPos);

    return false
}

function handleHeadingEnter(event, state, dispatch, $from, $to, notationBlock, notationBlockPos) {
    if ($from.parent.type.name !== "heading") return false;

    event.preventDefault();

    if ($from.pos === $to.pos) {
        const headingText = $from.parent.textContent;
        const beforeText = headingText.slice(0, $from.parentOffset);
        const afterText = headingText.slice($from.parentOffset);

        const newParagraph = state.schema.nodes.paragraph.create(
            null,
            afterText ? state.schema.text(afterText) : null
        );

        const updatedHeading = state.schema.nodes.heading.create(
            { level: $from.parent.attrs.level },
            beforeText ? state.schema.text(beforeText) : null
        );

        const headingBlock = NodeConverter.constructHeading(updatedHeading)

        let tr = state.tr;

        tr = tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            headingBlock
        );

        const paragraphPos = notationBlockPos + headingBlock.nodeSize;
        tr = tr.insert(paragraphPos, newParagraph);

        const cursorPos = paragraphPos + 1;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
    return false;
}

function handleHeadingSpecEnter(event, state, dispatch, $from, $to, notationBlock, notationBlockPos) {
    if ($from.parent.type.name !== "heading_spec") return false;

    event.preventDefault();

    if ($from.pos === $to.pos) {
        const headingSpecText = $from.parent.textContent;
        const beforeText = headingSpecText.slice(0, $from.parentOffset);
        const afterText = headingSpecText.slice($from.parentOffset);

        const headingNode = notationBlock.child(1);
        const headingText = headingNode.textContent;

        if (beforeText.includes(" ")) {
            const updatedHeading = state.schema.nodes.heading.create(
                { level: $from.parent.attrs.level }
            );

            const headingBlock = NodeConverter.constructHeading(updatedHeading)

            let tr = state.tr;

            tr = tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                headingBlock
            );

            const newParagraph = state.schema.nodes.paragraph.create(
                null,
                headingText ? state.schema.text(headingText) : null
            );

            const paragraphPos = notationBlockPos + headingBlock.nodeSize;
            tr = tr.insert(paragraphPos, newParagraph);

            const cursorPos = paragraphPos + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;

        } else if (beforeText === "") {
            const newParagraph = state.schema.nodes.paragraph.create(
                null,
                null
            );

            const updatedHeading = state.schema.nodes.heading.create(
                { level: $from.parent.attrs.level },
                state.schema.text(headingText)
            );

            const newHeadingBlock = NodeConverter.constructHeading(updatedHeading)

            let tr = state.tr;

            tr = tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                newParagraph
            );

            const headingBlockPos = notationBlockPos + newParagraph.nodeSize;
            tr = tr.insert(headingBlockPos, newHeadingBlock);

            const cursorPos = headingBlockPos + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;

        } else if (beforeText === "#".repeat($from.parent.attrs.level)) {
            const beforeParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(beforeText)
            );

            const afterParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(" " + headingText)
            );

            let tr = state.tr;

            tr = tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                beforeParagraph
            );

            const afterParagraphPos = notationBlockPos + beforeParagraph.nodeSize;
            tr = tr.insert(afterParagraphPos, afterParagraph);

            const cursorPos = afterParagraphPos + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;
        } else {
            const beforeParagraph = state.schema.nodes.paragraph.create(
                null,
                state.schema.text(beforeText)
            );

            let tr = state.tr;

            tr = tr.replaceWith(
                notationBlockPos,
                notationBlockPos + notationBlock.nodeSize,
                beforeParagraph
            );

            const afterHeading = state.schema.nodes.heading.create(
                { level: $from.parent.attrs.level - beforeText.length },
                state.schema.text(headingText)
            );

            const afterHeadingBlock = NodeConverter.constructHeading(afterHeading)

            const headingBlockPos = notationBlockPos + beforeParagraph.nodeSize;
            tr = tr.insert(headingBlockPos, afterHeadingBlock);

            const cursorPos = headingBlockPos + 1;
            dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
            return true;
        }
    }
    return false;
}