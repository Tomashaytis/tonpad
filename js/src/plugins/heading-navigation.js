import { Plugin } from "prosemirror-state";
import { keydownHandler } from "prosemirror-keymap";

export function headingNavigationPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: keydownHandler({
                "ArrowRight": (state, dispatch, view) => {
                    const { $from } = state.selection;

                    if ($from.parent.type.name === "heading_spec" &&
                        $from.parentOffset >= $from.parent.content.size) {

                        const headingPos = $from.after() + 2;

                        const newSelection = state.selection.constructor.near(state.doc.resolve(headingPos));
                        dispatch(state.tr.setSelection(newSelection));
                        return true;
                    }
                    return false;
                },
                "ArrowLeft": (state, dispatch) => {
                    const { $from } = state.selection;

                    if ($from.parent.type.name === "heading" && $from.parentOffset === 0) {
                        const beforePos = $from.before();
                        const $before = state.doc.resolve(beforePos);
                        const prevNode = $before.nodeBefore;

                        if (prevNode?.type.name === "heading_spec") {
                            const specPos = beforePos - prevNode.nodeSize;
                            const endOfSpec = specPos + prevNode.nodeSize - 2;

                            const tr = state.tr.setSelection(
                                state.selection.constructor.near(state.doc.resolve(endOfSpec), -2)
                            );
                            dispatch(tr.scrollIntoView());
                            return true;
                        }
                    }
                    return false;
                }
            })
        }
    });
}