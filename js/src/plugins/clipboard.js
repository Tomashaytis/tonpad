import { Plugin } from "prosemirror-state";
import { NodeSelector } from "../core/node-selector.js";
import { NodeConverter } from "../core/node-converter.js";
import { NodeInputter } from "../core/node-inputter.js";
import { NodeReconstructor } from "../core/node-reconstructor.js";

let sourceNodePos = null;

export function clipboardPlugin() {
    return new Plugin({
        props: {
            handleDOMEvents: {
                copy(view, e) {
                    const copied = NodeSelector.copySelectionToClipboard(view, e);
                    if (window.editorBridge && window.editorBridge.setClipboardText) {
                        window.editorBridge.setClipboardText(copied);
                    }
                    return true;
                },
                cut(view, e) {
                    const copied = NodeSelector.copySelectionToClipboard(view, e);
                    if (!copied) return false;

                    if (window.editorBridge && window.editorBridge.setClipboardText) {
                        window.editorBridge.setClipboardText(copied);
                    }

                    const deleteTr = NodeSelector.createDeleteSelectionTransaction(view);
                    if (deleteTr) {
                        view.dispatch(deleteTr);
                        return true;
                    }
                    return false;
                },
                paste(view, e) {
                    e.preventDefault();

                    let clipboardText = "";

                    if (e.clipboardData && e.clipboardData.getData) {
                        clipboardText = e.clipboardData.getData('text/plain');
                    }

                    if (!clipboardText && window.editorBridge && window.editorBridge.getClipboardText) {
                        clipboardText = window.editorBridge.getClipboardText();
                    }

                    if (!clipboardText) return false;

                    const { state, dispatch } = view;
                    const { selection } = state;

                    if (!selection.empty) {
                        const deleteTr = NodeSelector.createDeleteSelectionTransaction(view);
                        if (deleteTr) {
                            const newState = state.apply(deleteTr);

                            if (clipboardText.length > 1) {
                                return NodeInputter.handlePasteInNode(view, newState, dispatch, clipboardText, deleteTr);
                            }
                            if (clipboardText.length === 1) {
                                return NodeInputter.handleInputInNode(newState, dispatch, clipboardText, deleteTr);
                            }
                            return false;
                        }
                    }

                    if (clipboardText.length > 1) {
                        return NodeInputter.handlePasteInNode(view, state, dispatch, clipboardText);
                    }
                    if (clipboardText.length === 1) {
                        return NodeInputter.handleInputInNode(state, dispatch, clipboardText);
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
