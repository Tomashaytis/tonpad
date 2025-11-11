import { markdownSchema } from "../schema/markdown-schema.js";
import { Fragment } from "prosemirror-model";

export class NodeConverter {
    static constructParagraph(content = "") {
        if (typeof content === 'string') {
            return markdownSchema.nodes.paragraph.create(null, content ? markdownSchema.text(content) : null);
        }
        return markdownSchema.nodes.paragraph.create(null, content);
    }

    static constructParagraphWithMarks(contentNodes) {
        const children = [];

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        return markdownSchema.nodes.paragraph.create(null, contentNodes);
    }

    static constructHeading(contentNodes, level) {
        const specOffset = level + 1;

        const children = [
            markdownSchema.text("#".repeat(level) + " ", [markdownSchema.marks.spec.create({
                specClass: "heading-spec"
            })])
        ];

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        const headingNode = markdownSchema.nodes.heading.create(
            {
                level: level,
                specOffset: specOffset
            },
            children
        );

        return headingNode;
    }

    static constructBlockquote(contentNodes) {
        const specOffset = 2;

        const children = [
            markdownSchema.text("> ", [markdownSchema.marks.spec.create({
                specClass: "blockquote-spec"
            })])
        ];

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        const blockquoteNode = markdownSchema.nodes.blockquote.create(
            {
                specOffset: specOffset
            },
            children
        );

        return blockquoteNode;
    }

    static constructTabListItem(contentNodes, tabs) {
        let specOffset = 0;
        const level = tabs.length;

        let index = 0;
        const children = Array(level).fill(null).map((_, i) =>
            markdownSchema.text(tabs[i], [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        tabs.forEach(tab => {
            specOffset += tab.length;
        });

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        return markdownSchema.nodes.tab_list_item.create(
            {
                level: level,
                specOffset: specOffset,
                tabs: tabs
            },
            children
        );
    }

    static constructBulletListItem(contentNodes, tabs, marker = '-') {
        let specOffset = 2;
        const level = tabs.length;

        let index = 0;
        const children = Array(level).fill(null).map((_, i) =>
            markdownSchema.text(tabs[i], [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        tabs.forEach(tab => {
            specOffset += tab.length;
        });

        children.push(markdownSchema.text(marker + " ", [markdownSchema.marks.marker.create()]));

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        return markdownSchema.nodes.bullet_list_item.create(
            {
                level: level,
                marker: marker,
                specOffset: specOffset,
                tabs: tabs
            },
            children
        );
    }

    static constructOrderedListItem(contentNodes, tabs, number = 1) {
        const marker = `${number}. `;
        let specOffset = marker.length;
        const level = tabs.length;

        let index = 0;
        const children = Array(level).fill(null).map((_, i) =>
            markdownSchema.text(tabs[i], [markdownSchema.marks.tab.create({
                tabIndex: index++
            })])
        );

        tabs.forEach(tab => {
            specOffset += tab.length;
        });

        children.push(markdownSchema.text(`${number}. `, [markdownSchema.marks.marker.create()]));

        if (contentNodes && contentNodes.forEach) {
            contentNodes.forEach(node => children.push(node));
        } else if (contentNodes) {
            children.push(contentNodes);
        }

        return markdownSchema.nodes.ordered_list_item.create(
            {
                level: level,
                number: number,
                specOffset: specOffset,
                tabs: tabs
            },
            children
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

        if (specialHandlers[node.attrs.type]) {
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
