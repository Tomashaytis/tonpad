import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { dropCursor } from "prosemirror-dropcursor";
import { gapCursor } from "prosemirror-gapcursor";
import { history, } from "prosemirror-history";
import { markdownSchema } from "./schema/markdown-schema.js";
import { CustomMarkdownParser } from "./parser/markdown-parser.js";
import { NodeConverter } from "./utils/node-converter.js";
import { markdownSerializer } from "./serializer/markdown-serializer.js";
import { blockNavigationPlugin } from "./plugins/block-navigation.js"
import { enterPlugin } from "./plugins/enter.js";
import { keymapPlugin } from "./plugins/keymap.js";
import { inputRulesPlugin } from "./plugins/input-rules.js";
import { backspacePlugin } from "./plugins/backspace.js";
import { deletePlugin } from "./plugins/delete.js";
import { inputPlugin } from "./plugins/input.js"
import { patternMatchPlugin } from "./plugins/pattern-matching.js"
import { disableInsertPlugin } from "./plugins/disable-insert.js"
import { hideSpecPlugin } from "./plugins/hide-spec.js"
import { copyPlugin } from "./plugins/copy.js"

export class Editor {
    constructor(target, content = '') {
        if (!target) throw new Error('Target element required');

        const parser = new CustomMarkdownParser();
        let doc = parser.parse(content);

        this.view = new EditorView(target, {
            state: EditorState.create({
                doc: doc,
                schema: markdownSchema,
                plugins: this.createPlugins()
            }),
            attributes: {
                class: "markdown-editor",
                spellcheck: "false",
                'data-gramm': "false",
                'data-gramm-editor': "false",
                style: `
                    outline: none; 
                    min-height: 300px; 
                    padding: 15px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                    font-size: 14px;
                    line-height: 1.6;
                    background: white;
                    white-space: pre-wrap;
                `
            }
        });

        this.rebuildTree();
    }

    rebuildTree() {
        let tr = this.view.state.tr;
        let hasChanges = false;

        const changes = [];

        this.view.state.doc.descendants((node, pos) => {
            if (node.type.name === 'heading') {
                changes.push({ type: 'heading', node, pos });
            }

            if (node.type.name === 'blockquote') {
                changes.push({ type: 'blockquote', node, pos });
            }

            if (node.isText && node.marks.length > 0) {
                const $pos = this.view.state.doc.resolve(pos);
                let paragraphNode = null;
                let paragraphPos = 0;

                for (let depth = $pos.depth; depth >= 0; depth--) {
                    if ($pos.node(depth).type.name === 'paragraph') {
                        paragraphNode = $pos.node(depth);
                        paragraphPos = $pos.start(depth);
                        break;
                    }
                }

                if (paragraphNode) {
                    changes.push({
                        type: 'mark',
                        node: node,
                        pos: pos,
                        paragraphNode: paragraphNode,
                        paragraphPos: paragraphPos
                    });
                }
            }
        });

        changes.sort((a, b) => b.pos - a.pos).forEach((change) => {
            if (change.type === 'heading') {
                const headingText = change.node.textContent.replace(/^#+\s/, '');
                const level = change.node.attrs.level;
                const headingBlock = NodeConverter.constructHeading(headingText, level);
                tr = tr.replaceWith(change.pos, change.pos + change.node.nodeSize, headingBlock);
                hasChanges = true;
            } else if (change.type === 'blockquote') {
                const blockquoteContent = change.node.content;
                const blockquoteBlock = NodeConverter.constructBlockquote(blockquoteContent);
                tr = tr.replaceWith(change.pos, change.pos + change.node.nodeSize, blockquoteBlock);
                hasChanges = true;
            } else if (change.type === 'mark') {
                const textNode = change.node;
                const paragraphNode = change.paragraphNode;
                const paragraphPos = change.paragraphPos;

                textNode.marks.forEach(mark => {
                    const textContent = textNode.textContent;
                    let markFragment;

                    switch (mark.type.name) {
                        case 'em':
                            markFragment = NodeConverter.constructEm(textContent);
                            break;
                        case 'strong':
                            markFragment = NodeConverter.constructStrong(textContent);
                            break;
                        case 'strike':
                            markFragment = NodeConverter.constructStrike(textContent);
                            break;
                        case 'highlight':
                            markFragment = NodeConverter.constructHighlight(textContent);
                            break;
                        case 'underline':
                            markFragment = NodeConverter.constructUnderline(textContent);
                            break;
                        case 'code':
                            markFragment = NodeConverter.constructCode(textContent);
                            break;
                        default:
                            return;
                    }

                    tr = tr.replaceWith(change.pos, change.pos + textNode.nodeSize, markFragment);
                    hasChanges = true;
                });
            }
        });

        if (hasChanges) {
            const currentPos = this.view.state.selection.from;
            this.view.dispatch(tr);

            const newSelection = this.view.state.selection.constructor.near(
                this.view.state.doc.resolve(Math.min(currentPos, this.view.state.doc.content.size - 1))
            );
            this.view.dispatch(this.view.state.tr.setSelection(newSelection));
        }
    }

    createPlugins() {
        return [
            history(),
            //lineBreakPlugin(),
            backspacePlugin(),
            deletePlugin(),
            enterPlugin(),
            keymapPlugin(this),
            dropCursor(),
            gapCursor(),
            inputRulesPlugin(),
            blockNavigationPlugin(),
            inputPlugin(),
            disableInsertPlugin(),
            //patternMatchPlugin(),
            //hideSpecPlugin(),
            copyPlugin(),
        ];
    }

    setMarkdown(markdown) {
        try {
            const parser = new CustomMarkdownParser();
            let newDoc = parser.parse(markdown || "");

            const tr = this.view.state.tr.replaceWith(0, this.view.state.doc.content.size, newDoc.content);
            this.view.dispatch(tr);

            setTimeout(() => this.rebuildTree(), 0);
        } catch (error) {
            console.log(`Error: ${error}`);
        }
    }

    getMarkdown() {
        try {
            return markdownSerializer.serialize(this.view.state.doc);
        } catch (error) {
            console.log(`Error: ${error}`);
            return "";
        }
    }

    getHTML() {
        try {
            const fragment = this.view.dom.cloneNode(true);
            const div = document.createElement("div");
            div.appendChild(fragment);
            return div.innerHTML;
        } catch (error) {
            console.log(`Error: ${error}`);
            return "";
        }
    }

    getDoc() {
        return this.view.state.doc.toJSON();
    }

    getCursorInfo() {
        const { $from } = this.view.state.selection;

        console.log('=== CURSOR INFO (CORRECTED) ===');
        console.log('$from.depth:', $from.depth);
        console.log('$from.parent.type:', $from.parent.type.name);

        const parentDepth = $from.depth - 1;
        const currentIndex = parentDepth >= 0 ? $from.index(parentDepth) : 0;
        const parent = parentDepth >= 0 ? $from.node(parentDepth) : null;

        console.log('parentDepth:', parentDepth);
        console.log('currentIndex (corrected):', currentIndex);

        const info = {
            currentNode: {
                type: $from.parent.type.name,
                attrs: $from.parent.attrs,
                content: $from.parent.content,
                nodeSize: $from.parent.nodeSize,
                textContent: $from.parent.textContent,
                depth: $from.depth,
                parentOffset: $from.parentOffset,
                indexInParent: currentIndex
            },
            siblings: [],
            parent: null,
            path: []
        };

        if (parent) {
            info.parent = {
                type: parent.type.name,
                attrs: parent.attrs,
                nodeSize: parent.nodeSize,
                childCount: parent.childCount
            };

            for (let i = 0; i < parent.childCount; i++) {
                const sibling = parent.child(i);
                info.siblings.push({
                    index: i,
                    type: sibling.type.name,
                    attrs: sibling.attrs,
                    nodeSize: sibling.nodeSize,
                    textContent: sibling.textContent,
                    isCurrent: i === currentIndex
                });
            }
        }

        console.log('CORRECT Current node:', info.currentNode.type, 'index:', currentIndex);
        console.log('Siblings:');
        info.siblings.forEach(sibling => {
            console.log(`  [${sibling.index}] ${sibling.type}${sibling.isCurrent ? ' ← CURRENT' : ''}`);
        });

        return info;
    }

    focus() {
        this.view.focus();
    }

    destroy() {
        this.view.destroy();
    }
}

/*
rebuildTree() {
        let tr = this.view.state.tr;
        let hasChanges = false;

        const changes = [];

        this.view.state.doc.descendants((node, pos) => {
            if (node.type.name === 'heading') {
                changes.push({ type: 'heading', node, pos });
            }
            else if (node.isText && node.marks.length > 0) {
                changes.push({ type: 'mark', node, pos, marks: [...node.marks] });
            }
        });

        changes.sort((a, b) => b.pos - a.pos).forEach((change) => {
            if (change.type === 'heading') {
                const headingBlock = NodeConverter.constructHeading(change.node);
                tr = tr.replaceWith(change.pos, change.pos + change.node.nodeSize, headingBlock);
                hasChanges = true;
            }
            else if (change.type === 'mark') {
                change.marks.forEach(mark => {
                    const textNode = markdownSchema.text(change.node.text, change.node.marks);

                    let markContainer;

                    switch (mark.type.name) {
                        case 'em':
                            markContainer = NodeConverter.constructEm(textNode);
                            break;
                        case 'strong':
                            markContainer = NodeConverter.constructStrong(textNode);
                            break;
                        case 'strike':
                            markContainer = NodeConverter.constructStrike(textNode);
                            break;
                        case 'highlight':
                            markContainer = NodeConverter.constructHighlight(textNode);
                            break;
                        case 'underline':
                            markContainer = NodeConverter.constructUnderline(textNode);
                            break;
                        case 'code':
                            markContainer = NodeConverter.constructCode(textNode);
                            break;
                        default:
                            return;
                    }

                    tr = tr.replaceWith(change.pos, change.pos + change.node.nodeSize, markContainer);
                    hasChanges = true;
                });
            }
        });

        if (hasChanges) {
            const currentPos = this.view.state.selection.from;

            this.view.dispatch(tr);

            const newSelection = this.view.state.selection.constructor.near(
                this.view.state.doc.resolve(currentPos)
            );
            this.view.dispatch(this.view.state.tr.setSelection(newSelection));
        }
    }
 */

/*import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { keymap } from "prosemirror-keymap";
import { baseKeymap, toggleMark, chainCommands, newlineInCode, createParagraphNear, liftEmptyBlock, splitBlock } from "prosemirror-commands";
import { dropCursor } from "prosemirror-dropcursor";
import { gapCursor } from "prosemirror-gapcursor";
import { history, redo, undo } from "prosemirror-history";
import { inputRules, textblockTypeInputRule, wrappingInputRule, InputRule } from "prosemirror-inputrules";
import { sinkListItem, splitListItem, liftListItem, wrapInList } from "prosemirror-schema-list";
import { markdownSchema } from "./schema/markdown-schema.js";
import { CustomMarkdownParser } from "./plugins/markdown-parser.js";
import { markdownSerializer } from "./plugins/markdown-serializer.js";

export class Editor {
    constructor(target, content = '') {
        if (!target) throw new Error('Target element required');

        const parser = new CustomMarkdownParser();
        const doc = parser.parse(content);

        this.view = new EditorView(target, {
            state: EditorState.create({
                doc: doc,
                schema: markdownSchema,
                plugins: this.createPlugins()
            }),
            attributes: {
                class: "markdown-editor",
                style: `
                    outline: none; 
                    min-height: 300px; 
                    padding: 15px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                    font-size: 14px;
                    line-height: 1.6;
                    background: white;
                    white-space: pre-wrap;
                `
            },
            dispatchTransaction: (tr) => {
                const newState = this.view.state.apply(tr);
                this.view.updateState(newState);
                
                setTimeout(() => {
                    this.updateFocusFromSelection();
                }, 10);
            }
        });

        this.setupFocusHandling();
    }

    setupFocusHandling() {
        this.activeHeadingPos = null;
        this.lastHeadingPos = null;
        
        this.view.dom.addEventListener('click', (e) => {
            this.updateFocusFromSelection();
        });

        this.view.dom.addEventListener('keyup', (e) => {
            this.updateFocusFromSelection();
        });

        this.view.dom.addEventListener('mousemove', (e) => {
            this.updateFocusFromSelection();
        });

        this.view.dom.addEventListener('mouseleave', (e) => {
            this.resetAllFocus();
        });

        window.addEventListener('resize', () => {
            setTimeout(() => this.updateFocusFromSelection(), 100);
        });
    }

    updateFocusFromSelection() {
        const { selection } = this.view.state;
        const pos = selection.$from.pos;
        
        try {
            const $pos = this.view.state.doc.resolve(pos);
            
            let headingNode = null;
            let headingPos = null;
            
            for (let depth = $pos.depth; depth > 0; depth--) {
                const node = $pos.node(depth);
                if (node && (node.type.name === 'heading' || node.type.name === 'heading_focus')) {
                    headingNode = node;
                    headingPos = $pos.before(depth);
                    break;
                }
            }
            
            if (headingNode && headingNode.type.name === 'heading' && this.activeHeadingPos !== headingPos) {
                if (this.activeHeadingPos !== null) {
                    this.convertToNormal(this.activeHeadingPos);
                }
                this.convertToFocus(headingPos);
                this.activeHeadingPos = headingPos;
                this.lastHeadingPos = headingPos;
            } else if (!headingNode && this.activeHeadingPos !== null) {
                this.convertToNormal(this.activeHeadingPos);
                this.activeHeadingPos = null;
                this.lastHeadingPos = null;
            } else if (headingNode && headingNode.type.name === 'heading_focus' && 
                     this.activeHeadingPos === headingPos && 
                     !this.isCursorInHeading($pos, headingPos)) {
                this.convertToNormal(headingPos);
                this.activeHeadingPos = null;
                this.lastHeadingPos = null;
            } else if (this.lastHeadingPos !== null && this.activeHeadingPos === null) {
                const lastNode = this.view.state.doc.nodeAt(this.lastHeadingPos);
                if (lastNode && lastNode.type.name === 'heading_focus' && 
                    !this.isCursorInHeading($pos, this.lastHeadingPos)) {
                    this.convertToNormal(this.lastHeadingPos);
                    this.lastHeadingPos = null;
                }
            }
        } catch (error) {
            console.log(`Error: ${error}`);
        }
    }

    isCursorInHeading($pos, headingPos) {
        try {
            const headingNode = this.view.state.doc.nodeAt(headingPos);
            if (!headingNode) return false;
            
            const headingStart = headingPos + 1;
            const headingEnd = headingStart + headingNode.nodeSize;
            
            return $pos.pos >= headingStart && $pos.pos <= headingEnd;
        } catch (error) {
            return false;
        }
    }

    resetAllFocus() {
        if (this.activeHeadingPos !== null) {
            this.convertToNormal(this.activeHeadingPos);
            this.activeHeadingPos = null;
        }
        
        const focusHeadings = [];
        this.view.state.doc.descendants((node, pos) => {
            if (node.type.name === 'heading_focus') {
                focusHeadings.push(pos);
            }
        });
        
        focusHeadings.forEach(pos => {
            this.convertToNormal(pos);
        });
        
        this.lastHeadingPos = null;
    }

    convertToFocus(headingPos) {
        if (headingPos === null || headingPos === undefined) return;
        
        try {
            const node = this.view.state.doc.nodeAt(headingPos);
            if (node && node.type.name === 'heading') {
                const tr = this.view.state.tr.setNodeMarkup(headingPos, markdownSchema.nodes.heading_focus, {
                    level: node.attrs.level
                });
                this.view.dispatch(tr);
            }
        } catch (error) {
            console.log(`Error: ${error}`);
        }
    }

    convertToNormal(headingPos) {
        if (headingPos === null || headingPos === undefined) return;
        
        try {
            const node = this.view.state.doc.nodeAt(headingPos);
            if (node && node.type.name === 'heading_focus') {
                const tr = this.view.state.tr.setNodeMarkup(headingPos, markdownSchema.nodes.heading, {
                    level: node.attrs.level
                });
                this.view.dispatch(tr);
            }
        } catch (error) {
            console.log(`Error: ${error}`);
        }
    }

    // НОВЫЙ МЕТОД: Уменьшение уровня заголовка или преобразование в параграф
    decreaseHeadingLevel(headingPos) {
        if (headingPos === null || headingPos === undefined) return false;
        
        try {
            const node = this.view.state.doc.nodeAt(headingPos);
            if (!node) return false;

            let tr = this.view.state.tr;
            
            if (node.type.name === 'heading_focus') {
                const currentLevel = node.attrs.level;
                
                if (currentLevel > 1) {
                    // Уменьшаем уровень заголовка
                    tr = tr.setNodeMarkup(headingPos, markdownSchema.nodes.heading_focus, {
                        level: currentLevel - 1
                    });
                } else {
                    // Преобразуем заголовок 1 уровня в параграф
                    tr = tr.setNodeMarkup(headingPos, markdownSchema.nodes.paragraph);
                }
                
                this.view.dispatch(tr);
                return true;
            }
        } catch (error) {
            console.log(`Error decreasing heading level: ${error}`);
        }
        
        return false;
    }

    createPlugins() {
        const enterCommand = chainCommands(
            splitListItem(markdownSchema.nodes.list_item),
            sinkListItem(markdownSchema.nodes.list_item),
            newlineInCode,
            createParagraphNear,
            liftEmptyBlock,
            splitBlock
        );

        // НОВАЯ КОМАНДА: Backspace в начале заголовка
        const backspaceCommand = (state, dispatch) => {
            const { selection } = state;
            const { $from } = selection;
            
            // Проверяем, что курсор в начале узла и это заголовок в фокусе
            if ($from.parentOffset === 0) {
                for (let depth = $from.depth; depth > 0; depth--) {
                    const node = $from.node(depth);
                    if (node && node.type.name === 'heading_focus') {
                        const headingPos = $from.before(depth);
                        
                        if (dispatch) {
                            // Используем наш метод для уменьшения уровня
                            setTimeout(() => {
                                this.decreaseHeadingLevel(headingPos);
                            }, 10);
                        }
                        return true;
                    }
                }
            }
            
            return false;
        };

        return [
            history(),
            keymap({
                ...baseKeymap,
                "Enter": (state, dispatch) => {
                    return enterCommand(state, dispatch);
                },
                "Backspace": chainCommands(
                    backspaceCommand, // Сначала пробуем нашу команду
                    baseKeymap.Backspace // Затем стандартное поведение
                ),
                "Mod-z": undo,
                "Mod-y": redo,
                "Mod-Shift-z": redo,
                "Mod-b": toggleMark(markdownSchema.marks.strong),
                "Mod-i": toggleMark(markdownSchema.marks.em)
            }),
            dropCursor(),
            gapCursor(),
            inputRules({
                rules: [
                    textblockTypeInputRule(
                        /^(#{1,6})\s$/,
                        markdownSchema.nodes.heading,
                        match => ({ level: match[1].length })
                    ),
                    wrappingInputRule(
                        /^\s*([-+*])\s$/,
                        markdownSchema.nodes.bullet_list,
                        null,
                        () => true
                    ),
                    wrappingInputRule(
                        /^\s*(\d+)\.\s$/,
                        markdownSchema.nodes.ordered_list,
                        match => ({ order: +match[1] }),
                        () => true
                    ),
                    new InputRule(/\*\*([^\*]+)\*\*$/, (state, match, start, end) => {
                        return state.tr.replaceWith(
                            start,
                            end,
                            state.schema.text(match[1], [markdownSchema.marks.strong.create()])
                        );
                    }),
                    new InputRule(/\*([^\*]+)\*$/, (state, match, start, end) => {
                        return state.tr.replaceWith(
                            start,
                            end,
                            state.schema.text(match[1], [markdownSchema.marks.em.create()])
                        );
                    }),
                    new InputRule(/^```(\w*)\s$/, (state, match, start, end) => {
                        const language = match[1] || "";
                        return state.tr.replaceWith(
                            start,
                            end,
                            markdownSchema.nodes.code_block.create(
                                { params: language },
                                language ? [] : [markdownSchema.text("")]
                            )
                        );
                    })
                ]
            })
        ];
    }

    getMarkdown() {
        try {
            let doc = this.view.state.doc;
            const focusHeadings = [];
            
            doc.descendants((node, pos) => {
                if (node.type.name === 'heading_focus') {
                    focusHeadings.push({ pos, node });
                }
            });

            if (focusHeadings.length > 0) {
                let tr = this.view.state.tr;
                focusHeadings.forEach(({ pos, node }) => {
                    tr = tr.setNodeMarkup(pos, markdownSchema.nodes.heading, {
                        level: node.attrs.level
                    });
                });
                doc = tr.doc;
            }

            return markdownSerializer.serialize(doc);
        } catch (error) {
            console.log(`Error: ${error}`);
            return "";
        }
    }

    setMarkdown(markdown) {
        try {
            const parser = new CustomMarkdownParser();
            const newDoc = parser.parse(markdown || "");
            const tr = this.view.state.tr.replaceWith(0, this.view.state.doc.content.size, newDoc.content);
            this.view.dispatch(tr);
            this.resetAllFocus();
        } catch (error) {
            console.log(`Error: ${error}`);
        }
    }

    getHTML() {
        try {
            const fragment = this.view.dom.cloneNode(true);
            const div = document.createElement("div");
            div.appendChild(fragment);
            return div.innerHTML;
        } catch (error) {
            console.log(`Error: ${error}`);
            return "";
        }
    }

    focus() {
        this.view.focus();
    }

    destroy() {
        this.view.destroy();
    }

    forceResetFocus() {
        this.resetAllFocus();
    }
}*/