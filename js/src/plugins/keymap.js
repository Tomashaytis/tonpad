import { keymap } from "prosemirror-keymap";
import { baseKeymap, toggleMark, chainCommands, newlineInCode, createParagraphNear, liftEmptyBlock, splitBlock, joinBackward, selectNodeBackward } from "prosemirror-commands";
import { undo, redo } from "prosemirror-history";
import { splitListItem, sinkListItem } from "prosemirror-schema-list";
import { markdownSchema } from "../schema/markdown-schema.js";

export function keymapPlugin(editor) {
    return keymap({
        ...baseKeymap,
        "Enter": chainCommands(
            splitListItem(markdownSchema.nodes.list_item),
            sinkListItem(markdownSchema.nodes.list_item),
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