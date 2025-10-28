import { Plugin } from "prosemirror-state";
import { NodeSplitter } from "../utils/node-splitter.js";

export const enterPlugin = () => {
    return new Plugin({
        props: {
            handleKeyDown(view, event) {
                if (event.key === "Enter") {
                    return NodeSplitter.handleEnter(view, event);
                }
                return false;
            }
        }
    });
};