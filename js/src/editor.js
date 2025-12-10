import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { dropCursor } from "prosemirror-dropcursor";
import { gapCursor } from "prosemirror-gapcursor";
import { history, } from "prosemirror-history";
import { markdownSchema } from "./schema/markdown-schema.js";
import { NodeConverter } from "./core/node-converter.js";
import { NodeInputter } from "./core/node-inputter.js";
import { NodeReconstructor } from "./core/node-reconstructor.js";
import { NodeSelector } from "./core/node-selector.js";
import { markdownSerializer } from "./serializer/markdown-serializer.js";
import { blockNavigationPlugin } from "./plugins/block-navigation.js"
import { keymapPlugin } from "./plugins/keymap.js";
import { inputPlugin } from "./plugins/input.js"
import { disableInsertPlugin } from "./plugins/disable-insert.js"
import { hideSpecPlugin } from "./plugins/hide-spec.js"
import { doubleClickPlugin } from "./plugins/double-click.js"
import { searchPlugin, searchCommands } from "./plugins/search.js"
import { clipboardPlugin } from "./plugins/clipboard.js"
import { wordCounterPlugin } from "./plugins/word-counter.js"
import jsYAML from 'js-yaml';

export class Editor {
    constructor(target, mode = 'note', content = '') {
        if (!target) throw new Error('Target element required');

        this.mode = mode;

        const docContent = this.parseDoc(content);

        if (this.mode == 'note' || this.mode == 'template') {
            this.frontMatter = docContent.frontMatter;

            this.frontMatterTable = document.getElementById('frontmatter-table');
            this.frontMatterBody = document.getElementById('frontmatter-body');
            this.updateFrontMatterTable();
        }

        if (this.mode == 'snippet') {
            docContent.markdown = content;
        }

        if (this.mode == 'note' || this.mode == 'snippet') {
            const doc = this.createDocumentFromText(docContent.markdown);

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
                }
            });

            this.rebuildTree();
        }
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
        tr.setMeta('addToHistory', false);

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

                const reconstructed = reconstructor.applyBlockRules([paragraph], change.pos);
                const reconstructedNode = reconstructed[0];

                if (reconstructedNode !== paragraph) {
                    tr = tr.replaceWith(change.pos, change.pos + paragraph.nodeSize, reconstructedNode);
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
            clipboardPlugin(),
            keymapPlugin(this),
            dropCursor(),
            gapCursor(),
            blockNavigationPlugin(),
            inputPlugin(),
            disableInsertPlugin(),
            searchPlugin(),
            hideSpecPlugin(),
            doubleClickPlugin(),
            wordCounterPlugin(),
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
                return 'Field name cannot be empty';
            }

            if (this.frontMatter[fieldName]) {
                return 'Field with this name already exists';
            }

            if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(fieldName)) {
                return 'Field name may contain only letters, numbers and underscores, and must start with a letter or underline';
            }

            return '';
        };

        const error = validateFieldName(newKey);
        if (error) {
            this.showErrorDialog('Error', error);
            row.querySelector('input[type="text"]').value = oldKey;
            return;
        }

        const value = this.frontMatter[oldKey];
        delete this.frontMatter[oldKey];
        this.frontMatter[newKey] = value;

        if (window.editorBridge && window.editorBridge.onFrontMatterChanged) {
            window.editorBridge.onFrontMatterChanged('updateKey', oldKey, value, newKey, value);
        }

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
        const oldValue = this.frontMatter[key];
        if (oldValue === newValue) return;

        this.frontMatter[key] = newValue;

        if (window.editorBridge && window.editorBridge.onFrontMatterChanged) {
            window.editorBridge.onFrontMatterChanged('updateValue', key, oldValue, key, newValue);
        }

        this.updateDocumentWithFrontMatter();
    }

    deleteFrontMatterField(key) {
        this.showConfirmDialog(
            `Remove field "${key}"?`,
            () => {
                const oldValue = this.frontMatter[key];
                delete this.frontMatter[key];

                if (Object.keys(this.frontMatter).length === 0) {
                    this.frontMatter = {};
                }

                if (window.editorBridge && window.editorBridge.onFrontMatterChanged) {
                    window.editorBridge.onFrontMatterChanged('delete', key, oldValue, null, null);
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
            <button class="frontmatter-confirm-btn cancel">Cancel</button>
            <button class="frontmatter-confirm-btn delete">Remove</button>
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
        <h3 class="frontmatter-add-title">Add new field</h3>
        <div class="frontmatter-add-form">
            <div class="frontmatter-add-field">
                <label class="frontmatter-add-label">Field name:</label>
                <input type="text" class="frontmatter-add-input" placeholder="Enter the field name">
                <div class="frontmatter-add-error"></div>
            </div>
            <div class="frontmatter-add-field">
                <label class="frontmatter-add-label">Value:</label>
                <input type="text" class="frontmatter-add-input" placeholder="Enter the value">
                <div class="frontmatter-add-error"></div>
            </div>
            <div class="frontmatter-add-buttons">
                <button class="frontmatter-add-btn cancel">Cancel</button>
                <button class="frontmatter-add-btn primary" disabled>Add</button>
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
                return 'Field name cannot be empty';
            }

            if (this.frontMatter[fieldName]) {
                return 'Field with this name already exists';
            }

            const invalidChars = /[:{}\[\]]/;
            if (invalidChars.test(fieldName)) {
                return 'Field name cannot contain characters : { } [ ]';
            }

            if (fieldName !== fieldName.trim()) {
                return 'Field name must not begin or end with spaces';
            }

            const mayNeedQuotes = /[#&*!|>'"%@`-]|\s/;
            if (mayNeedQuotes.test(fieldName)) {
                return 'The field name contains characters that may require quotes in YAML. This is acceptable, but can make reading difficult.';
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

            if (window.editorBridge && window.editorBridge.onFrontMatterChanged) {
                window.editorBridge.onFrontMatterChanged('add', null, null, fieldName, fieldValue);
            }

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
            setTimeout(() => {
                const nextCommand = searchCommands.getCurrentResult();
                const currentResult = nextCommand(this.view.state);

                if (currentResult) {
                    this.view.focus();

                    const cursorPos = currentResult.from;

                    const cursorElement = this.view.domAtPos(cursorPos).node;
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
                }
            }, 0);

            return JSON.stringify(this.getSearchInfo());
        }
        return JSON.stringify(null);
    }

    findNext() {
        const command = searchCommands.findNext();
        const executed = command(this.view.state, this.view.dispatch);
        if (executed) {
            setTimeout(() => {
                const nextCommand = searchCommands.getCurrentResult();
                const currentResult = nextCommand(this.view.state);

                if (currentResult) {
                    this.view.focus();

                    const cursorPos = currentResult.from;

                    const cursorElement = this.view.domAtPos(cursorPos).node;
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
                }
            }, 0);

            return JSON.stringify(this.getSearchInfo());
        }
        return JSON.stringify(null);
    }

    findPrevious() {
        const command = searchCommands.findPrevious();
        const executed = command(this.view.state, this.view.dispatch);
        if (executed) {
            setTimeout(() => {
                const nextCommand = searchCommands.getCurrentResult();
                const currentResult = nextCommand(this.view.state);

                if (currentResult) {
                    this.view.focus();

                    const cursorPos = currentResult.from;

                    const cursorElement = this.view.domAtPos(cursorPos).node;
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
                }
            }, 0);

            return JSON.stringify(this.getSearchInfo());
        }
        return JSON.stringify(null);
    }

    clearSearch() {
        const command = searchCommands.clearSearch();
        const executed = command(this.view.state, this.view.dispatch);
        return JSON.stringify(executed ? this.getSearchInfo() : null);
    }

    getSearchInfo() {
        const command = searchCommands.getSearchInfo();
        return command(this.view.state);
    }

    setNoteContent(content) {
        const docContent = this.parseDoc(content);
        if (this.mode == 'note' || this.mode == 'template') {
            this.frontMatter = docContent.frontMatter;
            this.updateFrontMatterTable();
        }

        if (this.mode == 'snippet') {
            docContent.markdown = content;
        }

        if (this.mode == 'note' || this.mode == 'snippet') {
            const newDoc = this.createDocumentFromText(docContent.markdown);
            const tr = this.view.state.tr.replaceWith(0, this.view.state.doc.content.size, newDoc.content);
            tr.setMeta('addToHistory', false);

            this.view.dispatch(tr);

            this.rebuildTree();
        }
    }

    setFrontMatter(yamlString) {
        this.frontMatter = this.parseYAML(yamlString);
        this.updateFrontMatterTable();
    }

    insertSnippet(snippetContent) {
        if (snippetContent) {
            const { state, dispatch } = this.view;
            const { selection } = state;

            if (!selection.empty) {
                const deleteTr = NodeSelector.createDeleteSelectionTransaction(this.view);
                if (deleteTr) {
                    const newState = state.apply(deleteTr);
                    return NodeInputter.handlePasteInNode(this.view, newState, dispatch, snippetContent, deleteTr, false);
                }
            }

            return NodeInputter.handlePasteInNode(this.view, state, dispatch, snippetContent, null, false);
        }
    }

    format(style) {
        const { state, dispatch } = this.view;
        const { from, to } = state.selection;

        const selectedNodes = [];
        state.doc.content.forEach((child, offset, index) => {
            const childPos = offset;
            const blockStart = childPos;
            const blockEnd = childPos + child.nodeSize;

            if (from < blockEnd && to > blockStart) {
                let markerTextLength = 0;

                child.descendants((node, pos) => {
                    if (node.isText) {
                        const hasNonFormatMark = node.marks.some(mark =>
                            mark.type.name === 'spec' && mark.attrs.type !== 'format'
                        );
                        const hasMarker = node.marks.some(mark => mark.type.name === 'marker');
                        const hasTab = node.marks.some(mark => mark.type.name === 'tab');

                        if (hasNonFormatMark || hasMarker || hasTab) {
                            markerTextLength += node.text.length;
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return true;
                });

                selectedNodes.push({
                    node: child,
                    pos: childPos,
                    index: index,
                    blockStart,
                    blockEnd,
                    markerTextLength,
                    realIntersectsFrom: Math.max(from, blockStart + 1),
                    intersectsFrom: Math.max(from, blockStart + 1 + markerTextLength),
                    intersectsTo: Math.min(to, blockEnd),
                    type: child.type.name,
                    hasText: child.textContent.length > 0,
                    isEmpty: child.childCount === 0,
                    text: child.textContent
                });
            }
        });

        if (style === 'clear') {
            const paragraphs = [];

            selectedNodes.forEach(nodeInfo => {
                let nodeText = nodeInfo.text;

                const textStart = nodeInfo.blockStart + 1;
                const markStartInText = nodeInfo.intersectsFrom - textStart;
                const markEndInText = nodeInfo.intersectsTo - textStart;

                const safeMarkStart = Math.max(0, Math.min(markStartInText, nodeText.length));
                const safeMarkEnd = Math.max(0, Math.min(markEndInText, nodeText.length));

                const selectedText = nodeText.slice(safeMarkStart, safeMarkEnd);

                const markers = [
                    '_', '*', '~~', '**', '__', '==', '%%', '$', '`',
                ];

                let cleanedSelectedText = selectedText;
                markers.forEach(marker => {
                    if (marker.length === 1) {
                        const regex = new RegExp(`\\${marker}([^\\${marker}]*?)\\${marker}`, 'g');
                        cleanedSelectedText = cleanedSelectedText.replace(regex, '$1');
                    } else {
                        const escapedMarker = marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                        const regex = new RegExp(`${escapedMarker}(.*?)${escapedMarker}`, 'gs');
                        cleanedSelectedText = cleanedSelectedText.replace(regex, '$1');
                    }
                });

                const beforeText = nodeText.slice(0, safeMarkStart);
                const afterText = nodeText.slice(safeMarkEnd);
                const finalText = beforeText + cleanedSelectedText + afterText;

                const paragraph = NodeConverter.constructParagraph(finalText);
                paragraphs.push(paragraph);
            });

            const reconstructor = new NodeReconstructor();
            const reconstructed = reconstructor.applyBlockRules(paragraphs, 0);

            let tr = state.tr;
            let startReplacePos = selectedNodes[0].blockStart;
            let endReplacePos = selectedNodes[selectedNodes.length - 1].blockEnd;
            tr = tr.replaceWith(startReplacePos, endReplacePos, reconstructed);

            const cursorPos = from;
            tr = tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            dispatch(tr);
            return;
        }

        let marker = '';
        switch (style) {
            case 'bold':
                marker = '**';
                break;
            case 'italic':
                marker = '_';
                break;
            case 'underline':
                marker = '__';
                break;
            case 'strikethrough':
                marker = '~~';
                break;
            case 'highlight':
                marker = '==';
                break;
            case 'comment':
                marker = '%%';
                break;
            case 'math':
                marker = '$';
                break;
            case 'code':
                marker = '`';
                break;
            default:
                return;
        }

        const paragraphs = [];
        selectedNodes.forEach(nodeInfo => {
            if (!nodeInfo.hasText && selectedNodes.length > 1) {
                let paragraph = NodeConverter.constructParagraph();
                paragraphs.push(paragraph);
                return;
            }

            const nodeText = nodeInfo.text;

            const textStart = nodeInfo.blockStart + 1;

            let markStartInText = nodeInfo.intersectsFrom - textStart;
            const markEndInText = nodeInfo.intersectsTo - textStart;

            if (style === 'comment') {
                markStartInText = nodeInfo.realIntersectsFrom - textStart;
            }


            const safeMarkStart = Math.max(0, Math.min(markStartInText, nodeText.length));
            const safeMarkEnd = Math.max(0, Math.min(markEndInText, nodeText.length));

            const beforeText = nodeText.slice(0, safeMarkStart);
            const markText = nodeText.slice(safeMarkStart, safeMarkEnd);
            const afterText = nodeText.slice(safeMarkEnd);

            const escapeMarker = marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const markerRegex = new RegExp(escapeMarker, 'g');

            const cleanBeforeText = beforeText.replace(markerRegex, '');
            const cleanMarkText = markText.replace(markerRegex, '');
            const cleanAfterText = afterText.replace(markerRegex, '');

            let paragraph = NodeConverter.constructParagraph(cleanBeforeText + marker + cleanMarkText + marker + cleanAfterText);
            paragraphs.push(paragraph);
        });

        const reconstructor = new NodeReconstructor();
        const reconstructed = reconstructor.applyBlockRules(paragraphs, 0);

        let tr = state.tr;
        let startReplacePos = selectedNodes[0].blockStart;
        let endReplacePos = selectedNodes[selectedNodes.length - 1].blockEnd;
        tr = tr.replaceWith(startReplacePos, endReplacePos, reconstructed);

        const cursorOffset = marker.length;
        const cursorPos = from + cursorOffset;

        tr = tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
        dispatch(tr);
    }

    paragraph(type) {
        const { state, dispatch } = this.view;
        const { from, to } = state.selection;

        const selectedNodes = [];
        state.doc.content.forEach((child, offset, index) => {
            const childPos = offset;
            const blockStart = childPos;
            const blockEnd = childPos + child.nodeSize;

            if (from < blockEnd && to > blockStart) {
                let markerTextLength = 0;
                let tabs = '';
                let tabLevel = 0;

                child.descendants((node, pos) => {
                    if (node.isText) {
                        const hasNonFormatMark = node.marks.some(mark =>
                            mark.type.name === 'spec' && mark.attrs.type !== 'format'
                        );
                        const hasMarker = node.marks.some(mark => mark.type.name === 'marker');
                        const hasTab = node.marks.some(mark => mark.type.name === 'tab');

                        if (hasNonFormatMark || hasMarker || hasTab) {
                            markerTextLength += node.text.length;
                            if (hasTab) {
                                tabs += node.text;
                                tabLevel += 1;
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return true;
                });

                selectedNodes.push({
                    node: child,
                    pos: childPos,
                    index: index,
                    blockStart,
                    blockEnd,
                    markerTextLength,
                    type: child.type.name,
                    hasText: child.textContent.length > 0,
                    isEmpty: child.childCount === 0,
                    text: child.textContent,
                    tabs,
                    tabLevel
                });
            }
        });

        let marker = '';
        let useTabs = false;
        switch (type) {
            case 'heading1':
                marker = '# ';
                break;
            case 'heading2':
                marker = '## ';
                break;
            case 'heading3':
                marker = '### ';
                break;
            case 'heading4':
                marker = '#### ';
                break;
            case 'heading5':
                marker = '##### ';
                break;
            case 'heading6':
                marker = '###### ';
                break;
            case 'quote':
                marker = '> ';
                break;
            case 'bullet-list':
                marker = '- ';
                useTabs = true;
                break;
            case 'ordered-list':
                marker = '';
                useTabs = true;
                break;
            case 'body':
                marker = '';
                useTabs = true;
                break;
            default:
                return;
        }

        let prevTabLevel = 0;
        let number = 0;
        let first = true;
        let cursorPos = from;

        const paragraphs = [];
        selectedNodes.forEach(nodeInfo => {
            if (type == 'ordered-list') {
                if (nodeInfo.tabLevel == prevTabLevel) {
                    number += 1;
                } else {
                    number = 1;
                }

                marker = `${number}. `;
            }
            prevTabLevel = nodeInfo.tabLevel;

            const nodeText = nodeInfo.text;

            const afterText = nodeText.slice(nodeInfo.markerTextLength);

            const beforeText = useTabs ? nodeInfo.tabs : '';

            let paragraph = NodeConverter.constructParagraph(beforeText + marker + afterText);
            paragraphs.push(paragraph);

            if (first) {
                cursorPos = nodeInfo.blockStart + nodeInfo.tabs.length + marker.length + 1;
                first = false;
            }
        });

        const reconstructor = new NodeReconstructor();
        const reconstructed = reconstructor.applyBlockRules(paragraphs, 0);

        let tr = state.tr;
        let startReplacePos = selectedNodes[0].blockStart;
        let endReplacePos = selectedNodes[selectedNodes.length - 1].blockEnd;
        tr = tr.replaceWith(startReplacePos, endReplacePos, reconstructed);

        tr = tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
        dispatch(tr);
    }

    link(type) {
        const { state, dispatch } = this.view;
        const { from, to, $from, $to } = state.selection;

        if ($from.parent !== $to.parent) {
            return;
        }

        const currentNode = $from.parent;
        const nodeStart = $from.start();
        const nodeEnd = $from.end();
        const nodeText = currentNode.textContent;

        const relativeFrom = from - nodeStart;
        const relativeTo = to - nodeStart;

        const beforeText = nodeText.slice(0, relativeFrom);
        const linkText = nodeText.slice(relativeFrom, relativeTo);
        const afterText = nodeText.slice(relativeTo);

        console.log(beforeText, linkText, afterText);

        let newNodeText = '';
        let cursorPos = nodeStart + beforeText.length;
        switch (type) {
            case 'note':
                newNodeText = beforeText + '[[' + linkText + ']]' + afterText;
                cursorPos += linkText.length + 2;
                break;
            case 'external':
                newNodeText = beforeText + '[' + linkText + ']()' + afterText;
                cursorPos += linkText.length + 3;
                break;
            default:
                return;
        }

        const paragraph = NodeConverter.constructParagraph(newNodeText);

        const reconstructor = new NodeReconstructor();
        const reconstructed = reconstructor.applyBlockRules([paragraph], 0);

        let tr = state.tr;
        tr = tr.replaceWith(nodeStart - 1, nodeEnd, reconstructed);

        tr = tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
        dispatch(tr);
    }

    canCreateLinks() {
        const { state } = this.view;
        const { $from, $to } = state.selection;
        return $from.parent === $to.parent;
    }

    selectAll() {
        const { state, dispatch } = this.view;

        const selection = state.selection.constructor.between(
            state.doc.resolve(0),
            state.doc.resolve(state.doc.content.size)
        );

        if (dispatch) {
            dispatch(state.tr.setSelection(selection));
        }

        return true;
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
        const frontMatter = this.getFrontMatterYAML() !== "" ? "---\n" + this.getFrontMatterYAML() + "---\n\n" : "";
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

        if (this.frontMatterTable) {
            this.frontMatterTable.style.display = 'none';
            if (this.frontMatterBody) {
                this.frontMatterBody.innerHTML = '';
            }
        }

        this.view = null;
        this.frontMatter = null;
        this.frontMatterTable = null;
    }
}