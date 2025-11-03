import { Plugin, PluginKey } from "prosemirror-state";
import { Decoration, DecorationSet } from "prosemirror-view";

export function hideSpecPlugin() {
    const pluginKey = new PluginKey("hideSpecPlugin");

    return new Plugin({
        key: pluginKey,
        state: {
            init(_, { doc }) {
                return createDecorations(doc, false);
            },
            apply(tr, oldDecoSet, oldState, newState) {
                const notationBlockFocused = isNotationBlockFocused(newState);

                if (
                    tr.docChanged ||
                    tr.selectionSet ||
                    oldState.selection.$from.pos !== newState.selection.$from.pos ||
                    oldState.selection.$to.pos !== newState.selection.$to.pos
                ) {
                    return createDecorations(newState.doc, notationBlockFocused);
                }
                return oldDecoSet;
            }
        },
        props: {
            decorations(state) {
                return pluginKey.getState(state);
            },
            handleKeyDown() {
                return false;
            },
            handleTextInput() {
                return false;
            }
        }
    });
}

function createDecorations(doc, notationBlockFocused = false) {
    const decorations = [];

    doc.descendants((node, pos) => {
        if (node.type.name === "notation_block") {
            const hasHeadingSpec = node.content.content.some(child => child.type.name === "spec_block");
            if (hasHeadingSpec) {
                const specNode = node.content.content.find(child => child.type.name === "spec_block");
                const specPos = pos + node.content.content.indexOf(specNode) + 1;

                decorations.push(
                    Decoration.node(specPos, specPos + specNode.nodeSize, {
                        class: notationBlockFocused ? "" : "spec-hidden"
                    })
                );
            }
        }
    });

    return DecorationSet.create(doc, decorations);
}

function isNotationBlockFocused(state) {
    const { $from } = state.selection;

    if ($from.parent.type.name === "notation_block") {
        return true;
    }

    const resolved = state.doc.resolve($from.pos);
    for (let i = 0; i < resolved.depth; i++) {
        const node = resolved.node(i);
        if (node.type.name === "notation_block") {
            return true;
        }
    }

    return false;
}