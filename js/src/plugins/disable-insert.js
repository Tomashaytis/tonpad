import { Plugin } from "prosemirror-state";

export function disableInsertPlugin() {
    return new Plugin({
        props: {
            handleKeyDown: (view, event) => {
                if (event.key === "Insert") {
                    event.preventDefault();
                    event.stopPropagation();
                    return true;
                }
                return false;
            }
        }
    });
}