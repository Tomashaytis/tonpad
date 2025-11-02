import { keymap } from "prosemirror-keymap";
import { baseKeymap, toggleMark, chainCommands, newlineInCode, createParagraphNear, liftEmptyBlock, splitBlock, joinBackward, selectNodeBackward } from "prosemirror-commands";
import { undo, redo } from "prosemirror-history";
import { markdownSchema } from "../schema/markdown-schema.js";
import { NodeInputter } from "../utils/node-inputter.js";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeReconstructor } from "../utils/node-reconstructor.js";
import { TextSelection } from "prosemirror-state";
import { correctCursorPos } from "../utils/utils.js";

export function keymapPlugin(editor) {
    return keymap({
        ...baseKeymap,
        "Tab": (state, dispatch, view) => {
            const { from, to } = state.selection;
            const { $from } = state.selection;

            if ($from.parent.type.name === 'spec_block') {
                return NodeInputter.handleInputInSpec(view, from, to, '\t');
            }

            if ($from.parent.type.name === 'paragraph') {
                const level = 1;
                const blockStart = $from.before(1);
                const blockEnd = $from.after(1);
                const currentBlock = $from.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText;

                const nodeBlock = NodeConverter.constructTabListItem(nodeText, level);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);
                const cursorPos = containerPos + specNode.nodeSize;

                let selection = TextSelection.create(tr.doc, cursorPos);
                const newSelection = correctCursorPos(tr, cursorPos);
                if (newSelection) {
                    selection = newSelection;
                }

                dispatch(tr.setSelection(selection));
                return true;
            }

            return NodeInputter.handleInputInNormalNode(view, from, to, '\t');
        },
        "Enter": chainCommands(
            newlineInCode,
            createParagraphNear,
            liftEmptyBlock,
            splitBlock
        ),
        "Mod-z": undo,
        "Mod-y": redo,
        "Mod-Shift-z": redo,
        "Mod-b": toggleMark(markdownSchema.marks.strong),
        "Mod-i": toggleMark(markdownSchema.marks.em),
        "Alt-1": () => {
            console.log('=== CURSOR INFO ===');
            console.log(editor.getCursorInfo());
        },
        "Alt-2": () => {
            console.log('=== DOCUMENT JSON ===');
            console.log(JSON.stringify(editor.getDoc(), null, 2));
        },
        "Alt-3": () => {
            console.log('=== HTML ===');
            const html = editor.getHTML();
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = html;

            function printHTML(element, indent = '') {
                if (element.nodeType === Node.TEXT_NODE) {
                    const text = element.textContent.trim();
                    if (text) {
                        console.log(indent + text);
                    }
                    return;
                }

                const tagName = element.tagName.toLowerCase();
                const attrs = Array.from(element.attributes).map(attr =>
                    `${attr.name}="${attr.value}"`
                ).join(' ');

                console.log(indent + `<${tagName}${attrs ? ' ' + attrs : ''}>`);

                Array.from(element.childNodes).forEach(child => {
                    printHTML(child, indent + '  ');
                });

                console.log(indent + `</${tagName}>`);
            }

            Array.from(tempDiv.childNodes).forEach(child => printHTML(child));
        },
        "Alt-4": () => {
            console.log('=== MARKDOWN ===');
            const markdown = editor.getMarkdown();
            const lines = markdown.split('\n');

            lines.forEach((line, index) => {
                if (line.trim()) {
                    const lineNumber = (index + 1).toString().padStart(3, '0');
                    console.log(lineNumber + ' ' + line);
                }
            });
        }
    });
}