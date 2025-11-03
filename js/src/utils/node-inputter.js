import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";
import { TextSelection } from "prosemirror-state";

export class NodeInputter {
    static handleInputInSpec(view, from, to, text) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;
        const notationBlock = this.findParentNotationBlock($from);
        if (!notationBlock) return false;

        const { specContent, nodeContent } = NodeConverter.extractNotationBlockRowText(notationBlock);

        let cursorOffset = $from.parentOffset;

        let marksCheck = {
            text: text,
            offset: 0
        }

        if (cursorOffset === notationBlock.child(0).nodeSize - 2) {
            marksCheck = this.checkMarks(text, $from);
            marksCheck.offset += 1;
        }

        const newSpecContent =
            specContent.slice(0, cursorOffset) +
            marksCheck.text +
            specContent.slice(cursorOffset);

        let fullText = newSpecContent + nodeContent;

        let paragraph = NodeConverter.constructParagraph(fullText);
        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;

        const reconstructor = new NodeReconstructor();
        const blockResult = reconstructor.applyBlockRules([paragraph], blockPos, from + cursorOffset);
        let reconstructedNode = blockResult.paragraphs[0];

        const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
        if (markReconstruction) {
            reconstructedNode = markReconstruction;
        }

        let tr = state.tr.replaceWith(blockPos, blockPos + blockSize, reconstructedNode);

        if (cursorOffset === notationBlock.child(0).nodeSize - 2) {
            cursorOffset += 1
        }

        if (reconstructedNode.type.name == 'notation_block') {
            cursorOffset += 1;
        }

        const cursorPos = blockPos + cursorOffset + marksCheck.offset + 2;
        const curSelection = TextSelection.create(tr.doc, cursorPos);
        tr = tr.setSelection(curSelection);

        dispatch(tr);
        return true;
    }

    static handleInputInNormalNode(view, from, to, text) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;

        const marksCheck = this.checkMarks(text, $from);

        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        const newText = node.textContent.slice(0, $from.parentOffset) +
            marksCheck.text +
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
            const tr = state.tr.replaceWith(nodePos, nodePos + nodeSize, newNode);

            const oldSize = node.nodeSize;
            const newSize = newNode.nodeSize;
            const sizeDiff = newSize - oldSize;

            const cursorPos = $from.pos + sizeDiff + marksCheck.offset;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

            dispatch(tr);
            return true;
        }

        return false;
    }

    static handleDeleteChar(view, from, to, forward = true) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;
        const textContent = node.textContent;
        let cursorOffset = $from.parentOffset;

        if (cursorOffset === 0 && forward || cursorOffset === node.nodeSize - 2 && !forward) {
            return false;
        }

        if (node.type.name === 'spec_block') {
            return this.handleDeleteInSpec(view, from, to, forward);
        }

        let newText;
        if (forward) {
            newText = textContent.slice(0, cursorOffset - 1) + textContent.slice(cursorOffset);
        } else {
            newText = textContent.slice(0, cursorOffset) + textContent.slice(cursorOffset + 1);
            cursorOffset += 1;
        }

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

        if (forward && cursorOffset === 1) {
            const parent = $from.node($from.depth - 1);
            if (parent && parent.type.name === 'notation_block') {
                const childIndex = $from.indexAfter();
                if (childIndex === 1) {
                    if (parent.child(0).type.name === 'spec_block') {
                        cursorOffset -= 2;
                    }
                }
            }
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
        const curSelection = TextSelection.create(tr.doc, cursorPos);
        tr.setSelection(curSelection);

        dispatch(tr);
        return true;
    }

    static handleDeleteInSpec(view, from, to, forward = true) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;
        let tr = state.tr;

        const notationBlock = this.findParentNotationBlock($from);
        if (!notationBlock) return false;

        const { specContent, nodeContent } = NodeConverter.extractNotationBlockRowText(notationBlock);

        let cursorOffset = $from.parentOffset;

        if (cursorOffset === 0 && forward || cursorOffset === notationBlock.child(0).nodeSize - 2 && !forward) {
            return false;
        }

        let newSpecContent;
        if (forward) {
            newSpecContent = specContent.slice(0, cursorOffset - 1) + specContent.slice(cursorOffset);
        } else {
            newSpecContent = specContent.slice(0, cursorOffset) + specContent.slice(cursorOffset + 1);
            cursorOffset += 1;
        }

        const fullText = newSpecContent + nodeContent;
        let paragraph = NodeConverter.constructParagraph(fullText);

        const blockPos = $from.before($from.depth - 1);
        const blockSize = notationBlock.nodeSize;

        const reconstructor = new NodeReconstructor();
        const blockResult = reconstructor.applyBlockRules([paragraph], blockPos);
        let reconstructedNode = blockResult.paragraphs[0];

        const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
        if (markReconstruction) {
            reconstructedNode = markReconstruction;
        }

        tr = tr.replaceWith(blockPos, blockPos + blockSize, reconstructedNode);

        if (cursorOffset === notationBlock.child(0).nodeSize - 2 && forward && ['bullet_list', 'ordered_list'].includes(notationBlock.attrs.type)) {
            cursorOffset += 2
        }

        if (reconstructedNode.type.name == 'notation_block') {
            cursorOffset += blockResult.blocksBeforeCursor + 1;
        }

        const cursorPos = blockPos + cursorOffset;
        const curSelection = TextSelection.create(tr.doc, cursorPos);
        tr.setSelection(curSelection);

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

    static checkMarks(text, $from) {
        if (!$from.parent.isTextblock) return null;

        const parent = $from.parent;
        const textBefore = parent.textBetween(0, $from.parentOffset);
        const textAfter = parent.textBetween($from.parentOffset, parent.nodeSize - 2);

        const markRules = [
            { pattern: /\*\*$/, leftDelimiter: "**", rightDelimiter: "**" },
            { pattern: /\*$/, leftDelimiter: "*", rightDelimiter: "*" },
            { pattern: /~~$/, leftDelimiter: "~~", rightDelimiter: "~~" },
            { pattern: /==$/, leftDelimiter: "==", rightDelimiter: "==" },
            { pattern: /__$/, leftDelimiter: "__", rightDelimiter: "__" },
            { pattern: /`$/, leftDelimiter: "`", rightDelimiter: "`" },
            { pattern: /\[$/, leftDelimiter: "[", rightDelimiter: "]" },
            { pattern: /\($/, leftDelimiter: "(", rightDelimiter: ")" },
            { pattern: /\{$/, leftDelimiter: "{", rightDelimiter: "}" },
        ];

        for (const rule of markRules) {
            if (rule.pattern.test(textBefore + text)) {
                if (["[", "(", "{"].includes(rule.leftDelimiter) && textAfter.length > 0 && !textAfter.startsWith(' ')) {
                    break;
                }
                if (textAfter.startsWith(rule.rightDelimiter[0])) {
                    return {
                        text: "",
                        offset: 1
                    };
                }
                if (rule.leftDelimiter == "**" && rule.pattern.test(textBefore)) {
                    return {
                        text: rule.rightDelimiter,
                        offset: -2
                    };
                }
                return {
                    text: text + rule.rightDelimiter,
                    offset: -rule.rightDelimiter.length
                };
            }
        }

        return {
            text: text,
            offset: 0
        };
    }
}