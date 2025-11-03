import { Plugin } from "prosemirror-state";
import { NodeMerger } from "../utils/node-merger";
import { NodeInputter } from "../utils/node-inputter";

export function deletePlugin() {
    return new Plugin({
        props: {
            handleKeyDown(view, event) {
                if (event.key === "Delete") {
                    const { state, dispatch } = view;
                    const { $from, empty } = state.selection;

                    if (!empty) return false;

                    const isAtBlockEnd = $from.parentOffset === $from.parent.textContent.length;

                    if (isAtBlockEnd) {
                        const tr = NodeMerger.mergeDown(state, state.selection);
                        
                        if (tr) {
                            event.preventDefault();
                            dispatch(tr);
                            return true;
                        }
                    } else {
                        const from = $from.pos - 1;
                        const to = $from.pos;
                        return NodeInputter.handleDeleteChar(view, from, to, false);
                    }
                    
                    return false;
                }
                return false;
            }
        }
    });
}
