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
                specClass: "heading-spec",
                type: "heading"
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
                specClass: "blockquote-spec",
                type: "blockquote"
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

    static constructEm(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["*", "*"], content, markdownSchema.marks.em, 'em-mark', 'em-mark');
        }
        return Fragment.from(this.createWrappedMark(["*", "*"], content, markdownSchema.marks.em, 'em-mark', 'em-mark'));
    }

    static constructItalic(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["_", "_"], content, markdownSchema.marks.italic, 'em-mark', 'em-mark');
        }
        return Fragment.from(this.createWrappedMark(["_", "_"], content, markdownSchema.marks.italic, 'em-mark', 'em-mark'));
    }

    static constructStrong(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["**", "**"], content, markdownSchema.marks.strong, 'strong-mark', 'strong-mark');
        }
        return Fragment.from(this.createWrappedMark(["**", "**"], content, markdownSchema.marks.strong, 'strong-mark', 'strong-mark'));
    }

    static constructStrike(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["~~", "~~"], content, markdownSchema.marks.strike, 'strike-mark', 'strike-mark');
        }
        return Fragment.from(this.createWrappedMark(["~~", "~~"], content, markdownSchema.marks.strike, 'strike-mark', 'strike-mark'));
    }

    static constructHighlight(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["==", "=="], content, markdownSchema.marks.highlight, 'highlight-mark', 'highlight-mark');
        }
        return Fragment.from(this.createWrappedMark(["==", "=="], content, markdownSchema.marks.highlight, 'highlight-mark', 'highlight-mark'));
    }

    static constructUnderline(content) {
        if (content instanceof Fragment) {
            return this.createWrappedMarkFragment(["__", "__"], content, markdownSchema.marks.underline, 'underline-mark', 'underline-mark');
        }
        return Fragment.from(this.createWrappedMark(["__", "__"], content, markdownSchema.marks.underline, 'underline-mark', 'underline-mark'));
    }

    static constructCode(text) {
        return Fragment.from(this.createWrappedMark(["`", "`"], text, markdownSchema.marks.code, 'code-mark-spec-left', 'code-mark-spec-right'));
    }

    static constructComment(text) {
        return Fragment.from(this.createWrappedMark(["%%", "%%"], text, markdownSchema.marks.comment, 'comment', 'comment'));
    }

    static constructMath(text) {
        const processedText = this.replaceMathSymbols(text);

        const components = this.parseMathComponents(processedText);

        return Fragment.from(this.createWrappedMark(
            ["$", "$"],
            components,
            markdownSchema.marks.math,
            'math-delimiter',
            'math-delimiter'
        ));
    }

    static replaceMathSymbols(text) {
        let processed = text.replace(/-/g, '−');
        return processed;
    }

    static parseMathComponents(text) {
        const nodes = [];
        let currentIndex = 0;

        const numberRegex = /^-?\d*\.?\d+(?:[eE][-+]?\d+)?/;
        const wordRegex = /^[a-zA-Zα-ωΑ-Ω]+/;
        const bracketRegex = /^[{}()\[\]]/;
        const operandRegex = /^[_^\.]/;

        while (currentIndex < text.length) {
            let match;

            if (text[currentIndex] === ' ') {
                nodes.push(markdownSchema.text(' '));
                currentIndex++;
                continue;
            }

            match = text.slice(currentIndex).match(bracketRegex);
            if (match) {
                nodes.push(markdownSchema.text(match[0], [
                    markdownSchema.marks.math_bracket.create()
                ]));
                currentIndex += match[0].length;
                continue;
            }

            match = text.slice(currentIndex).match(numberRegex);
            if (match) {
                nodes.push(markdownSchema.text(match[0], [
                    markdownSchema.marks.math_number.create()
                ]));
                currentIndex += match[0].length;
                continue;
            }

            match = text.slice(currentIndex).match(wordRegex);
            if (match) {
                nodes.push(markdownSchema.text(match[0], [
                    markdownSchema.marks.math_word.create()
                ]));
                currentIndex += match[0].length;
                continue;
            }

            match = text.slice(currentIndex).match(operandRegex);
            if (match) {
                nodes.push(markdownSchema.text(match[0], [
                    markdownSchema.marks.math_operand.create()
                ]));
                currentIndex += match[0].length;
                continue;
            }

            nodes.push(markdownSchema.text(text[currentIndex], [
                markdownSchema.marks.math.create()
            ]));
            currentIndex++;
        }

        return nodes;
    }

    static createWrappedMarkFragment(delimiters, content, mark, leftMarkClass = 'mark-spec', rightMarkClass = 'mark-spec') {
        const nodes = [
            markdownSchema.text(delimiters[0], [markdownSchema.marks.spec.create({
                specClass: leftMarkClass
            })])
        ];

        if (content instanceof Fragment) {
            content.forEach(node => {
                if (node.isText) {
                    const newMarks = [...node.marks, mark.create()];
                    nodes.push(markdownSchema.text(node.text, newMarks));
                } else {
                    nodes.push(node);
                }
            });
        } else if (typeof content === 'string' && content !== "") {
            nodes.push(markdownSchema.text(content, [mark.create()]));
        }

        nodes.push(markdownSchema.text(delimiters[1], [markdownSchema.marks.spec.create({
            specClass: rightMarkClass
        })]));

        return Fragment.from(nodes);
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

    static constructNoteLink(text) {        
        const nodes = [
            markdownSchema.text("[[", [markdownSchema.marks.spec.create()])
        ];

        if (text && text !== '') {
            const separatorIndex = text.indexOf('|');

            if (separatorIndex !== -1) {
                const beforeSeparator = text.substring(0, separatorIndex);
                const afterSeparator = text.substring(separatorIndex + 1);
                const href = beforeSeparator;

                if (beforeSeparator.length > 0) {
                    nodes.push(markdownSchema.text(beforeSeparator, [markdownSchema.marks.link.create({
                        href: 'tonpad://' + href
                    })]));
                }

                nodes.push(markdownSchema.text("|", [markdownSchema.marks.spec.create()]));

                if (afterSeparator.length > 0) {
                    nodes.push(markdownSchema.text(afterSeparator, [markdownSchema.marks.link.create({
                        href: 'tonpad://' + href
                    })]));
                }
            } else {
                nodes.push(markdownSchema.text(text, [markdownSchema.marks.link.create({
                    href: 'tonpad://' + text
                })]));
            }
        }

        nodes.push(markdownSchema.text("]]", [markdownSchema.marks.spec.create()]));

        return Fragment.from(nodes);
    }

    static constructEmbeddedLink(text) {        
        const nodes = [
            markdownSchema.text("![[", [markdownSchema.marks.spec.create()])
        ];

        if (text && text !== '') {
            const separatorIndex = text.indexOf('|');

            if (separatorIndex !== -1) {
                const beforeSeparator = text.substring(0, separatorIndex);
                const afterSeparator = text.substring(separatorIndex + 1);
                const href = beforeSeparator;

                if (beforeSeparator.length > 0) {
                    nodes.push(markdownSchema.text(beforeSeparator, [markdownSchema.marks.link.create({
                        href: 'tonpad://' + href
                    })]));
                }

                nodes.push(markdownSchema.text("|", [markdownSchema.marks.spec.create()]));

                if (afterSeparator.length > 0) {
                    nodes.push(markdownSchema.text(afterSeparator, [markdownSchema.marks.link.create({
                        href: 'tonpad://' + href
                    })]));
                }
            } else {
                nodes.push(markdownSchema.text(text, [markdownSchema.marks.link.create({
                    href: 'tonpad://' + text
                })]));
            }
        }

        nodes.push(markdownSchema.text("]]", [markdownSchema.marks.spec.create()]));

        return Fragment.from(nodes);
    }

    static constructEmptyLink(text) {
        const nodes = [
            markdownSchema.text("[", [markdownSchema.marks.spec.create()])
        ];

        if (text && text !== "") {
            nodes.push(markdownSchema.text(text, [markdownSchema.marks.link.create()]));
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

        if (Array.isArray(text)) {
            nodes.push(...text);
        } else if (text && text !== "") {
            nodes.push(markdownSchema.text(text, [mark.create()]));
        }

        nodes.push(markdownSchema.text(delimiters[1], [markdownSchema.marks.spec.create({
            specClass: rightMarkClass
        })]));
        return nodes;
    }
}
