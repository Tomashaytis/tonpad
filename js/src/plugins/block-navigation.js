import { Plugin } from "prosemirror-state";
import { keydownHandler } from "prosemirror-keymap";
import { findNodePosition, getNavigationInfo, getNeighbor } from "../utils/utils.js";

export function blockNavigationPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: keydownHandler({
                "ArrowDown": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    let currentOffset = $from.parentOffset;
                    const navInfo = getNavigationInfo(state);
                    const nextNode = getNeighbor(navInfo, 'next');

                    if (!nextNode) return false;

                    const nextNodePos = findNodePosition(state, nextNode);

                    if ($from.parent.attrs.specOffset && nextNode.attrs.specOffset) {
                        if ($from.parent.attrs.specOffset && currentOffset >= $from.parent.attrs.specOffset) {
                            currentOffset -= $from.parent.attrs.specOffset;
                        }
                        currentOffset += nextNode.attrs.specOffset;
                    } else {
                        if ($from.parent.attrs.specOffset && currentOffset >= $from.parent.attrs.specOffset) {
                            currentOffset -= $from.parent.attrs.specOffset;
                        }
                        if (nextNode.attrs.specOffset) {
                            currentOffset += nextNode.attrs.specOffset;
                        }
                    }

                    const newPos = nextNodePos + Math.min(currentOffset + 1, nextNode.nodeSize - 1);
                    const tr = state.tr.setSelection(state.selection.constructor.near(state.tr.doc.resolve((newPos))));

                    if (dispatch) dispatch(tr);
                    return true;
                },
                "ArrowUp": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    let currentOffset = $from.parentOffset;
                    const navInfo = getNavigationInfo(state);
                    const prevNode = getNeighbor(navInfo, 'previous');

                    if (!prevNode) return false;

                    const prevNodePos = findNodePosition(state, prevNode);

                    if ($from.parent.attrs.specOffset && prevNode.attrs.specOffset) {
                        if ($from.parent.attrs.specOffset && currentOffset >= $from.parent.attrs.specOffset) {
                            currentOffset -= $from.parent.attrs.specOffset;
                        }
                        currentOffset += prevNode.attrs.specOffset;
                    } else {
                        if ($from.parent.attrs.specOffset && currentOffset >= $from.parent.attrs.specOffset) {
                            currentOffset -= $from.parent.attrs.specOffset;
                        }
                        if (prevNode.attrs.specOffset) {
                            currentOffset += prevNode.attrs.specOffset;
                        }
                    }

                    const newPos = prevNodePos + Math.min(currentOffset + 1, prevNode.nodeSize - 1);
                    const tr = state.tr.setSelection(state.selection.constructor.near(state.tr.doc.resolve((newPos))));

                    if (dispatch) dispatch(tr);
                    return true;
                },
            })
        }
    });
}
