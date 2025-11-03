import { Plugin } from "prosemirror-state";
import { NodeInputter } from "../utils/node-inputter.js";

export function inputPlugin() {
    return new Plugin({
        props: {
            handleTextInput(view, from, to, text) {
                const { state } = view;
                const { selection } = state;
                const $from = selection.$from;

                if ($from.parent.type.name === 'spec_block') {
                    return NodeInputter.handleInputInSpec(view, from, to, text);
                } else {
                    return NodeInputter.handleInputInNormalNode(view, from, to, text);
                }
            }
        }
    });
}
