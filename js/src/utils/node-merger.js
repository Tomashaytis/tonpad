import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";
import { TextSelection } from "prosemirror-state";

export class NodeMerger {
    static mergeUp(state, selection) {
        return this.findAndMerge(state, selection, 'up');
    }

    static mergeDown(state, selection) {
        return this.findAndMerge(state, selection, 'down');
    }

    static findAndMerge(state, selection, direction) {
        const { $from } = selection;
        return this.findNeighborRecursive(state, $from.pos, $from.depth, direction);
    }

    static findNeighborRecursive(state, pos, depth, direction) {
        if (depth < 0) return null;

        const neighborInfo = this.findNeighbor(state, pos, depth, direction);

        if (neighborInfo) {
            return this.performMerge(state, pos, neighborInfo, direction);
        }

        const $pos = state.doc.resolve(pos);
        const parentDepth = depth - 1;

        if (parentDepth >= 0) {
            const parentPos = $pos.start(depth);
            return this.findNeighborRecursive(state, parentPos, parentDepth, direction);
        }

        return null;
    }

    static findNeighbor(state, pos, depth, direction) {
        const $pos = state.doc.resolve(pos);

        if (depth < 0) return null;

        const parent = $pos.node(depth);
        const currentIndex = $pos.index(depth);

        let neighborIndex;
        if (direction === 'up') {
            neighborIndex = currentIndex - 1;
        } else {
            neighborIndex = currentIndex + 1;
        }

        if (neighborIndex < 0 || neighborIndex >= parent.childCount) {
            return null;
        }

        const neighborNode = parent.child(neighborIndex);

        let neighborPos = $pos.start(depth);
        for (let i = 0; i < neighborIndex; i++) {
            neighborPos += parent.child(i).nodeSize;
        }

        return {
            node: neighborNode,
            pos: neighborPos,
            depth: depth + 1,
            index: neighborIndex,
            parent: parent
        };
    }

    static getOffsetToNode(parent, targetIndex) {
        let offset = 0;
        for (let i = 0; i < targetIndex; i++) {
            offset += parent.child(i).nodeSize;
        }
        return offset;
    }

    static performMerge(state, currentPos, neighborInfo, direction) {
        const $current = state.doc.resolve(currentPos);

        const mergeParams = this.getMergeParams($current, neighborInfo, direction);

        if (!mergeParams) return null;

        let { from, to, mergedParagraphs, cursorPos } = mergeParams;

        const tr = this.executeMerge(state, from, to, mergedParagraphs, cursorPos);

        return tr;
    }

    static getMergeParams($current, neighborInfo, direction) {
        const parentDepth = neighborInfo.depth - 1;
        const parent = neighborInfo.parent;

        const currentIndexAtDepth = $current.index(parentDepth + 1);
        let currentPosAtDepth = $current.start(parentDepth + 1);
        for (let i = 0; i < currentIndexAtDepth; i++) {
            currentPosAtDepth += parent.child(i).nodeSize;
        }

        const currentNode = $current.parent;
        const neighborPosAtDepth = neighborInfo.pos;

        let from = Math.min(neighborPosAtDepth, currentPosAtDepth);
        const to = from + (neighborInfo.node.textContent.length != 0 ? neighborInfo.node.nodeSize : 1) + (currentNode.textContent.length != 0 ? currentNode.nodeSize : 1);

        const currentParagraphs = NodeConverter.destructNode(currentNode);
        const neighborParagraphs = NodeConverter.destructNode(neighborInfo.node);

        let mergedParagraphs;
        let cursorPos = null;

        if (direction === 'up') {
            if (neighborParagraphs.length > 0 && currentParagraphs.length > 0) {
                const lastNeighborPara = neighborParagraphs[neighborParagraphs.length - 1];
                const firstCurrentPara = currentParagraphs[0];

                const mergedContent = lastNeighborPara.content.append(firstCurrentPara.content);
                const mergedParagraph = markdownSchema.nodes.paragraph.create(
                    lastNeighborPara.attrs,
                    mergedContent,
                    lastNeighborPara.marks
                );

                mergedParagraphs = [
                    ...neighborParagraphs.slice(0, -1),
                    mergedParagraph,
                    ...currentParagraphs.slice(1)
                ];

                let mergedParaPos = from;
                for (let i = 0; i < neighborParagraphs.length - 1; i++) {
                    mergedParaPos += neighborParagraphs[i].nodeSize;
                }
                cursorPos = mergedParaPos + lastNeighborPara.content.size + 1;
            } else {
                mergedParagraphs = [...neighborParagraphs, ...currentParagraphs];
            }
        } else {
            from -= 1
            if (currentParagraphs.length > 0 && neighborParagraphs.length > 0) {
                const lastCurrentPara = currentParagraphs[currentParagraphs.length - 1];
                const firstNeighborPara = neighborParagraphs[0];

                const mergedContent = lastCurrentPara.content.append(firstNeighborPara.content);
                const mergedParagraph = markdownSchema.nodes.paragraph.create(
                    lastCurrentPara.attrs,
                    mergedContent,
                    lastCurrentPara.marks
                );

                mergedParagraphs = [
                    ...currentParagraphs.slice(0, -1),
                    mergedParagraph,
                    ...neighborParagraphs.slice(1)
                ];

                let mergedParaPos = from;
                for (let i = 0; i < currentParagraphs.length - 1; i++) {
                    mergedParaPos += currentParagraphs[i].nodeSize;
                }
                cursorPos = mergedParaPos + lastCurrentPara.content.size + 1;

                
            } else {
                mergedParagraphs = [...currentParagraphs, ...neighborParagraphs];
            }
        }

        return { from, to, mergedParagraphs, cursorPos };
    }

    static executeMerge(state, from, to, mergedParagraphs, cursorPos) {
        const tr = state.tr;

        const reconstructedData = this.applyReconstruction(tr, mergedParagraphs, from, to, cursorPos);

        reconstructedData.tr.setSelection(TextSelection.create(reconstructedData.tr.doc, reconstructedData.cursorPos));

        return reconstructedData.tr;
    }

    static applyReconstruction(tr, mergedParagraphs, from, to, cursorPos) {
        const reconstructor = new NodeReconstructor();

        const reconstructed = reconstructor.applyBlockRules(mergedParagraphs, from);
        const blockReconstructed = reconstructed;
        const hasBlockChanges = blockReconstructed.some((para, index) => para !== mergedParagraphs[index]);

        let finalParagraphs = mergedParagraphs;

        if (hasBlockChanges) {
            finalParagraphs = blockReconstructed;
        }

        const reconstructedParagraphs = finalParagraphs.map(paragraph => {
            const markReconstruction = reconstructor.reconstructMarksInNode(paragraph);
            if (markReconstruction && markReconstruction !== paragraph) {
                return markReconstruction;
            }
            return paragraph;
        });

        tr.replaceWith(from, to, reconstructedParagraphs);

        return { tr, cursorPos };
    }

    static setCursorSelection(tr, cursorPos) {
        if (cursorPos !== null) {
            const resolvedPos = tr.doc.resolve(cursorPos);
            if (resolvedPos.pos >= 0 && resolvedPos.pos <= tr.doc.content.size) {
                tr.setSelection(TextSelection.create(tr.doc, resolvedPos.pos));
            }
        }
    }
}
