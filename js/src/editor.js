import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { dropCursor } from "prosemirror-dropcursor";
import { gapCursor } from "prosemirror-gapcursor";
import { history, } from "prosemirror-history";
import { markdownSchema } from "./schema/markdown-schema.js";
import { NodeConverter } from "./utils/node-converter.js";
import { NodeReconstructor } from "./utils/node-reconstructor.js";
import { markdownSerializer } from "./serializer/markdown-serializer.js";
import { blockNavigationPlugin } from "./plugins/block-navigation.js"
import { keymapPlugin } from "./plugins/keymap.js";
import { inputRulesPlugin } from "./plugins/input-rules.js";
import { inputPlugin } from "./plugins/input.js"
import { disableInsertPlugin } from "./plugins/disable-insert.js"
import { hideSpecPlugin } from "./plugins/hide-spec.js"
import { copyPlugin } from "./plugins/copy.js"
import jsYAML from 'js-yaml';

export class Editor {
    constructor(target, content = '') {
        if (!target) throw new Error('Target element required');

        const docContent = this.parseDoc(content);

        this.frontMatter = docContent.frontMatter;

        const doc = this.createDocumentFromText(docContent.markdown);

        this.frontMatterTable = document.getElementById('frontmatter-table');
        this.frontMatterBody = document.getElementById('frontmatter-body');
        this.updateFrontMatterTable();

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

    createDocumentFromText(content) {
        if (!content.trim()) {
            return markdownSchema.nodes.doc.create({}, [
                NodeConverter.constructParagraph()
            ]);
        }

        const lines = content.split('\n');
        const paragraphs = [];

        for (const line of lines) {
            paragraphs.push(NodeConverter.constructParagraph(line));
        }

        return markdownSchema.nodes.doc.create({}, paragraphs);
    }

    rebuildTree() {
        let tr = this.view.state.tr;
        let hasChanges = false;

        const reconstructor = new NodeReconstructor();
        const changes = [];

        this.view.state.doc.descendants((node, pos) => {
            if (node.type.name === 'paragraph') {
                changes.push({ type: 'paragraph', node, pos });
            }
        });

        changes.sort((a, b) => b.pos - a.pos).forEach((change) => {
            if (change.type === 'paragraph') {
                const paragraph = change.node;
                const text = paragraph.textContent;

                const blockResult = reconstructor.applyBlockRules([paragraph], change.pos);
                const reconstructedNode = blockResult.paragraphs[0];

                const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
                const finalNode = markReconstruction || reconstructedNode;

                if (finalNode !== paragraph) {
                    tr = tr.replaceWith(change.pos, change.pos + paragraph.nodeSize, finalNode);
                    hasChanges = true;
                }
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
            keymapPlugin(this),
            dropCursor(),
            gapCursor(),
            inputRulesPlugin(),
            blockNavigationPlugin(),
            inputPlugin(),
            disableInsertPlugin(),
            //hideSpecPlugin(),
            copyPlugin(),
        ];
    }

    parseDoc(text) {
        const yamlRegex = /^---\s*\n([\s\S]*?)\n---\s*\n([\s\S]*)$/;
        const match = text.match(yamlRegex);

        if (match) {
            const yamlContent = match[1];
            const markdownContent = match[2];

            return {
                frontMatter: this.parseYAML(yamlContent),
                markdown: markdownContent
            };
        }

        return {
            frontMatter: {},
            markdown: text
        };
    }

    parseYAML(yamlString) {
        try {
            return jsYAML.load(yamlString);
        } catch (error) {
            console.warn('Invalid YAML front matter:', error);
            return {};
        }
    }

    updateFrontMatterTable() {
        this.frontMatterBody.innerHTML = '';

        if (this.frontMatter && Object.keys(this.frontMatter).length > 0) {
            Object.entries(this.frontMatter).forEach(([key, value]) => {
                const row = document.createElement('tr');

                // Ячейка ключа
                const keyCell = document.createElement('td');
                const keyInput = document.createElement('input');
                keyInput.type = 'text';
                keyInput.value = key;
                keyInput.className = 'frontmatter-input';
                keyInput.addEventListener('change', (e) => this.updateFrontMatterKey(key, e.target.value, row));
                keyCell.appendChild(keyInput);

                // Ячейка значения
                const valueCell = document.createElement('td');
                const valueInput = document.createElement('input');
                valueInput.type = 'text';
                valueInput.value = String(value);
                valueInput.className = 'frontmatter-input';
                valueInput.addEventListener('change', (e) => this.updateFrontMatterValue(key, e.target.value));
                valueCell.appendChild(valueInput);

                // Ячейка действий
                const actionCell = document.createElement('td');
                const deleteButton = document.createElement('button');
                deleteButton.textContent = '×';
                deleteButton.className = 'frontmatter-delete-btn';
                deleteButton.addEventListener('click', () => this.deleteFrontMatterField(key));
                actionCell.appendChild(deleteButton);

                row.appendChild(keyCell);
                row.appendChild(valueCell);
                row.appendChild(actionCell);
                this.frontMatterBody.appendChild(row);
            });

            const addRow = document.createElement('tr');
            const emptyCell1 = document.createElement('td');
            const emptyCell2 = document.createElement('td');
            const addCell = document.createElement('td');

            const addButton = document.createElement('button');
            addButton.textContent = '+';
            addButton.className = 'frontmatter-add-btn';
            addButton.onclick = () => this.addFrontMatterField();

            addCell.appendChild(addButton);

            addRow.appendChild(emptyCell1);
            addRow.appendChild(emptyCell2);
            addRow.appendChild(addCell);
            this.frontMatterBody.appendChild(addRow);

            this.frontMatterTable.style.display = 'table';
        } else {
            const addRow = document.createElement('tr');
            const emptyCell1 = document.createElement('td');
            const emptyCell2 = document.createElement('td');
            const addCell = document.createElement('td');

            const addButton = document.createElement('button');
            addButton.textContent = '+';
            addButton.className = 'frontmatter-add-btn';
            addButton.onclick = () => this.addFrontMatterField();

            addCell.appendChild(addButton);

            addRow.appendChild(emptyCell1);
            addRow.appendChild(emptyCell2);
            addRow.appendChild(addCell);
            this.frontMatterBody.appendChild(addRow);

            this.frontMatterTable.style.display = 'table';
        }
    }

    updateFrontMatterKey(oldKey, newKey, row) {
        if (oldKey === newKey) return;

        if (this.frontMatter[newKey]) {
            alert('Поле с таким именем уже существует!');
            row.querySelector('input[type="text"]').value = oldKey;
            return;
        }

        const value = this.frontMatter[oldKey];

        delete this.frontMatter[oldKey];
        this.frontMatter[newKey] = value;

        this.updateDocumentWithFrontMatter();
    }

    updateFrontMatterValue(key, newValue) {
        if (this.frontMatter[key] === newValue) return;

        this.frontMatter[key] = newValue;
        this.updateDocumentWithFrontMatter();
    }

    deleteFrontMatterField(key) {
        if (confirm(`Удалить поле "${key}"?`)) {
            delete this.frontMatter[key];

            if (Object.keys(this.frontMatter).length === 0) {
                this.frontMatter = {};
            }

            this.updateDocumentWithFrontMatter();
        }
    }

    addFrontMatterField() {
        const newKey = `new_field_${Date.now()}`;
        this.frontMatter[newKey] = '';
        this.updateFrontMatterTable();
    }

    updateDocumentWithFrontMatter() {
        this.updateFrontMatterTable();

        const currentMarkdown = this.getMarkdown();

        const newContent = this.getNoteContent(currentMarkdown);
        this.setNoteContent(newContent);
    }

    setNoteContent(content) {
        try {
            const docContent = this.parseDoc(content);
            this.frontMatter = docContent.frontMatter;
            this.updateFrontMatterTable();

            const newDoc = this.createDocumentFromText(docContent.markdown);
            const tr = this.view.state.tr.replaceWith(0, this.view.state.doc.content.size, newDoc.content);
            this.view.dispatch(tr);

            this.rebuildTree();
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

    getFrontMatter() {
        if (this.frontMatter && Object.keys(this.frontMatter).length > 0) {
            let frontMatterContent = "---\n";
            Object.entries(this.frontMatter).forEach(([key, value]) => {
                frontMatterContent += `${key}: ${value}\n`
            });
            frontMatterContent += "---\n\n"
            return frontMatterContent;
        } else {
            console.log('No front matter found');
            return "";
        }
    }

    getFrontMatterJSON() {
        try {
            if (this.frontMatter && Object.keys(this.frontMatter).length > 0) {
                return JSON.stringify(this.frontMatter);
            } else {
                return "{}";
            }
        } catch (error) {
            console.error('Error serializing front matter:', error);
            return "{}";
        }
    }

    getDoc() {
        return this.view.state.doc.toJSON();
    }

    getNoteContent() {
        return this.getFrontMatter() + this.getMarkdown();
    }

    getCursorInfo() {
        const { $from } = this.view.state.selection;

        const parentDepth = $from.depth - 1;
        const currentIndex = parentDepth >= 0 ? $from.index(parentDepth) : 0;
        const parent = parentDepth >= 0 ? $from.node(parentDepth) : null;

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

        return info;
    }

    focus() {
        this.view.focus();
    }

    destroy() {
        this.view.destroy();
    }
}
