import { Plugin } from "prosemirror-state";
import { keydownHandler } from "prosemirror-keymap";
import { TextSelection } from "prosemirror-state";
import { correctCursorPos, findNodePosition, getNavigationInfo, getNeighbor } from "../utils/utils.js";

export function blockNavigationPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: keydownHandler({
                "ArrowDown": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    const navInfo = getNavigationInfo(state);
                    const nextNode = getNeighbor(navInfo, 'next');
                    let currentOffset = $from.parentOffset;
                    if ($from.parent.type.name === 'spec_block') {
                        currentOffset = 0;
                    }
                    
                    let tr = state.tr;

                    if (navInfo.targetNode.type.name === 'notation_block' && navInfo.targetNode.attrs.layout === 'col') {
                        return false;
                    }

                    if (nextNode) {
                        if (nextNode.type.name === 'notation_block' && nextNode.attrs.layout === 'col') {
                            return false;
                        }

                        if (nextNode.type.name === 'notation_block' && nextNode.childCount >= 2) {
                            const nextNodePos = findNodePosition(state, nextNode);
                            if (nextNodePos !== -1) {
                                const secondChildPos = nextNodePos + nextNode.child(0).nodeSize + 2;
                                const $pos = state.doc.resolve(secondChildPos);

                                const targetOffset = Math.min(currentOffset, $pos.parent.content.size, nextNode.nodeSize - 2);
                                let targetPos = $pos.pos + targetOffset;

                                const correctedSelection = correctCursorPos(tr, targetPos);
                                if (correctedSelection) {
                                    tr = tr.setSelection(correctedSelection);
                                } else {
                                    tr = tr.setSelection(TextSelection.create(tr.doc, targetPos));
                                }
                                
                                dispatch(tr);
                                return true;
                            }
                        } else {
                            const nextNodePos = findNodePosition(state, nextNode);
                            if (nextNodePos !== -1) {
                                const $pos = state.doc.resolve(nextNodePos);
                                const targetOffset = Math.min(currentOffset, $pos.parent.content.size, nextNode.nodeSize - 2);
                                let targetPos = $pos.pos + targetOffset + 1;

                                const correctedSelection = correctCursorPos(tr, targetPos);
                                if (correctedSelection) {
                                    tr = tr.setSelection(correctedSelection);
                                } else {
                                    tr = tr.setSelection(TextSelection.create(tr.doc, targetPos));
                                }
                                
                                dispatch(tr);
                                return true;
                            }
                        }
                    }
                    return false;
                },
                "ArrowUp": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    let currentOffset = $from.parentOffset;
                    if ($from.parent.type.name === 'spec_block') {
                        currentOffset = 0;
                    }
                    const navInfo = getNavigationInfo(state);
                    const prevNode = getNeighbor(navInfo, 'previous');
                    
                    let tr = state.tr;

                    if (navInfo.targetNode.type.name === 'notation_block' && navInfo.targetNode.attrs.layout === 'col') {
                        return false;
                    }

                    /*if (navInfo && navInfo.targetNode && navInfo.targetNode.type.name === 'notation_block' && !($from.parent.type.name === 'spec_block' && currentOffset === 0)) {
                        const notationBlock = navInfo.targetNode;
                        const notationPos = findNodePosition(state, notationBlock);
                        if (notationPos !== -1) {
                            let targetPos = notationPos + 2;

                            const correctedSelection = correctCursorPosWithoutChanges(tr, targetPos);
                            if (correctedSelection) {
                                tr = tr.setSelection(correctedSelection);
                            } else {
                                tr = tr.setSelection(TextSelection.create(tr.doc, targetPos));
                            }
                            
                            dispatch(tr);
                            return true;
                        }
                    }*/

                    if (prevNode.type.name === 'notation_block' && prevNode.attrs.layout === 'col') {
                        return false;
                    }

                    if (prevNode) {
                        let targetPos;

                        if (prevNode.type.name === 'notation_block' && prevNode.childCount >= 2) {
                            const prevNodePos = findNodePosition(state, prevNode);
                            if (prevNodePos !== -1) {
                                const secondChildPos = prevNodePos + prevNode.child(0).nodeSize + 2;
                                const $pos = state.doc.resolve(secondChildPos);
                                targetPos = $pos.pos + Math.min(currentOffset, $pos.parent.content.size, prevNode.nodeSize - 2);
                            }
                        } else {
                            const prevNodePos = findNodePosition(state, prevNode);
                            if (prevNodePos !== -1) {
                                const $pos = state.doc.resolve(prevNodePos);
                                targetPos = $pos.pos + Math.min(currentOffset, $pos.parent.content.size, prevNode.nodeSize - 2) + 1;
                            }
                        }

                        if (targetPos !== undefined) {
                            const correctedSelection = correctCursorPos(tr, targetPos);
                            if (correctedSelection) {
                                tr = tr.setSelection(correctedSelection);
                            } else {
                                tr = tr.setSelection(TextSelection.create(tr.doc, targetPos));
                            }
                            
                            dispatch(tr);
                            return true;
                        }
                    }
                    return false;
                },
            })
        }
    });
}