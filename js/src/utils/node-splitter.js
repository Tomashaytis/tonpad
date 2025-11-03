import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";
import { TextSelection } from "prosemirror-state";

export class NodeSplitter {
    static handleEnter(view, event) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        if ($from.parent.type.name === 'spec_block') {
            return this.handleEnterInSpec(view, event);
        }

        const notationBlock = this.findParentNotationBlock($from);
        if (notationBlock && notationBlock.attrs.layout === 'row') {
            return this.handleEnterInNotationBlock(view, event, notationBlock);
        }

        return this.handleNormalEnter(view, event);
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

    static handleEnterInSpec(view, event) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const notationBlock = this.findParentNotationBlock($from);
        if (!notationBlock) return false;

        event.preventDefault();

        const paragraph = NodeConverter.destructNode(notationBlock)[0];
        const fullText = paragraph.textContent;
        const cursorOffset = $from.parentOffset;

        const beforeText = fullText.slice(0, cursorOffset);
        const afterText = fullText.slice(cursorOffset);

        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;
        let offset = 1;

        let newNodes = [];

        let upperNode
        let lowerNode;
        if (notationBlock.attrs.type === 'blockquote' && notationBlock.child(1).textContent.length !== 0) {
            upperNode = NodeConverter.constructParagraph(beforeText);
            lowerNode = NodeConverter.constructBlockquote(afterText);
            offset += lowerNode.child(0).nodeSize - 1;
            newNodes.push(upperNode);
        } else if (notationBlock.attrs.type === 'tab_list' && notationBlock.child(1).textContent.length !== 0) {
            upperNode = NodeConverter.constructParagraph(beforeText);
            lowerNode = NodeConverter.constructTabListItem(afterText, notationBlock.attrs.level);
            offset += lowerNode.child(0).nodeSize - 1;
            newNodes.push(upperNode);
        } else if (notationBlock.attrs.type === 'bullet_list' && notationBlock.child(1).textContent.length !== 0) {
            upperNode = NodeConverter.constructParagraph(beforeText);
            lowerNode = NodeConverter.constructBulletListItem(afterText, notationBlock.attrs.level, notationBlock.child(1).attrs.marker);
            offset += lowerNode.child(0).nodeSize - 1;
            newNodes.push(upperNode);
        } else if (notationBlock.attrs.type === 'ordered_list' && notationBlock.child(1).textContent.length !== 0) {
            upperNode = NodeConverter.constructParagraph(beforeText);
            lowerNode = NodeConverter.constructOrderedListItem(afterText, notationBlock.attrs.level, notationBlock.child(1).attrs.number + 1);
            offset += lowerNode.child(0).nodeSize - 1;
            newNodes.push(upperNode);
        } else {
            if (!['blockquote', 'tab_list', 'bullet_list', 'ordered_list'].includes(notationBlock.attrs.type)) {
                upperNode = NodeConverter.constructParagraph(beforeText);
                newNodes.push(upperNode);
            }
            lowerNode = NodeConverter.constructParagraph(afterText);
        }
        newNodes.push(lowerNode);

        let tr = state.tr;
        const reconstructedNodes = this.applyReconstruction(newNodes);
        tr = tr.replaceWith(blockPos, blockPos + blockSize, reconstructedNodes);

        if (reconstructedNodes.length > 1) {
            const cursorPos = blockPos + reconstructedNodes[0].nodeSize + offset;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));
        } else if (reconstructedNodes.length === 1) {
            const cursorPos = blockPos + reconstructedNodes[0].nodeSize - 1;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));
        }

        dispatch(tr);
        return true;
    }

    static handleNormalEnter(view, event) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        event.preventDefault();

        const node = $from.parent;
        const nodePos = $from.before();
        const nodeSize = node.nodeSize;
        const cursorOffset = $from.parentOffset;

        const textContent = node.textContent;
        const beforeText = textContent.slice(0, cursorOffset);
        const afterText = textContent.slice(cursorOffset);

        let newNodes = [];

        let upperNode, lowerNode;

        if (beforeText) {
            upperNode = node.type.create(node.attrs, markdownSchema.text(beforeText));
        } else {
            upperNode = NodeConverter.constructParagraph();
        }
        newNodes.push(upperNode);

        if (afterText) {
            lowerNode = node.type.create(node.attrs, markdownSchema.text(afterText));
        } else {
            lowerNode = NodeConverter.constructParagraph();
        }
        newNodes.push(lowerNode);

        let tr = state.tr;
        const reconstructedNodes = this.applyReconstruction(newNodes);
        tr = tr.replaceWith(nodePos, nodePos + nodeSize, reconstructedNodes);

        if (reconstructedNodes.length > 1) {
            const cursorPos = nodePos + reconstructedNodes[0].nodeSize + 1;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));
        }

        dispatch(tr);
        return true;
    }

    static handleEnterInNotationBlock(view, event, notationBlock) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        event.preventDefault();

        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;
        const cursorOffset = $from.parentOffset;

        const nodeSpecContent = notationBlock.child(0).textContent;
        const nodeContent = notationBlock.child(1).textContent;
        const beforeText = nodeContent.slice(0, cursorOffset);
        const afterText = nodeContent.slice(cursorOffset);
        let offset = 1;

        let newNodes = [];

        const upperNode = NodeConverter.constructParagraph(nodeSpecContent + beforeText);

        newNodes.push(upperNode);

        let lowerNode;
        if (notationBlock.attrs.type === 'blockquote') {
            lowerNode = NodeConverter.constructBlockquote(afterText);
            offset += lowerNode.child(0).nodeSize - 1;
        } else if (notationBlock.attrs.type === 'tab_list') {
            lowerNode = NodeConverter.constructTabListItem(afterText, notationBlock.attrs.level);
            offset += lowerNode.child(0).nodeSize - 1;
        } else if (notationBlock.attrs.type === 'bullet_list') {
            lowerNode = NodeConverter.constructBulletListItem(afterText, notationBlock.attrs.level, notationBlock.child(1).attrs.marker);
            offset += lowerNode.child(0).nodeSize - 1;
        } else if (notationBlock.attrs.type === 'ordered_list') {
            lowerNode = NodeConverter.constructOrderedListItem(afterText, notationBlock.attrs.level, notationBlock.child(1).attrs.number + 1);
            offset += lowerNode.child(0).nodeSize - 1;
        } else {
            lowerNode = NodeConverter.constructParagraph(afterText);
        }
        newNodes.push(lowerNode);

        let tr = state.tr;
        const reconstructedNodes = this.applyReconstruction(newNodes);
        tr = tr.replaceWith(blockPos, blockPos + blockSize, reconstructedNodes);

        const cursorPos = blockPos + (beforeText ? reconstructedNodes[0].nodeSize : nodeSpecContent.length + 4) + offset;
        tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);
        return true;
    }

    static applyReconstruction(nodes) {
        const reconstructor = new NodeReconstructor();
        let reconstructedNodes = [];

        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];

            const blockResult = reconstructor.applyBlockRules([node], 0);
            let reconstructedNode = blockResult.paragraphs[0];

            const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
            if (markReconstruction) {
                reconstructedNode = markReconstruction;
            }

            reconstructedNodes.push(reconstructedNode);
        }

        return reconstructedNodes;
    }
}