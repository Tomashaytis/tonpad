import { MarkdownSerializer } from "prosemirror-markdown";

export class ExtendedMarkdownSerializer extends MarkdownSerializer {
    serializeFragment(fragment, options = {}) {
        const state = new this.State(this.nodes, this.marks, options);
        
        let nodes = [];
        fragment.forEach(node => nodes.push(node));
        
        nodes.forEach((node, index) => {
            const handler = this.nodes[node.type.name];
            if (handler) {
                handler(state, node);
            }
            
            if (node.isBlock && index < nodes.length - 1) {
                state.write("\n");
            }
        });
        
        return state.out;
    }
}

export const markdownSerializer = new ExtendedMarkdownSerializer(
    {
        paragraph: (state, node) => {
            state.renderInline(node);
            state.write("\n");
        },

        heading: (state, node) => {
            state.write("#".repeat(node.attrs.level) + " ");
            state.renderInline(node);
            state.write("\n");
        },

        notation_block: (state, node) => {
            if (node.attrs.type === 'heading') {
                state.write("#".repeat(node.attrs.level) + " ");
                node.content.forEach(child => {
                    if (child.type.name === 'heading') {
                        state.renderInline(child);
                    }
                });
                state.write("\n");
            } else if (node.attrs.type === 'blockquote') {
                state.write("> ");
                node.content.forEach(child => {
                    if (child.type.name === 'blockquote') {
                        state.renderInline(child);
                    }
                });
                state.write("\n");
            } else {
                state.renderContent(node);
            }
        },

        blockquote: (state, node) => {
            state.wrapBlock("> ", null, node, () => state.renderContent(node));
            state.write("\n");
        },

        horizontal_rule: (state) => {
            state.write("---\n");
        },

        code_block: (state, node) => {
            state.write("```" + (node.attrs.params || "") + "\n");
            state.text(node.textContent, false);
            state.write("\n```\n");
        },

        bullet_list: (state, node) => {
            state.renderList(node, "  ", () => "- ");
            state.write("\n");
        },

        ordered_list: (state, node) => {
            const start = node.attrs.order || 1;
            state.renderList(node, "  ", (i) => `${start + i}. `);
            state.write("\n");
        },

        list_item: (state, node) => {
            state.renderContent(node);
        },

        image: (state, node) => {
            state.write(`![${node.attrs.alt || ""}](${node.attrs.src}${node.attrs.title ? ` "${node.attrs.title}"` : ""})`);
        },

        hard_break: (state) => {
            state.write("\\\n");
        },

        spec_block: (state, node) => {
        },

        html_comment: (state, node) => {
        },

        text: (state, node) => {
            state.text(node.text);
        }
    },
    {
        spec: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true,
            escape: false
        },
        em: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
        },
        strong: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
        },
        link: {
            open: "[",
            close: (state, mark) => `](${mark.attrs.href}${mark.attrs.title ? ` "${mark.attrs.title}"` : ""})`,
            mixable: true,
            expelEnclosingWhitespace: true
        },
        code: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        strike: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
        },
        highlight: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
        },
        underline: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
        }
    }
);
