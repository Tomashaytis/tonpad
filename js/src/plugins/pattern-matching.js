import { Plugin } from "prosemirror-state";
import { NodeConverter } from "../utils/node-converter.js";

export function patternMatchPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: (view, event) => {
                if (['Delete', 'Backspace', 'Enter'].includes(event.key)) {
                    setTimeout(() => {
                        checkHeadingPattern(view, event.key);
                    }, 10);
                }
                return false;
            }
        }
    });
}

function checkHeadingPattern(view, key) {
    if (!view) return false;

    const { state, dispatch } = view;
    const { $from } = state.selection;

    if ($from.parentOffset === 0) return false;

    if ($from.parent.type.name !== "paragraph") return false;

    const paragraphText = $from.parent.textContent;
    const headingMatch = paragraphText.match(/^(#{1,6})\s(.*)$/);

    if (headingMatch) {
        const level = headingMatch[1].length;
        const headingText = headingMatch[2];

        const headingNode = state.schema.nodes.heading.create(
            { level: level },
            headingText ? state.schema.text(headingText) : null
        );

        const headingBlock = NodeConverter.constructHeading(headingNode);
        const paragraphPos = $from.before(1);

        let tr = state.tr.replaceWith(
            paragraphPos,
            paragraphPos + $from.parent.nodeSize,
            headingBlock
        );

        const containerPos = tr.mapping.map(paragraphPos);
        const specNode = headingBlock.content.child(0);
        const cursorPos = containerPos + specNode.nodeSize - 1;

        dispatch(tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos))));
        return true;
    }

    return false;
}