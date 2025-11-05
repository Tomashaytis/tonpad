import { keymap } from "prosemirror-keymap";
import { baseKeymap, toggleMark, chainCommands, newlineInCode, createParagraphNear, liftEmptyBlock, splitBlock, joinBackward, selectNodeBackward } from "prosemirror-commands";
import { undo, redo } from "prosemirror-history";
import { markdownSchema } from "../schema/markdown-schema.js";
import { NodeMerger } from "../utils/node-merger.js";
import { NodeSplitter } from "../utils/node-splitter.js";
import { NodeInputter } from "../utils/node-inputter.js";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeReconstructor } from "../utils/node-reconstructor.js";
import { TextSelection } from "prosemirror-state";

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

                dispatch(tr.setSelection(selection));
                return true;
            }

            return NodeInputter.handleInputInNormalNode(view, from, to, '\t');
        },
        "Enter": (state, dispatch, view) => {
            const event = new KeyboardEvent('keydown', { key: 'Enter' });
            const splitterResult = NodeSplitter.handleEnter(view, event);

            if (splitterResult !== undefined && splitterResult !== false) {
                return splitterResult;
            }

            return chainCommands(
                newlineInCode,
                createParagraphNear,
                liftEmptyBlock,
                splitBlock
            )(state, dispatch, view);
        },
        "Backspace": (state, dispatch, view) => {
            const { $from, empty } = state.selection;

            if (!empty) return false;

            const isAtBlockStart = $from.parentOffset === 0;

            if (isAtBlockStart) {
                const tr = NodeMerger.mergeUp(state, state.selection);

                if (tr) {
                    dispatch(tr);
                    return true;
                }
            } else {
                const from = $from.pos - 1;
                const to = $from.pos;
                return NodeInputter.handleDeleteChar(view, from, to);
            }

            return false;
        },
        "Delete": (state, dispatch, view) => {
            const { $from, empty } = state.selection;

            if (!empty) return false;

            const isAtBlockEnd = $from.parentOffset === $from.parent.textContent.length;

            if (isAtBlockEnd) {
                const tr = NodeMerger.mergeDown(state, state.selection);

                if (tr) {
                    dispatch(tr);
                    return true;
                }
            } else {
                const from = $from.pos - 1;
                const to = $from.pos;
                return NodeInputter.handleDeleteChar(view, from, to, false);
            }

            return false;
        },
        "Mod-z": undo,
        "Mod-y": redo,
        "Mod-Shift-z": redo,
        "Mod-b": toggleMark(markdownSchema.marks.strong),
        "Mod-i": toggleMark(markdownSchema.marks.em),
        "Alt-1": () => {
            const info = editor.getCursorInfo();

            console.log('=== CURSOR INFO ===');
            console.log('$from.depth:', info.currentNode.depth);
            console.log('$from.parent.type:', info.currentNode.type);

            const parentDepth = info.currentNode.depth - 1;
            console.log('parentDepth:', parentDepth);
            console.log('currentIndex:', info.currentNode.indexInParent);

            console.log('Current node:', info.currentNode.type, 'index:', info.currentNode.indexInParent);
            console.log('Siblings:');
            info.siblings.forEach(sibling => {
                console.log(`  [${sibling.index}] ${sibling.type}${sibling.isCurrent ? ' <- CURRENT' : ''}`);
            });

            console.log(info);
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
        },
        "Alt-5": () => {
            console.log('=== FRONT MATTER ===');
            const frontMatter = editor.getFrontMatterYAML();
            const lines = frontMatter.split('\n');

            lines.forEach((line, index) => {
                if (line.trim()) {
                    const lineNumber = (index + 1).toString().padStart(3, '0');
                    console.log(lineNumber + ' ' + line);
                }
            });
        },
        "Alt-6": () => {
            console.log('=== NOTE CONTENT ===');
            const noteContent = editor.getNoteContent();
            const lines = noteContent.split('\n');

            lines.forEach((line, index) => {
                if (line.trim()) {
                    const lineNumber = (index + 1).toString().padStart(3, '0');
                    console.log(lineNumber + ' ' + line);
                }
            });
        }
    });
}