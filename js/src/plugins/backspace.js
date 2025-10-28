import { Plugin } from "prosemirror-state";
import { NodeMerger } from "../utils/node-merger";
import { NodeInputter } from "../utils/node-inputter";

let isMerging = false;

export function backspacePlugin() {
    return new Plugin({
        props: {
            handleKeyDown(view, event) {
                if (event.key === "Backspace") {
                    const { state, dispatch } = view;
                    const { $from, empty } = state.selection;

                    if (!empty) return false;

                    const isAtBlockStart = $from.parentOffset === 0;

                    if (isAtBlockStart && !isMerging) {
                        isMerging = true;
                        const tr = NodeMerger.mergeUp(state, state.selection);
                        
                        if (tr) {
                            event.preventDefault();
                            dispatch(tr);
                            setTimeout(() => { isMerging = false; }, 0);
                            return true;
                        }
                    } else if (!isMerging) {
                        const from = $from.pos - 1;
                        const to = $from.pos;
                        return NodeInputter.handleDeleteChar(view, from, to);
                    }
                }
                return false;
            }
        }
    });
}
