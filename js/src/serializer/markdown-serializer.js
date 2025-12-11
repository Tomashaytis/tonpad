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
            state.renderInline(node);
            state.write("\n");
        },

        blockquote: (state, node) => {
            state.renderInline(node);
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

        tab_list_item: (state, node) => {
            state.renderInline(node);
            state.write("\n");
        },

        bullet_list_item: (state, node) => {
            state.renderInline(node);
            state.write("\n");
        },

        ordered_list_item: (state, node) => {
            state.renderInline(node);
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
            state.text(node.text, false, true);
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
        tab: {
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true,
            escape: false
        },
        marker: {
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
        italic: {
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
            open: "",
            close: "",
            mixable: true,
            expelEnclosingWhitespace: true
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
        },
        code: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        comment: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        math: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        math_word: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        math_number: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        math_bracket: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
        math_operand: {
            open: "",
            close: "",
            mixable: false,
            expelEnclosingWhitespace: false
        },
    }
);
