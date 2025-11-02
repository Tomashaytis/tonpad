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

    static constructTabListItem(text, level) {
        const tabListNode = markdownSchema.nodes.tab_list_item.create(
            {
                level: level,
                renderAs: "span"
            },
            text ? markdownSchema.text(text) : null
        );

        let index = 0;
        const textNodes = Array(level).fill(null).map(() =>
            markdownSchema.text("\t", [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        const specNode = markdownSchema.nodes.spec_block.create(
            {
                level: level,
                specClass: "tab-list-spec",
            },
            textNodes
        );

        return markdownSchema.nodes.notation_block.create(
            {
                level: level,
                type: "tab_list",
                layout: "row",
            },
            [specNode, tabListNode]
        );
    }

    static constructBulletListItem(text, level, marker = '-') {
        const bulletListNode = markdownSchema.nodes.bullet_list_item.create(
            {
                level: level,
                marker: marker,
                renderAs: "span"
            },
            text ? markdownSchema.text(text) : null
        );

        let index = 0;
        const textNodes = Array(level).fill(null).map(() =>
            markdownSchema.text("\t", [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        textNodes.push(markdownSchema.text(marker + ' ', [markdownSchema.marks.marker.create()]))

        const specNode = markdownSchema.nodes.spec_block.create(
            {
                level: level,
                specClass: "bullet-list-spec",
            },
            textNodes
        );

        return markdownSchema.nodes.notation_block.create(
            {
                level: level,
                type: "bullet_list",
                layout: "row",
            },
            [specNode, bulletListNode]
        );
    }

    static constructOrderedListItem(text, level, number = 1) {
        const bulletListNode = markdownSchema.nodes.ordered_list_item.create(
            {
                level: level,
                number: number,
                renderAs: "span"
            },
            text ? markdownSchema.text(text) : null
        );

        let index = 0;
        const textNodes = Array(level).fill(null).map(() =>
            markdownSchema.text("\t", [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        textNodes.push(markdownSchema.text(`${number}. `, [markdownSchema.marks.marker.create()]))

        const specNode = markdownSchema.nodes.spec_block.create(
            {
                level: level,
                specClass: "ordered-list-spec",
            },
            textNodes
        );

        return markdownSchema.nodes.notation_block.create(
            {
                level: level,
                type: "ordered_list",
                layout: "row",
            },
            [specNode, bulletListNode]
        );
    }

    static constructEm(text) {
        return Fragment.from(this.createWrappedMark(["*", "*"], text, markdownSchema.marks.em));
    }

    static constructStrong(text) {
        return Fragment.from(this.createWrappedMark(["**", "**"], text, markdownSchema.marks.strong));
    }

    static constructStrike(text) {
        return Fragment.from(this.createWrappedMark(["~~", "~~"], text, markdownSchema.marks.strike));
    }

    static constructHighlight(text) {
        return Fragment.from(this.createWrappedMark(["==", "=="], text, markdownSchema.marks.highlight));
    }

    static constructUnderline(text) {
        return Fragment.from(this.createWrappedMark(["__", "__"], text, markdownSchema.marks.underline));
    }

    static constructCode(text) {
        return Fragment.from(this.createWrappedMark(["`", "`"], text, markdownSchema.marks.code, 'code-mark-spec-left', 'code-mark-spec-right'));
    }

    static constructUrl(url) {
        if (!url || url === "") {
            return Fragment.from([]);
        }
        return Fragment.from([
            markdownSchema.text(url, [markdownSchema.marks.link.create({
                href: url
            })])
        ]);
    }

    static constructEmail(email) {
        if (!email || email === "") {
            return Fragment.from([]);
        }
        return Fragment.from([
            markdownSchema.text(email, [markdownSchema.marks.link.create({
                href: 'mailto:' + email
            })])
        ]);
    }

    static constructTag(tag) {
        if (!tag || tag === "") {
            return Fragment.from([]);
        }
        return Fragment.from([
            markdownSchema.text(tag, [markdownSchema.marks.link.create({
                href: 'tag:' + tag,
                linkClass: 'tag'
            })])
        ]);
    }

    static constructNoteLink(text, href = "") {
        const nodes = [
            markdownSchema.text("[", [markdownSchema.marks.spec.create()])
        ];

        if (text && text !== "") {
            nodes.push(markdownSchema.text(text, [markdownSchema.marks.link.create({
                href: 'tonpad://' + href
            })]));
        }

        nodes.push(markdownSchema.text("]", [markdownSchema.marks.spec.create()]));

        return Fragment.from(nodes);
    }

    static constructLink(text, href = "") {
        const nodes = [
            markdownSchema.text("[", [markdownSchema.marks.spec.create()])
        ];

        if (text && text !== "") {
            nodes.push(markdownSchema.text(text, [markdownSchema.marks.link.create({
                href: href === "" ? "#" : href
            })]));
        }

        nodes.push(markdownSchema.text("]", [markdownSchema.marks.spec.create()]));

        nodes.push(markdownSchema.text("(", [markdownSchema.marks.spec.create()]));

        if (href && href !== "") {
            nodes.push(markdownSchema.text(href, [markdownSchema.marks.link.create({
                href: href === "" ? "#" : href
            })]));
        }

        nodes.push(markdownSchema.text(")", [markdownSchema.marks.spec.create()]));

        return Fragment.from(nodes);
    }

    static destructNode(node) {
        const specialHandlers = {
            //'code_block': this.destructCodeBlock,
        };

        if (node.type.name === "notation_block" && specialHandlers[node.attrs.type]) {
            return specialHandlers[node.attrs.type].call(this, node);
        }

        return [this.constructParagraph(node.textContent)];
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

    static extractNotationBlockRowText(notationBlock) {
        const specContent = notationBlock.child(0).textContent;
        const nodeContent = notationBlock.child(1).textContent;

        return {
            specContent: specContent,
            nodeContent: nodeContent
        };
    }

    static createWrappedMark(delimiters, text, mark, leftMarkClass = 'mark-spec', rightMarkClass = 'mark-spec') {
        const nodes = [
            markdownSchema.text(delimiters[0], [markdownSchema.marks.spec.create({
                specClass: leftMarkClass
            })])
        ];

        if (text && text !== "") {
            nodes.push(markdownSchema.text(text, [mark.create()]));
        }

        nodes.push(markdownSchema.text(delimiters[1], [markdownSchema.marks.spec.create({
            specClass: rightMarkClass
        })]));
        return nodes;
    }
}