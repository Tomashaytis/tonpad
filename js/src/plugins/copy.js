import { Plugin } from "prosemirror-state";
import { markdownSerializer } from "../serializer/markdown-serializer.js";

export function copyPlugin() {
    return new Plugin({
        props: {
            handleDOMEvents: {
                copy(view, e) {
                    const { state } = view;
                    const { selection, doc } = state;
                    const { $from, $to } = selection;

                    if ($from.pos === $to.pos) return false;

                    const fragment = doc.content.cut($from.pos, $to.pos);

                    const tempDoc = state.schema.topNodeType.create({}, fragment);

                    const markdownText = markdownSerializer.serialize(tempDoc);

                    const cleanedText = markdownText.replace(/\n{3,}/g, '\n\n').trim();

                    e.clipboardData.setData('text/plain', cleanedText);
                    e.clipboardData.setData('text/markdown', cleanedText);
                    e.preventDefault();

                    return true;
                }
            }
        }
    });
}