import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { NodeInputter } from "./node-inputter.js";
import { TextSelection } from "prosemirror-state";
import { markdownSerializer } from "../serializer/markdown-serializer.js";

export class NodeSelector {
    static createDeleteSelectionTransaction(view) {
        const { state } = view;
        const { selection } = state;
        const { $from, $to } = selection;

        if (selection.empty) {
            return state.tr;
        }

        const fromPos = $from.pos;

        const firstNode = $from.parent;
        const lastNode = $to.parent;

        const firstNodePos = $from.before();
        const lastNodePos = $to.before();

        const textBeforeSelection = firstNode.textContent.slice(0, $from.parentOffset);
        const textAfterSelection = lastNode.textContent.slice($to.parentOffset);

        const newText = textBeforeSelection + textAfterSelection;
        let newNode = NodeConverter.constructParagraph(newText);

        const reconstructor = new NodeReconstructor();
        const reconstructed = reconstructor.applyBlockRules([newNode], firstNodePos);
        newNode = reconstructed[0];

        const tr = state.tr.replaceWith(firstNodePos, lastNodePos + lastNode.nodeSize, newNode);

        const cursorPos = fromPos;
        tr.setSelection(TextSelection.create(tr.doc, cursorPos));

        return tr;
    }

    static copySelectionToClipboard(view, event) {
        const { state } = view;
        const { selection, doc } = state;
        const { $from, $to } = selection;

        const fragment = doc.content.cut($from.pos, $to.pos);

        const tempDoc = state.schema.topNodeType.create({}, fragment);
        const markdownText = markdownSerializer.serialize(tempDoc);
        const cleanedText = markdownText.replace(/\n{3,}/g, '\n\n').trim();

        if (event.clipboardData) {
            event.clipboardData.setData('text/plain', cleanedText);
            event.clipboardData.setData('text/markdown', cleanedText);
            event.preventDefault();
        } else if (event.dataTransfer) {
            event.dataTransfer.setData('text/plain', cleanedText);
            event.dataTransfer.setData('text/markdown', cleanedText);
        }

        return cleanedText;
    }
}