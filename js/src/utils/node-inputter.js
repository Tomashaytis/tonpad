import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";

export class NodeInputter {
    static handleInputInSpec(view, from, to, text) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const notationBlock = this.findParentNotationBlock($from);
        if (!notationBlock) return false;

        const blockType = notationBlock.attrs.type;

        const originalSpecContent = notationBlock.child(0).textContent;
        const contentNode = notationBlock.child(1);
        const contentText = contentNode.textContent;

        let cursorOffset = $from.parentOffset;
        const newSpecContent =
            originalSpecContent.slice(0, cursorOffset) +
            text +
            originalSpecContent.slice(cursorOffset);

        let fullText = newSpecContent + contentText;

        let paragraph = NodeConverter.constructParagraph(fullText);
        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;

        let tr = state.tr.replaceWith(blockPos, blockPos + blockSize, paragraph);

        const reconstructor = new NodeReconstructor();
        const blockResult = reconstructor.applyBlockRules([paragraph], blockPos, from + cursorOffset);
        let reconstructedNode = blockResult.paragraphs[0];

        const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
        if (markReconstruction) {
            reconstructedNode = markReconstruction;
        }

        tr = tr.replaceWith(blockPos, blockPos + paragraph.nodeSize, reconstructedNode);

        if (reconstructedNode.type.name == 'notation_block') {
            cursorOffset += blockResult.blocksBeforeCursor;
        }

        const cursorPos = blockPos + cursorOffset + text.length + 1;
        tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);
        return true;
    }

    static handleInputInNormalNode(view, from, to, text) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;
        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        const newText = node.textContent.slice(0, $from.parentOffset) +
            text +
            node.textContent.slice($from.parentOffset);

        let newNode = node.type.create(node.attrs, markdownSchema.text(newText));

        const reconstructor = new NodeReconstructor();

        const blockResult = reconstructor.applyBlockRules([newNode], nodePos);
        newNode = blockResult.paragraphs[0];

        const markReconstruction = reconstructor.reconstructMarksInNode(newNode);
        if (markReconstruction) {
            newNode = markReconstruction;
        }

        if (newNode !== node) {
            let tr = state.tr.replaceWith(nodePos, nodePos + nodeSize, newNode);

            const oldSize = node.nodeSize;
            const newSize = newNode.nodeSize;
            const sizeDiff = newSize - oldSize;

            let cursorPos = $from.pos + sizeDiff;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

            dispatch(tr);
            return true;
        }

        return false;
    }

    static handleDeleteChar(view, from, to) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;
        const textContent = node.textContent;
        let cursorOffset = $from.parentOffset;

        if (cursorOffset === 0) {
            return false;
        }

        if (node.type.name === 'spec_block') {
            return this.handleDeleteInSpec(view, from, to);
        }
        const newText = textContent.slice(0, cursorOffset - 1) + textContent.slice(cursorOffset);

        let newNode;

        if (node.type.name === 'paragraph') {
            newNode = NodeConverter.constructParagraph(newText);
        } else {
            newNode = node.type.create(node.attrs, newText ? markdownSchema.text(newText) : null);
        }

        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        const reconstructor = new NodeReconstructor();

        if (node.type.name === 'paragraph') {
            const blockResult = reconstructor.applyBlockRules([newNode], nodePos);
            newNode = blockResult.paragraphs[0];
        }

        const markReconstruction = reconstructor.reconstructMarksInNode(newNode);
        if (markReconstruction) {
            newNode = markReconstruction;
        }

        let tr = state.tr.replaceWith(nodePos, nodePos + nodeSize, newNode);

        if (newNode.type.name == 'notation_block') {
            cursorOffset += 1;
        }

        const cursorPos = nodePos + cursorOffset;
        tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);
        return true;
    }

    static handleDeleteInSpec(view, from, to) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const notationBlock = this.findParentNotationBlock($from);
        if (!notationBlock) return false;

        const originalSpecContent = notationBlock.child(0).textContent;
        const nodeContent = notationBlock.child(1).textContent;

        let cursorOffset = $from.parentOffset;

        if (cursorOffset === 0) {
            return false;
        }

        const newSpecContent =
            originalSpecContent.slice(0, cursorOffset - 1) +
            originalSpecContent.slice(cursorOffset);

        const fullText = newSpecContent + nodeContent;
        let paragraph = NodeConverter.constructParagraph(fullText);

        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;

        let tr = state.tr.replaceWith(blockPos, blockPos + blockSize, paragraph);

        const reconstructor = new NodeReconstructor();
        const blockResult = reconstructor.applyBlockRules([paragraph], blockPos);
        let reconstructedNode = blockResult.paragraphs[0];

        const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
        if (markReconstruction) {
            reconstructedNode = markReconstruction;
        }

        tr = tr.replaceWith(blockPos, blockPos + paragraph.nodeSize, reconstructedNode);

        if (reconstructedNode.type.name == 'notation_block') {
            cursorOffset += 1;
        }

        const cursorPos = blockPos + cursorOffset;
        tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);
        return true;
    }

    static findParentNotationBlock($pos) {
        for (let depth = $pos.depth; depth >= 0; depth--) {
            const node = $pos.node(depth);
            if (node && node.type.name === 'notation_block') {
                return node;
            }
        }
        return null;
    }
}