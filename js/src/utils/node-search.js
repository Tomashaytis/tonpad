export class NodeSearch {
    static getCurrent($from) {
        return {
            node: $from.parent,
            pos: $from.start($from.depth),
            depth: $from.depth
        };
    }

    static getParent($from, levelsUp = 1, isStart = false) {
        const targetDepth = $from.depth - levelsUp;
        if (targetDepth < 0) return null;

        return {
            node: $from.node(targetDepth),
            pos: isStart ? $from.start(targetDepth) : $from.before(targetDepth),
            depth: targetDepth
        };
    }

    static getChildren($from) {
        const currentNode = $from.parent;
        const children = [];

        for (let i = 0; i < currentNode.childCount; i++) {
            const child = currentNode.child(i);
            const childPos = $from.start($from.depth) + (i > 0 ? currentNode.child(i - 1).nodeSize : 0);

            children.push({
                node: child,
                pos: childPos,
                index: i
            });
        }

        return children;
    }

    static findPrevious($from, targetDepth = null) {
        const depth = targetDepth !== null ? targetDepth : $from.depth - 1;
        const currentIndex = $from.index(depth);

        if (currentIndex === 0) return null;

        const parent = $from.node(depth);
        const prevNode = parent.child(currentIndex - 1);

        let prevPos = $from.start(depth);
        for (let i = 0; i < currentIndex - 1; i++) {
            prevPos += parent.child(i).nodeSize;
        }

        return {
            node: prevNode,
            pos: prevPos,
            index: currentIndex - 1,
            depth: depth
        };
    }

    static findNext($from) {
        const currentIndex = $from.index();
        const parent = $from.node($from.depth - 1);

        if (currentIndex >= parent.childCount - 1) return null;

        const nextNode = parent.child(currentIndex + 1);
        let nextPos = $from.start($from.depth - 1);

        for (let i = 0; i <= currentIndex; i++) {
            nextPos += parent.child(i).nodeSize;
        }

        return {
            node: nextNode,
            pos: nextPos,
            index: currentIndex + 1
        };
    }

    static getSiblings($from) {
        const parent = $from.node($from.depth - 1);
        const siblings = [];
        let currentPos = $from.start($from.depth - 1);

        for (let i = 0; i < parent.childCount; i++) {
            const node = parent.child(i);
            siblings.push({
                node: node,
                pos: currentPos,
                index: i,
                isCurrent: i === $from.index()
            });
            currentPos += node.nodeSize;
        }

        return siblings;
    }
}