import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { TextSelection } from "prosemirror-state";

export class NodeInputter {
    static handleInputInNode(state, dispatch, text, baseTr = null) {
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;

        const marksCheck = this.checkMarks(text, $from);

        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        const newText = node.textContent.slice(0, $from.parentOffset) +
            marksCheck.text +
            node.textContent.slice($from.parentOffset);

        let newNode = NodeConverter.constructParagraph(newText);

        const reconstructor = new NodeReconstructor();

        const reconstructed = reconstructor.applyBlockRules([newNode], nodePos);
        newNode = reconstructed[0];

        let tr = baseTr || state.tr;

        tr = tr.replaceWith(nodePos, nodePos + nodeSize, newNode);

        const oldSize = node.nodeSize;
        const newSize = newNode.nodeSize;
        const sizeDiff = newSize - oldSize;

        const cursorPos = $from.pos + sizeDiff + marksCheck.offset;
        tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);
        return true;
    }

    static handlePasteInNode(view, state, dispatch, text, baseTr = null, offsetToEnd = true) {
        const lines = text.split('\n');

        return this.handleMultiLinePaste(view, state, dispatch, lines, baseTr, offsetToEnd);
    }

    static handleMultiLinePaste(view, state, dispatch, lines, baseTr = null, offsetToEnd = true) {
        const { $from } = state.selection;
        const node = $from.parent;

        const beforeText = node.textContent.slice(0, $from.parentOffset);
        const afterText = node.textContent.slice($from.parentOffset);

        const paragraphs = [];

        lines.forEach((line, index) => {
            let paragraphText = '';

            if (lines.length === 1) {
                paragraphText = beforeText + line + afterText;
            } else if (index === 0) {
                paragraphText = beforeText + line;
            } else if (index === lines.length - 1) {
                paragraphText = line + afterText;
            } else {
                paragraphText = line;
            }

            let paragraph = NodeConverter.constructParagraph(paragraphText);
            paragraphs.push(paragraph);
        });

        const reconstructor = new NodeReconstructor();
        const reconstructed = reconstructor.applyBlockRules(paragraphs, 0);

        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        let cursorOffset = 0;
        if (offsetToEnd) {
            reconstructed.forEach((node, index) => {
                cursorOffset += node.nodeSize;
            });
            cursorOffset -= afterText.length + 1;
        }

        let tr = baseTr || state.tr;

        tr = tr.replaceWith(nodePos, nodePos + nodeSize, reconstructed);

        const cursorPos = nodePos + cursorOffset;

        tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));

        dispatch(tr);

        if (offsetToEnd) {
            setTimeout(() => {
                view.focus();

                const cursorElement = view.domAtPos(cursorPos).node;
                if (cursorElement.nodeType === Node.TEXT_NODE) {
                    cursorElement.parentNode.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center',
                        inline: 'nearest'
                    });
                } else {
                    cursorElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center',
                        inline: 'nearest'
                    });
                }
            }, 0);
        }

        return true;
    }

    static handleDeleteChar(view, backward = true) {
        const { state, dispatch } = view;
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;
        const textContent = node.textContent;
        let cursorOffset = $from.parentOffset;

        if (cursorOffset === 0 && backward || cursorOffset === node.nodeSize - 2 && !backward) {
            return false;
        }

        let newText;
        if (backward) {
            newText = textContent.slice(0, cursorOffset - 1) + textContent.slice(cursorOffset);
        } else {
            newText = textContent.slice(0, cursorOffset) + textContent.slice(cursorOffset + 1);
            cursorOffset += 1;
        }

        let newNode;

        newNode = NodeConverter.constructParagraph(newText);

        const nodePos = $from.before();
        const nodeSize = node.nodeSize;

        const reconstructor = new NodeReconstructor();

        const reconstructed = reconstructor.applyBlockRules([newNode], nodePos);
        newNode = reconstructed[0];

        let tr = state.tr.replaceWith(nodePos, nodePos + nodeSize, newNode);

        const cursorPos = nodePos + cursorOffset;
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
            { pattern: /"$/, leftDelimiter: "\"", rightDelimiter: "\"" },
            { pattern: /'$/, leftDelimiter: "\'", rightDelimiter: "'" },
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
