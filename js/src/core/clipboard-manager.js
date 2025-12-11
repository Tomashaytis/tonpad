import { NodeInputter } from "./node-inputter.js";
import { NodeSelector } from "./node-selector.js";
import { markdownSerializer } from "../serializer/markdown-serializer.js";

export class ClipboardManager {
    constructor(view, editorBridge) {
        this.view = view;
        this.editorBridge = editorBridge;
    }

    copy() {
        const { state } = this.view;
        const { selection, doc } = state;
        const { $from, $to } = selection;

        if ($from.pos === $to.pos) {
            return false;
        }

        const fragment = doc.content.cut($from.pos, $to.pos);
        const tempDoc = state.schema.topNodeType.create({}, fragment);
        const markdownText = markdownSerializer.serialize(tempDoc);
        const cleanedText = markdownText.replace(/\n{3,}/g, '\n\n').trim();

        if (this.editorBridge && this.editorBridge.setClipboardText) {
            this.editorBridge.setClipboardText(cleanedText);
        }

        return cleanedText;
    }

    cut() {
        const copied = this.copy();
        if (!copied) return false;

        const deleteTr = NodeSelector.createDeleteSelectionTransaction(this.view);
        if (deleteTr) {
            this.view.dispatch(deleteTr);
            return true;
        }
        return false;
    }

    paste() {
        let clipboardText = "";
        if (this.editorBridge && this.editorBridge.getClipboardText) {
            clipboardText = this.editorBridge.getClipboardText();
        }

        if (!clipboardText) return false;

        const { state, dispatch } = this.view;
        const { selection } = state;

        if (clipboardText.length > 1) {
            return NodeInputter.handlePasteInNode(this.view, state, dispatch, clipboardText);
        }
        if (clipboardText.length === 1) {
            return NodeInputter.handleInputInNode(state, dispatch, clipboardText);
        }
        return false;
    }

    async copyToSystemClipboard(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            try {
                await navigator.clipboard.writeText(text);
            } catch (err) {
                console.warn('Failed to copy to system clipboard:', err);
            }
        }
    }
}