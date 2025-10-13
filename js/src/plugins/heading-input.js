import { Plugin } from "prosemirror-state";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeSearch } from "../utils/node-search";

export function headingInputPlugin() {
    let view = null;

    return new Plugin({
        props: {
            handleTextInput: (view, from, to, text) => {
                const { state } = view;
                const $from = state.doc.resolve(from);

                if (!view) view = view;
                return handleSpecInput(view, $from, text, from);
            }
        },

        view: () => ({
            update: (v) => { view = v; }
        })
    });
}

function handleSpecInput(view, $from, text, from) {
    if (!view) return false;

    const { state, dispatch } = view;
    if ($from.parent.type.name !== "heading_spec") return false;

    const currentText = $from.parent.textContent.slice(0, -1);
    const { node: notationBlock, pos: notationBlockPos } = NodeSearch.getParent($from);

    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const originalCursorPos = $from.pos;
    const cursorOffset = from;
    const textPos = $from.parentOffset;

    if (/^#+/.test(text) && textPos <= headingNode.attrs.level) {
        const newText = text + currentText;

        if (newText.length > 6) {
            return convertToParagraph(state, dispatch, notationBlock, notationBlockPos, originalCursorPos, text, cursorOffset);
        }

        const updatedHeading = state.schema.nodes.heading.create(
            { level: newText.length },
            state.schema.text(headingNode.textContent)
        );

        const newHeadingBlock = NodeConverter.constructHeading(updatedHeading);

        let tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            newHeadingBlock
        );

        const cursorPos = notationBlockPos + textPos + text.length + 2;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
    if (text[0] === ' ' && textPos >= headingNode.attrs.level) {
        const updatedHeadingNode = state.schema.nodes.heading.create(
            { level: headingNode.attrs.level },
            state.schema.text(text + headingNode.textContent)
        );

        const updatedHeadingBlock = NodeConverter.constructHeading(updatedHeadingNode);

        let tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            updatedHeadingBlock
        );

        const cursorPos = notationBlockPos + headingSpecNode.nodeSize + 2 + text.length;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }
    if (textPos === headingNode.attrs.level + 1) {
        const updatedHeadingNode = state.schema.nodes.heading.create(
            { level: headingNode.attrs.level },
            state.schema.text(text + headingNode.textContent)
        );

        const updatedHeadingBlock = NodeConverter.constructHeading(updatedHeadingNode);

        let tr = state.tr.replaceWith(
            notationBlockPos,
            notationBlockPos + notationBlock.nodeSize,
            updatedHeadingBlock
        );

        const cursorPos = notationBlockPos + headingSpecNode.nodeSize + 2 + text.length;
        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    return convertToParagraph(state, dispatch, notationBlock, notationBlockPos, originalCursorPos, text, cursorOffset);
}

function convertToParagraph(state, dispatch, notationBlock, notationBlockPos, originalCursorPos, text, cursorOffset) {
    const headingSpecNode = notationBlock.child(0);
    const headingNode = notationBlock.child(1);

    const offset = cursorOffset - notationBlockPos - 2;

    const combinedText = headingSpecNode.textContent.slice(0, offset) + text + headingSpecNode.textContent.slice(offset) + headingNode.textContent;
    const newParagraph = state.schema.nodes.paragraph.create(
        null,
        state.schema.text(combinedText)
    );

    let tr = state.tr.replaceWith(
        notationBlockPos,
        notationBlockPos + notationBlock.nodeSize,
        newParagraph
    );

    const cursorPos = originalCursorPos;
    dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));

    return true;
}