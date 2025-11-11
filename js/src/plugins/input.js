import { Plugin } from "prosemirror-state";
import { NodeInputter } from "../utils/node-inputter.js";
import { NodeSelector } from "../utils/node-selector.js";

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
            },
            handlePaste: (view, event) => {
                const { state, dispatch } = view;
                const { selection } = state;

                const clipboardText = event.clipboardData?.getData('text/plain');

                if (!selection.empty) {
                    const deleteTr = NodeSelector.createDeleteSelectionTransaction(view);
                    if (deleteTr) {
                        const newState = state.apply(deleteTr);

                        if (clipboardText.length > 1) {
                            return NodeInputter.handlePasteInNode(view, newState, dispatch, clipboardText, deleteTr);
                        }
                        if (clipboardText.length == 1) {
                            return NodeInputter.handleInputInNode(newState, dispatch, clipboardText, deleteTr);
                        }
                        return false;
                    }
                }

                if (clipboardText.length > 1) {
                    return NodeInputter.handlePasteInNode(view, state, dispatch, clipboardText);
                }
                if (clipboardText.length == 1) {
                    return NodeInputter.handleInputInNode(state, dispatch, clipboardText);
                }
                return false;
            }
        }
    });
}
