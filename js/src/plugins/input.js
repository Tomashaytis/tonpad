import { Plugin } from "prosemirror-state";
import { NodeInputter } from "../core/node-inputter.js";
import { NodeSelector } from "../core/node-selector.js";

export function inputPlugin() {
    return new Plugin({
        props: {
            handleTextInput(view, from, to, text) {
                const { state, dispatch } = view;
                const { selection } = state;

                if (!selection.empty) {
                    const deleteTr = NodeSelector.createDeleteSelectionTransaction(view);
                    if (deleteTr) {
                        const newState = state.apply(deleteTr);
                        return NodeInputter.handleInputInNode(newState, dispatch, text, deleteTr);
                    }
                }

                return NodeInputter.handleInputInNode(state, dispatch, text);
            }
        }
    });
}
