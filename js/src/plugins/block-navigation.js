import { Plugin } from "prosemirror-state";
import { keydownHandler } from "prosemirror-keymap";
import { TextSelection } from "prosemirror-state";

export function blockNavigationPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: keydownHandler({
                "ArrowDown": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    const navInfo = getNavigationInfo(state);
                    const nextNode = getNeighbor(navInfo, 'next');

                    if (navInfo.targetNode.type.name === 'notation_block' && navInfo.targetNode.attrs.layout === 'col') {
                        return false;
                    }
                    if (nextNode.type.name === 'notation_block' && nextNode.attrs.layout === 'col') {
                        return false;
                    }

                    if (nextNode) {
                        if (nextNode.type.name === 'notation_block' && nextNode.childCount >= 2) {
                            const nextNodePos = findNodePosition(state, nextNode);
                            if (nextNodePos !== -1) {
                                const secondChildPos = nextNodePos + nextNode.child(0).nodeSize + 2;
                                const $pos = state.doc.resolve(secondChildPos);

                                const targetOffset = Math.min($from.parentOffset, $pos.parent.content.size, nextNode.nodeSize - 2);
                                const targetPos = $pos.pos + targetOffset;

                                const selection = TextSelection.create(state.doc, targetPos);
                                dispatch(state.tr.setSelection(selection));
                                return true;
                            }
                        } else {
                            const nextNodePos = findNodePosition(state, nextNode);
                            if (nextNodePos !== -1) {
                                const $pos = state.doc.resolve(nextNodePos);
                                const targetOffset = Math.min($from.parentOffset, $pos.parent.content.size, nextNode.nodeSize - 2);
                                const targetPos = $pos.pos + targetOffset + 1;

                                const selection = TextSelection.create(state.doc, targetPos);
                                dispatch(state.tr.setSelection(selection));
                                return true;
                            }
                        }
                    }
                    return false;
                },
                "ArrowUp": (state, dispatch, view) => {
                    const { $from } = state.selection;
                    const currentOffset = $from.parentOffset;
                    const navInfo = getNavigationInfo(state);
                    const prevNode = getNeighbor(navInfo, 'previous');

                    if (navInfo.targetNode.type.name === 'notation_block' && navInfo.targetNode.attrs.layout === 'col') {
                        return false;
                    }

                    if (navInfo && navInfo.targetNode && navInfo.targetNode.type.name === 'notation_block' && $from.parent.type.name != "spec_block") {
                        const notationBlock = navInfo.targetNode;
                        const notationPos = findNodePosition(state, notationBlock);
                        if (notationPos !== -1) {
                            const firstChildPos = notationPos + 2;
                            const selection = TextSelection.create(state.doc, firstChildPos);
                            dispatch(state.tr.setSelection(selection));
                            return true;
                        }
                    }

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
                            const selection = TextSelection.create(state.doc, targetPos);
                            dispatch(state.tr.setSelection(selection));
                            return true;
                        }
                    }
                    return false;
                },
            })
        }
    });
}

function findNodePosition(state, targetNode) {
    let foundPos = -1;
    state.doc.descendants((node, pos) => {
        if (node === targetNode) {
            foundPos = pos;
            return false;
        }
    });
    return foundPos;
}

function getNavigationInfo(state) {
    const { $from } = state.selection;
    const parentNode = $from.depth > 0 ? $from.node($from.depth - 1) : null;

    let targetNode, container;

    if (parentNode && parentNode.type.name === 'notation_block') {
        targetNode = parentNode;
        container = $from.depth > 1 ? $from.node($from.depth - 2) : null;
    } else {
        targetNode = $from.parent;
        container = parentNode;
    }

    if (!container) return null;

    let index = -1;
    for (let i = 0; i < container.childCount; i++) {
        if (container.child(i) === targetNode) {
            index = i;
            break;
        }
    }

    if (index === -1) return null;

    return {
        targetNode,
        container,
        index
    };
}

function getNeighbor(navInfo, direction) {
    if (!navInfo) return null;

    const { container, index } = navInfo;

    if (direction === 'next' && index < container.childCount - 1) {
        return container.child(index + 1);
    }

    if (direction === 'previous' && index > 0) {
        return container.child(index - 1);
    }

    return null;
}