import { NodeConverter } from "./node-converter.js";
import { NodeReconstructor } from "./node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";

export class NodeSplitter {
    static splitBlockNode(state, dispatch, baseTr = null) {
        const { selection } = state;
        const { $from } = selection;

        const node = $from.parent;
        const nodePos = $from.before();
        const nodeSize = node.nodeSize;
        const cursorOffset = $from.parentOffset;

        const textContent = node.textContent;
        const beforeText = textContent.slice(0, cursorOffset);
        const afterText = textContent.slice(cursorOffset);

        let newNodes = [];

        let upperNode, lowerNode;

        if (beforeText) {
            upperNode = NodeConverter.constructParagraph(markdownSchema.text(beforeText));
        } else {
            upperNode = NodeConverter.constructParagraph();
        }

        let useSpecOffset = false;

        if (node.type.name === 'blockquote') {
            if (cursorOffset === node.attrs.specOffset && afterText === '') {
                upperNode = NodeConverter.constructParagraph();
                newNodes.push(upperNode);
            } else {
                const spec = node.textContent.slice(0, node.attrs.specOffset);
                lowerNode = NodeConverter.constructParagraph(markdownSchema.text(spec + afterText));
                useSpecOffset = true;
                newNodes.push(upperNode);
                newNodes.push(lowerNode);
            }
        } else if (node.type.name === 'tab_list_item') {
            if (cursorOffset === node.attrs.specOffset && afterText === '') {
                const spec = node.attrs.tabs.slice(0, -1).join('');
                upperNode = NodeConverter.constructParagraph(spec ? markdownSchema.text(spec) : null);
                newNodes.push(upperNode);
            } else {
                const spec = node.textContent.slice(0, node.attrs.specOffset);
                lowerNode = NodeConverter.constructParagraph(markdownSchema.text(spec + afterText));
                useSpecOffset = true;
                newNodes.push(upperNode);
                newNodes.push(lowerNode);
            }
        } else if (node.type.name === 'bullet_list_item') {
            if (cursorOffset === node.attrs.specOffset && afterText === '') {
                let spec;
                if (node.attrs.level > 0) {
                    spec = node.attrs.tabs.slice(0, -1).join('') + `${node.attrs.marker} `;
                } else {
                    spec = '';
                }
                upperNode = NodeConverter.constructParagraph(spec ? markdownSchema.text(spec) : null);
                newNodes.push(upperNode);
            } else {
                const spec = node.textContent.slice(0, node.attrs.specOffset);
                lowerNode = NodeConverter.constructParagraph(markdownSchema.text(spec + afterText));
                useSpecOffset = true;
                newNodes.push(upperNode);
                newNodes.push(lowerNode);
            }
        } else if (node.type.name === 'ordered_list_item') {
            if (cursorOffset === node.attrs.specOffset && afterText === '') {
                let spec;
                if (node.attrs.level > 0) {
                    spec = node.attrs.tabs.slice(0, -1).join('') + `${node.attrs.number}. `;
                } else {
                    spec = '';
                }
                upperNode = NodeConverter.constructParagraph(spec ? markdownSchema.text(spec) : null);
                newNodes.push(upperNode);
            } else {
                const spec = node.attrs.tabs.join('') + `${node.attrs.number + 1}. `;
                lowerNode = NodeConverter.constructParagraph(markdownSchema.text(spec + afterText));
                useSpecOffset = true;
                newNodes.push(upperNode);
                newNodes.push(lowerNode);
            }
        } else {
            if (afterText) {
                lowerNode = NodeConverter.constructParagraph(markdownSchema.text(afterText));
            } else {
                lowerNode = NodeConverter.constructParagraph();
            }
            newNodes.push(upperNode);
            newNodes.push(lowerNode);
        }

        let tr = baseTr || state.tr;
        const reconstructedNodes = this.applyReconstruction(newNodes);
        tr = tr.replaceWith(nodePos, nodePos + nodeSize, reconstructedNodes);

        if (reconstructedNodes.length > 1) {
            let specOffset = 0;
            if (useSpecOffset) {
                specOffset = reconstructedNodes[1].attrs.specOffset;
            }
            const cursorPos = nodePos + reconstructedNodes[0].nodeSize + 1 + specOffset;
            tr.setSelection(selection.constructor.near(tr.doc.resolve(cursorPos)));
        }

        dispatch(tr);
        return true;
    }

    static applyReconstruction(nodes) {
        const reconstructor = new NodeReconstructor();
        let reconstructedNodes = [];

        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];

            const reconstructed = reconstructor.applyBlockRules([node], 0);
            let reconstructedNode = reconstructed[0];

            const markReconstruction = reconstructor.reconstructMarksInNode(reconstructedNode);
            if (markReconstruction) {
                reconstructedNode = markReconstruction;
            }

            reconstructedNodes.push(reconstructedNode);
        }

        return reconstructedNodes;
    }
}
