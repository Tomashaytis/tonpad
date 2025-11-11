import { Plugin } from "prosemirror-state";
import { NodeSelector } from "../utils/node-selector.js";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeReconstructor } from "../utils/node-reconstructor.js";

let sourceNodePos = null;

export function copyPlugin() {
    return new Plugin({
        props: {
            handleDOMEvents: {
                copy(view, e) {
                    return NodeSelector.copySelectionToClipboard(view, e);
                },
                cut(view, e) {
                    const copied = NodeSelector.copySelectionToClipboard(view, e);
                    if (!copied) return false;

                    const deleteTr = NodeSelector.createDeleteSelectionTransaction(view);
                    if (deleteTr) {
                        view.dispatch(deleteTr);
                        return true;
                    }
                    return false;
                },
                dragstart(view, e) {
                    const { state } = view;
                    const { selection } = state;
                    const $from = selection.$from;

                    sourceNodePos = $from.before();
                },
                drop(view, e) {
                    setTimeout(() => {
                        cleanupSpecMarks(view);

                        if (sourceNodePos) {
                            cleanupSourceNode(view, sourceNodePos);
                            sourceNodePos = null;
                        }
                    }, 0);
                },
            }
        }
    });
}

function cleanupSpecMarks(view) {
    const { state, dispatch } = view;
    const { selection } = state;

    let tr = state.tr;

    const $pos = selection.$from;
    const node = $pos.parent;
    const nodePos = $pos.before();
    const cursorOffset = $pos.parentOffset;

    const paragraph = NodeConverter.constructParagraph(node.textContent)

    const reconstructor = new NodeReconstructor();
    const reconstructed = reconstructor.applyBlockRules([paragraph], 0);

    tr = tr.replaceWith(nodePos, nodePos + node.nodeSize, reconstructed[0]);
    tr.setSelection(selection.constructor.near(tr.doc.resolve(nodePos + cursorOffset + 1)));

    dispatch(tr);
}

function cleanupSourceNode(view, nodePos) {
    const { state, dispatch } = view;
    const { doc } = state;
    
    const node = doc.nodeAt(nodePos);
    if (!node) return;
    
    let tr = state.tr;
    
    const paragraph = NodeConverter.constructParagraph(node.textContent);
    const reconstructor = new NodeReconstructor();
    const reconstructed = reconstructor.applyBlockRules([paragraph], 0);
    
    tr = tr.replaceWith(nodePos, nodePos + node.nodeSize, reconstructed[0]);
    
    dispatch(tr);
}
