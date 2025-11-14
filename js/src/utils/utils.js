import { TextSelection } from "prosemirror-state";

export function findTabs(spaces) {
    if (!spaces) {
        return [];
    }

    let index = 0;
    const tabs = []

    while (index < spaces.length) {
        if (spaces.substring(index, index + 4) === "    ") {
            tabs.push('    ');
            index += 4;
        } else if (spaces[index] === "\t") {
            tabs.push('\t');
            index += 1;
        } else {
            index++;
        }
    }

    return tabs;
}

export function correctCursorPos(tr, cursorPos) {
    const $newPos = tr.doc.resolve(cursorPos);

    const notationBlock = findParentNotationBlock($newPos);

    if (notationBlock && notationBlock.attrs.layout === "row") {
        if (notationBlock.child(1) === $newPos.parent && $newPos.parentOffset == 0) {
            cursorPos = cursorPos - 2;
            return TextSelection.create(tr.doc, cursorPos);
        }
    }
    return null;
}

export function findParentNotationBlock($pos) {
    for (let depth = $pos.depth; depth >= 0; depth--) {
        const node = $pos.node(depth);
        if (node && node.type.name === 'notation_block') {
            return node;
        }
    }
    return null;
}

export function findNodePosition(state, targetNode) {
    let foundPos = -1;
    state.doc.descendants((node, pos) => {
        if (node === targetNode) {
            foundPos = pos;
            return false;
        }
    });
    return foundPos;
}

export function getNavigationInfo(state) {
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

export function getNeighbor(navInfo, direction) {
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