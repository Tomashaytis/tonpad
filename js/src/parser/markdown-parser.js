import { MarkdownParser } from "prosemirror-markdown";
import MarkdownIt from "markdown-it";
import { markdownSchema } from "../schema/markdown-schema.js";

export class CustomMarkdownParser {
    constructor() {
        const md = new MarkdownIt();

        md.block.ruler.before("html_block", "custom_tag", (state, startLine, endLine, silent) => {
            const startPos = state.bMarks[startLine] + state.tShift[startLine];
            const maxPos = state.eMarks[startLine];
            const lineText = state.src.slice(startPos, maxPos);

            const openTagMatch = lineText.match(/^<!--\s*(\w+)(?:\s+([^>]*))?\s*-->$/);
            if (!openTagMatch) return false;

            const tagName = openTagMatch[1];
            const attrString = openTagMatch[2] || "";
            const attrs = CustomMarkdownParser.parseTagAttributes(attrString);

            if (silent) return true;

            let nextLine = startLine + 1;
            let contentStart = state.bMarks[nextLine];
            let foundEndTag = false;
            let contentEnd;

            for (; nextLine < endLine; nextLine++) {
                const nextLineText = state.src.slice(state.bMarks[nextLine], state.eMarks[nextLine]);
                if (nextLineText.match(new RegExp(`^<!--\\s*/${tagName}\\s*-->$`))) {
                    foundEndTag = true;
                    contentEnd = state.bMarks[nextLine];
                    break;
                }
            }

            if (!foundEndTag) return false;

            const content = state.src.slice(contentStart, contentEnd);
            const token = state.push("custom_tag", "", 0);
            token.block = true;
            token.attrs = [
                ["data-tag-name", tagName],
                ["data-tag-params", JSON.stringify(attrs)],
                ["data-source-text", `<!-- ${tagName} ${attrString} -->${content}<!-- /${tagName} -->`],
            ];
            token.content = content;
            token.map = [startLine, nextLine + 1];
            state.line = nextLine + 1;

            return true;
        });

        this.parser = new MarkdownParser(markdownSchema, md, {
            custom_tag: {
                block: "customTag",
                getAttrs: tok => ({
                    name: tok.attrGet('data-tag-name') || '',
                    type: tok.attrGet('data-tag-type') || '',
                    params: JSON.parse(tok.attrGet('data-tag-params') || '{}')
                })
            },
            blockquote: { block: "blockquote" },
            paragraph: { block: "paragraph" },
            list_item: { block: "list_item" },
            bullet_list: { block: "bullet_list" },
            ordered_list: { block: "ordered_list" },
            heading: { block: "heading", getAttrs: tok => ({ level: +tok.tag.slice(1) }) },
            code_block: { block: "code_block" },
            fence: { block: "code_block", getAttrs: tok => ({ params: tok.info || "" }) },
            hr: { node: "horizontal_rule" },
            image: {
                node: "image",
                getAttrs: tok => ({
                    src: tok.attrGet("src"),
                    title: tok.attrGet("title") || null,
                    alt: tok.children[0]?.content || null
                })
            },
            hard_break: { node: "hard_break" },
            em: { mark: "em" },
            strong: { mark: "strong" },
            link: {
                mark: "link",
                getAttrs: tok => ({
                    href: tok.attrGet("href"),
                    title: tok.attrGet("title") || null
                })
            },
            code_inline: { mark: "code" }
        });
    }

    parse(text) {
        try {
            const doc = this.parser.parse(text || "");
            return doc && doc.content ? doc : markdownSchema.nodes.doc.create(null, [markdownSchema.nodes.paragraph.create()]);
        } catch (error) {
            console.error("Parse error:", error);
            return markdownSchema.nodes.doc.create(null, [markdownSchema.nodes.paragraph.create()]);
        }
    }

    static parseTagAttributes(attrString) {
        const attrs = {};
        const regex = /(\w+)=["']([^"']*)["']/g;
        let match;
        while ((match = regex.exec(attrString)) !== null) {
            attrs[match[1]] = match[2];
        }
        return attrs;
    }
}

/*import { MarkdownParser } from "prosemirror-markdown";
import MarkdownIt from "markdown-it";
import { markdownSchema } from "../schema/markdown-schema.js";

export class CustomMarkdownParser {
    constructor() {
        const md = new MarkdownIt();

        md.block.ruler.before("html_block", "custom_tag", (state, startLine, endLine, silent) => {
            const startPos = state.bMarks[startLine] + state.tShift[startLine];
            const maxPos = state.eMarks[startLine];
            const lineText = state.src.slice(startPos, maxPos);

            const openTagMatch = lineText.match(/^<!--\s*(\w+)(?:\s+([^>]*))?\s*-->$/);
            if (!openTagMatch) return false;

            const tagName = openTagMatch[1];
            const attrString = openTagMatch[2] || "";
            const attrs = CustomMarkdownParser.parseTagAttributes(attrString);

            if (silent) return true;

            let nextLine = startLine + 1;
            let contentStart = state.bMarks[nextLine];
            let foundEndTag = false;
            let contentEnd;

            for (; nextLine < endLine; nextLine++) {
                const nextLineText = state.src.slice(state.bMarks[nextLine], state.eMarks[nextLine]);
                if (nextLineText.match(new RegExp(`^<!--\\s* /${tagName}\\s*-->$`))) {
                    foundEndTag = true;
                    contentEnd = state.bMarks[nextLine];
                    break;
                }
            }

            if (!foundEndTag) return false;

            const content = state.src.slice(contentStart, contentEnd);
            const token = state.push("custom_tag", "", 0);
            token.block = true;
            token.tag = "div";
            token.attrs = [
                ["data-custom-tag", ""],
                ["data-tag-name", tagName],
                ["data-tag-type", ""],
                ["data-tag-params", JSON.stringify(attrs)],
                ["data-source-text", `<!-- ${tagName} ${attrString} -->${content}<!-- /${tagName} -->`],
            ];
            token.content = content;
            token.map = [startLine, nextLine + 1];
            state.line = nextLine + 1;

            return true;
        });

        this.parser = new MarkdownParser(markdownSchema, md, {
            custom_tag: {
                block: "customTag",
                getAttrs: (tok) => ({
                    name: tok.attrGet("data-tag-name") || "",
                    type: tok.attrGet("data-tag-type") || "",
                    params: JSON.parse(tok.attrGet("data-tag-params") || "{}"),
                    sourceText: tok.attrGet("data-source-text") || "",
                }),
            },
            blockquote: { block: "blockquote" },
            paragraph: { block: "paragraph" },
            list_item: { block: "list_item" },
            bullet_list: { block: "bullet_list" },
            ordered_list: { block: "ordered_list" },
            heading: { 
                block: "heading", 
                getAttrs: (tok) => ({ level: +tok.tag.slice(1) }),
                getContent: (tok, schema) => {
                    const level = +tok.tag.slice(1);
                    const prefixText = '#'.repeat(level) + ' ';
                    const contentText = tok.content || "";
                    
                    return [
                        schema.nodes.heading_spec.create(null, schema.text(prefixText)),
                        schema.nodes.heading_content.create(null, schema.text(contentText))
                    ];
                }
            },
            fence: {
                block: "code_block",
                getAttrs: (tok) => ({ params: tok.info || "" }),
                getContent: (tok, schema) => schema.text(tok.content || ""),
            },
            code_block: {
                block: "code_block",
                getAttrs: (tok) => ({ params: tok.info || "" }),
                getContent: (tok, schema) => schema.text(tok.content || ""),
            },
            hr: { node: "horizontal_rule" },
            image: {
                node: "image",
                getAttrs: (tok) => ({
                    src: tok.attrGet("src"),
                    title: tok.attrGet("title") || null,
                    alt: tok.children[0]?.content || null,
                }),
            },
            hard_break: { node: "hard_break" },
            em: { mark: "em" },
            strong: { mark: "strong" },
            link: {
                mark: "link",
                getAttrs: (tok) => ({
                    href: tok.attrGet("href"),
                    title: tok.attrGet("title") || null,
                }),
            },
            code_inline: { mark: "code" },
        });
    }

    parse(text) {
        try {
            const doc = this.parser.parse(text || "");
            if (doc && doc.content) {
                return doc;
            }
            return markdownSchema.nodes.doc.create(null, [
                markdownSchema.nodes.paragraph.create(null, markdownSchema.text("")),
            ]);
        } catch (error) {
            console.error("Parse error:", error);
            return markdownSchema.nodes.doc.create(null, [
                markdownSchema.nodes.paragraph.create(null, markdownSchema.text("")),
            ]);
        }
    }

    static parseTagAttributes(attrString) {
        const attrs = {};
        const regex = /(\w+)=["']([^"']*)["']/g;
        let match;
        while ((match = regex.exec(attrString)) !== null) {
            attrs[match[1]] = match[2];
        }
        return attrs;
    }
}*/

/*import { MarkdownParser } from "prosemirror-markdown";
import MarkdownIt from "markdown-it";
import { markdownSchema } from "../schema/markdown-schema.js";

export class CustomMarkdownParser {
    constructor() {
        const md = new MarkdownIt();
        
        // Добавляем правило для парсинга кастомных тегов
        md.block.ruler.before('html_block', 'custom_tag', (state, startLine, endLine, silent) => {
            const startPos = state.bMarks[startLine] + state.tShift[startLine];
            const maxPos = state.eMarks[startLine];
            const lineText = state.src.slice(startPos, maxPos);

            // Проверяем открывающий тег вида <!-- tagname param1="value1" -->
            const openTagMatch = lineText.match(/^<!--\s*(\w+)(?:\s+([^>]*))?\s*-->$/);
            if (!openTagMatch) return false;

            const tagName = openTagMatch[1];
            const attrString = openTagMatch[2] || '';
            const attrs = CustomMarkdownParser.parseTagAttributes(attrString);

            if (silent) return true;

            // Ищем закрывающий тег <!-- /tagname -->
            let nextLine = startLine + 1;
            let contentStart = state.bMarks[nextLine];
            let foundEndTag = false;
            let contentEnd;

            for (; nextLine < endLine; nextLine++) {
                const nextLineText = state.src.slice(state.bMarks[nextLine], state.eMarks[nextLine]);
                if (nextLineText.match(new RegExp(`^<!--\\s* /${tagName}\\s*-->$`))) {
                    foundEndTag = true;
                    contentEnd = state.bMarks[nextLine];
                    break;
                }
            }

            if (!foundEndTag) return false;

            // Собираем содержимое между тегами
            const content = state.src.slice(contentStart, contentEnd);
            const token = state.push('custom_tag', '', 0);
            token.block = true;
            token.tag = 'div';
            token.attrs = [['data-custom-tag', ''], ['data-tag-name', tagName], ['data-tag-type', ''], ['data-tag-params', JSON.stringify(attrs)]];
            token.content = content;
            token.map = [startLine, nextLine + 1];
            state.line = nextLine + 1;

            return true;
        });

        this.standardParser = new MarkdownParser(
            markdownSchema,
            md,
            {
                custom_tag: {
                    block: "customTag",
                    getAttrs: tok => ({
                        name: tok.attrGet('data-tag-name') || '',
                        type: tok.attrGet('data-tag-type') || '',
                        params: JSON.parse(tok.attrGet('data-tag-params') || '{}')
                    })
                },
                blockquote: { block: "blockquote" },
                paragraph: { block: "paragraph" },
                list_item: { block: "list_item" },
                bullet_list: { block: "bullet_list" },
                ordered_list: { block: "ordered_list" },
                heading: { block: "heading", getAttrs: tok => ({ level: +tok.tag.slice(1) }) },
                code_block: { block: "code_block" },
                fence: { block: "code_block", getAttrs: tok => ({ params: tok.info || "" }) },
                hr: { node: "horizontal_rule" },
                image: {
                    node: "image",
                    getAttrs: tok => ({
                        src: tok.attrGet("src"),
                        title: tok.attrGet("title") || null,
                        alt: tok.children[0]?.content || null
                    })
                },
                hard_break: { node: "hard_break" },
                em: { mark: "em" },
                strong: { mark: "strong" },
                link: {
                    mark: "link",
                    getAttrs: tok => ({
                        href: tok.attrGet("href"),
                        title: tok.attrGet("title") || null
                    })
                },
                code_inline: { mark: "code" }
            }
        );
    }

    parse(text) {
        console.log('Parsing input text:', JSON.stringify(text));
        try {
            const doc = this.standardParser.parse(text || "");
            console.log('Parsed nodes:', Array.from(doc.content).map(node => node.toJSON()));
            if (doc && doc.content) {
                return doc;
            }
            // Если документ невалидный, возвращаем входной текст как paragraph
            console.warn('Parsed document is invalid or has no content:', doc);
            return markdownSchema.nodes.doc.create(null, [
                markdownSchema.nodes.paragraph.create(
                    null,
                    markdownSchema.text(text || "")
                )
            ]);
        } catch (error) {
            console.error('Parse error:', error);
            // В случае ошибки парсинга возвращаем входной текст как paragraph
            return markdownSchema.nodes.doc.create(null, [
                markdownSchema.nodes.paragraph.create(
                    null,
                    markdownSchema.text(text || "")
                )
            ]);
        }
    }

    static parseTagAttributes(attrString) {
        const attrs = {};
        const regex = /(\w+)=["']([^"']*)["']/g;
        let match;

        while ((match = regex.exec(attrString)) !== null) {
            attrs[match[1]] = match[2];
        }

        return attrs;
    }
}*/