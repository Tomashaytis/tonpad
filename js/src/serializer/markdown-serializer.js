import { MarkdownSerializer } from "prosemirror-markdown";

export const markdownSerializer = new MarkdownSerializer(
    {
        paragraph: (state, node) => {
            state.renderInline(node);
            state.closeBlock(node);
        },
        
        heading: (state, node) => {
            state.write("#".repeat(node.attrs.level) + " ");
            state.renderInline(node);
            state.closeBlock(node);
        },
        
        blockquote: (state, node) => {
            state.wrapBlock("> ", null, node, () => state.renderContent(node));
        },
        
        horizontal_rule: (state) => {
            state.write("---\n");
            state.ensureNewLine();
        },
        
        code_block: (state, node) => {
            state.write("```" + (node.attrs.params || "") + "\n");
            state.text(node.textContent, false);
            state.ensureNewLine();
            state.write("```\n");
            state.ensureNewLine();
        },
        
        bullet_list: (state, node) => {
            state.renderList(node, "  ", () => "- ");
        },
        
        ordered_list: (state, node) => {
            const start = node.attrs.order || 1;
            state.renderList(node, "  ", (i) => `${start + i}. `);
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
        
        customTag: (state, node) => {
            const params = Object.entries(node.attrs.params)
                .map(([k, v]) => `${k}="${v}"`)
                .join(" ");
            state.write(`<!-- ${node.attrs.name} ${params} -->\n`);
            state.renderContent(node);
            state.ensureNewLine();
            state.write(`<!-- /${node.attrs.name} -->\n`);
            state.ensureNewLine();
        },
        
        notation_block: (state, node) => {
            state.renderContent(node);
        },

        heading_spec: () => {
        },

        html_comment: () => {
        },
        
        text: (state, node) => {
            state.text(node.text);
        }
    },
    {
        em: { 
            open: "*", 
            close: "*", 
            mixable: true, 
            expelEnclosingWhitespace: true 
        },
        strong: { 
            open: "**", 
            close: "**", 
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
            open: "`", 
            close: "`", 
            mixable: false, 
            expelEnclosingWhitespace: false 
        }
    }
);

/*import { MarkdownSerializer } from "prosemirror-markdown";
import { markdownSchema } from "../schema/markdown-schema.js";

export const markdownSerializer = new MarkdownSerializer(
    {
        paragraph: (state, node) => {
            state.renderInline(node);
            state.closeBlock(node);
        },
        heading: (state, node) => {
            state.write("#".repeat(node.attrs.level) + " ");
            state.renderInline(node);
            state.closeBlock(node);
        },
        heading_focus: (state, node) => {
            state.write("#".repeat(node.attrs.level) + " ");
            state.renderInline(node);
            state.closeBlock(node);
        },
        customTag: (state, node) => {
            const params = Object.entries(node.attrs.params)
                .map(([k, v]) => `${k}="${v}"`)
                .join(" ");
            state.write(`<!-- ${node.attrs.name} ${params} -->\n`);
            state.renderContent(node);
            state.ensureNewLine();
            state.write(`<!-- /${node.attrs.name} -->\n`);
            state.ensureNewLine();
        },
        code_block: (state, node) => {
            state.write("```" + (node.attrs.params || "") + "\n");
            state.text(node.textContent, false);
            state.ensureNewLine();
            state.write("```\n");
            state.ensureNewLine();
        },
        blockquote: (state, node) => {
            state.wrapBlock("> ", null, node, () => state.renderContent(node));
        },
        horizontal_rule: (state) => {
            state.write("---\n");
            state.ensureNewLine();
        },
        bullet_list: (state, node) => {
            state.renderList(node, "  ", () => "- ");
        },
        ordered_list: (state, node) => {
            const start = node.attrs.order || 1;
            state.renderList(node, "  ", (i) => `${start + i}. `);
        },
        list_item: (state, node) => {
            state.renderContent(node);
        },
        image: (state, node) => {
            state.write(`![${node.attrs.alt || ""}](${node.attrs.src}${node.attrs.title ? ` "${node.attrs.title}"` : ""})`);
            state.closeBlock(node);
        },
        hard_break: (state) => {
            state.write("\\\n");
        }
    },
    {
        em: { open: "*", close: "*", mixable: true, expelEnclosingWhitespace: true },
        strong: { open: "**", close: "**", mixable: true, expelEnclosingWhitespace: true },
        link: {
            open: "[",
            close: (state, mark) => `](${mark.attrs.href}${mark.attrs.title ? ` "${mark.attrs.title}"` : ""})`,
            mixable: true,
            expelEnclosingWhitespace: true
        },
        code: { open: "`", close: "`", mixable: false, expelEnclosingWhitespace: false }
    }
);*/