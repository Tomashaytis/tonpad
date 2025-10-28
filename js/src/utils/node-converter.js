import { markdownSchema } from "../schema/markdown-schema.js";
import { Fragment } from "prosemirror-model";

export class NodeConverter {
    static constructParagraph(content = "") {
        if (typeof content === 'string') {
            return markdownSchema.nodes.paragraph.create(null, content ? markdownSchema.text(content) : null);
        }
        return markdownSchema.nodes.paragraph.create(null, content);
    }

    static constructHeading(text, level) {
        const headingNode = markdownSchema.nodes.heading.create(
            {
                level: level,
                renderAs: "span"
            },
            text ? markdownSchema.text(text) : null
        );

        const specNode = markdownSchema.nodes.spec_block.create(
            {
                level: level,
                specClass: "heading-spec",
            },
            markdownSchema.text("#".repeat(level) + " ")
        );

        return markdownSchema.nodes.notation_block.create(
            {
                type: "heading",
                layout: "row",
                level: level,
            },
            [specNode, headingNode]
        );
    }

    static constructBlockquote(text) {
        const blockquoteNode = markdownSchema.nodes.blockquote.create(
            {
                renderAs: "span"
            },
            text ? markdownSchema.text(text) : null
        );

        const specNode = markdownSchema.nodes.spec_block.create(
            {
                specClass: "blockquote-spec",
            },
            markdownSchema.text("> ")
        );

        return markdownSchema.nodes.notation_block.create(
            {
                type: "blockquote",
                layout: "row",
            },
            [specNode, blockquoteNode]
        );
    }

    static constructEm(text) {
        return Fragment.from([
            markdownSchema.text("*", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.em.create()]),
            markdownSchema.text("*", [markdownSchema.marks.spec.create()])
        ]);
    }

    static constructStrong(text) {
        return Fragment.from([
            markdownSchema.text("**", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.strong.create()]),
            markdownSchema.text("**", [markdownSchema.marks.spec.create()])
        ]);
    }

    static constructStrike(text) {
        return Fragment.from([
            markdownSchema.text("~~", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.strike.create()]),
            markdownSchema.text("~~", [markdownSchema.marks.spec.create()])
        ]);
    }

    static constructHighlight(text) {
        return Fragment.from([
            markdownSchema.text("==", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.highlight.create()]),
            markdownSchema.text("==", [markdownSchema.marks.spec.create()])
        ]);
    }

    static constructUnderline(text) {
        return Fragment.from([
            markdownSchema.text("__", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.underline.create()]),
            markdownSchema.text("__", [markdownSchema.marks.spec.create()])
        ]);
    }

    static constructCode(text) {
        return Fragment.from([
            markdownSchema.text("`", [markdownSchema.marks.spec.create()]),
            markdownSchema.text(text, [markdownSchema.marks.code.create()]),
            markdownSchema.text("`", [markdownSchema.marks.spec.create()])
        ]);
    }

    static destructHeading(headingBlock) {
        return this.constructParagraph(headingBlock.textContent);
    }

    static destructBlockquote(blockquoteBlock) {
        return this.constructParagraph(blockquoteBlock.textContent);
    }

    static destructNode(node) {
        switch (node.type.name) {
            case "notation_block":
                switch (node.attrs.type) {
                    case "heading":
                        return [this.destructHeading(node)];
                    case "blockquote":
                        return [this.destructBlockquote(node)];
                    default:
                        return [this.constructParagraph(node.textContent)];
                }
            case "paragraph":
                return [this.destructParagraph(node)];
            case "heading":
                return [this.destructHeading(node)];
            case "blockquote":
                return [this.destructBlockquote(node)];
            default:
                return [this.constructParagraph(node.textContent)];
        }
    }

    static destructParagraph(paragraphNode) {
        const plainText = this.extractPlainText(paragraphNode);
        return this.constructParagraph(plainText);
    }

    static extractPlainText(node) {
        let text = '';

        node.descendants((childNode, pos, parent) => {
            if (childNode.isText) {
                text += childNode.text;
            }
        });

        return text;
    }

}