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
import { searchPlugin, searchCommands } from "./plugins/search.js"
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
            searchPlugin(),
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

        const validateFieldName = (fieldName) => {
            if (!fieldName.trim()) {
                return 'Имя поля не может быть пустым';
            }

            if (this.frontMatter[fieldName]) {
                return 'Поле с таким именем уже существует';
            }

            if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(fieldName)) {
                return 'Имя поля может содержать только буквы, цифры и подчеркивания, и должно начинаться с буквы или подчеркивания';
            }

            return '';
        };

        const error = validateFieldName(newKey);
        if (error) {
            this.showErrorDialog('Ошибка', error);
            row.querySelector('input[type="text"]').value = oldKey;
            return;
        }

        const value = this.frontMatter[oldKey];
        delete this.frontMatter[oldKey];
        this.frontMatter[newKey] = value;

        this.updateDocumentWithFrontMatter();
    }

    showErrorDialog(title, message) {
        const modal = document.createElement('div');
        modal.className = 'frontmatter-add-modal';

        const dialog = document.createElement('div');
        dialog.className = 'frontmatter-add-dialog';

        dialog.innerHTML = `
        <h3 class="frontmatter-add-title">${title}</h3>
        <div style="margin-bottom: 20px; color: #666;">${message}</div>
        <div class="frontmatter-add-buttons">
            <button class="frontmatter-add-btn primary">OK</button>
        </div>
    `;

        modal.appendChild(dialog);
        document.body.appendChild(modal);

        const okButton = dialog.querySelector('.frontmatter-add-btn.primary');

        const closeModal = () => {
            document.body.removeChild(modal);
        };

        okButton.addEventListener('click', closeModal);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });

        setTimeout(() => okButton.focus(), 0);
    }
    updateFrontMatterValue(key, newValue) {
        if (this.frontMatter[key] === newValue) return;

        this.frontMatter[key] = newValue;
        this.updateDocumentWithFrontMatter();
    }

    deleteFrontMatterField(key) {
        this.showConfirmDialog(
            `Удалить поле "${key}"?`,
            () => {
                delete this.frontMatter[key];

                if (Object.keys(this.frontMatter).length === 0) {
                    this.frontMatter = {};
                }

                this.updateDocumentWithFrontMatter();
            }
        );
    }

    showConfirmDialog(message, onConfirm) {
        const modal = document.createElement('div');
        modal.className = 'frontmatter-confirm-modal';

        const dialog = document.createElement('div');
        dialog.className = 'frontmatter-confirm-dialog';

        dialog.innerHTML = `
        <div class="frontmatter-confirm-message">${message}</div>
        <div class="frontmatter-confirm-buttons">
            <button class="frontmatter-confirm-btn cancel">Отмена</button>
            <button class="frontmatter-confirm-btn delete">Удалить</button>
        </div>
    `;

        modal.appendChild(dialog);
        document.body.appendChild(modal);

        const cancelBtn = dialog.querySelector('.cancel');
        const deleteBtn = dialog.querySelector('.delete');

        const closeModal = () => {
            document.body.removeChild(modal);
        };

        cancelBtn.addEventListener('click', closeModal);
        deleteBtn.addEventListener('click', () => {
            onConfirm();
            closeModal();
        });

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal();
            }
        });

        const escHandler = (e) => {
            if (e.key === 'Escape') {
                closeModal();
                document.removeEventListener('keydown', escHandler);
            }
        };
        document.addEventListener('keydown', escHandler);

        cancelBtn.focus();
    }

    addFrontMatterField() {
        this.showAddFieldDialog();
    }

    showAddFieldDialog() {
        const modal = document.createElement('div');
        modal.className = 'frontmatter-add-modal';

        const dialog = document.createElement('div');
        dialog.className = 'frontmatter-add-dialog';

        dialog.innerHTML = `
        <h3 class="frontmatter-add-title">Добавить новое поле</h3>
        <div class="frontmatter-add-form">
            <div class="frontmatter-add-field">
                <label class="frontmatter-add-label">Имя поля:</label>
                <input type="text" class="frontmatter-add-input" placeholder="Введите имя поля">
                <div class="frontmatter-add-error"></div>
            </div>
            <div class="frontmatter-add-field">
                <label class="frontmatter-add-label">Значение:</label>
                <input type="text" class="frontmatter-add-input" placeholder="Введите значение">
                <div class="frontmatter-add-error"></div>
            </div>
            <div class="frontmatter-add-buttons">
                <button class="frontmatter-add-btn cancel">Отмена</button>
                <button class="frontmatter-add-btn primary" disabled>Добавить</button>
            </div>
        </div>
    `;

        modal.appendChild(dialog);
        document.body.appendChild(modal);

        const nameInput = dialog.querySelector('.frontmatter-add-input');
        const valueInput = dialog.querySelectorAll('.frontmatter-add-input')[1];
        const nameError = dialog.querySelector('.frontmatter-add-error');
        const addButton = dialog.querySelector('.frontmatter-add-btn.primary');
        const cancelButton = dialog.querySelector('.frontmatter-add-btn.cancel');

        const validateFieldName = (fieldName) => {
            if (!fieldName.trim()) {
                return 'Имя поля не может быть пустым';
            }

            if (this.frontMatter[fieldName]) {
                return 'Поле с таким именем уже существует';
            }

            const invalidChars = /[:{}\[\]]/;
            if (invalidChars.test(fieldName)) {
                return 'Имя поля не может содержать символы : { } [ ]';
            }

            if (fieldName !== fieldName.trim()) {
                return 'Имя поля не должно начинаться или заканчиваться пробелами';
            }

            const mayNeedQuotes = /[#&*!|>'"%@`-]|\s/;
            if (mayNeedQuotes.test(fieldName)) {
                return 'Имя поля содержит символы, которые могут требовать кавычек в YAML. Это допустимо, но может усложнить чтение.';
            }

            return '';
        };

        const updateAddButtonState = () => {
            const name = nameInput.value.trim();
            const nameErrorText = validateFieldName(name);
            const isValid = !nameErrorText && name.length > 0;

            addButton.disabled = !isValid;

            if (nameErrorText) {
                nameInput.classList.add('error');
            } else {
                nameInput.classList.remove('error');
            }
        };

        nameInput.addEventListener('input', updateAddButtonState);
        nameInput.addEventListener('blur', () => {
            const error = validateFieldName(nameInput.value.trim());
            nameError.textContent = error;
        });

        const closeModal = () => {
            document.body.removeChild(modal);
            document.removeEventListener('keydown', escHandler);
        };

        const addField = () => {
            const fieldName = nameInput.value.trim();
            const fieldValue = valueInput.value.trim();

            const error = validateFieldName(fieldName);
            if (error) {
                nameError.textContent = error;
                nameInput.focus();
                return;
            }

            this.frontMatter[fieldName] = fieldValue;
            this.updateDocumentWithFrontMatter();
            closeModal();
        };

        cancelButton.addEventListener('click', closeModal);
        addButton.addEventListener('click', addField);

        const handleKeyPress = (e) => {
            if (e.key === 'Enter' && !addButton.disabled) {
                addField();
            }
        };

        nameInput.addEventListener('keypress', handleKeyPress);
        valueInput.addEventListener('keypress', handleKeyPress);

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal();
            }
        });

        const escHandler = (e) => {
            if (e.key === 'Escape') {
                closeModal();
            }
        };
        document.addEventListener('keydown', escHandler);

        setTimeout(() => {
            nameInput.focus();
            nameInput.select();
        }, 0);
    }

    updateDocumentWithFrontMatter() {
        this.updateFrontMatterTable();

        const currentMarkdown = this.getMarkdown();

        const newContent = this.getNoteContent(currentMarkdown);
        this.setNoteContent(newContent);
    }

    find(query, caseSensitive = false) {
        const command = searchCommands.find(query, caseSensitive);
        const executed = command(this.view.state, this.view.dispatch);
        
        if (executed) {
            return this.getSearchInfo();
        }
        return null;
    }

    findNext() {
        const command = searchCommands.findNext();
        const executed = command(this.view.state, this.view.dispatch);
        return executed ? this.getSearchInfo() : null;
    }

    findPrevious() {
        const command = searchCommands.findPrevious();
        const executed = command(this.view.state, this.view.dispatch);
        return executed ? this.getSearchInfo() : null;
    }

    clearSearch() {
        const command = searchCommands.clearSearch();
        const executed = command(this.view.state, this.view.dispatch);
        return executed ? this.getSearchInfo() : null;
    }

    getSearchInfo() {
        const command = searchCommands.getSearchInfo();
        return command(this.view.state);
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

    getFrontMatterYAML() {
        if (this.frontMatter && Object.keys(this.frontMatter).length > 0) {
            try {
                const yamlContent = jsYAML.dump(this.frontMatter, {
                    indent: 2,
                    lineWidth: -1,
                    skipInvalid: true,
                    noRefs: true,
                    noCompatMode: true
                });

                return yamlContent;
            } catch (error) {
                console.error('Error formatting YAML front matter:', error);
                let frontMatterContent = "";
                Object.entries(this.frontMatter).forEach(([key, value]) => {
                    frontMatterContent += `${key}: ${value}\n`
                });
                return frontMatterContent;
            }
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
        const frontMatter =  this.getFrontMatterYAML() !== "" ? "---\n" + this.getFrontMatterYAML() + "---\n\n" : "";
        return frontMatter + this.getMarkdown();
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
