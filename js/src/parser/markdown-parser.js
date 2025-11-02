import { MarkdownParser } from "prosemirror-markdown";
import MarkdownIt from "markdown-it";
import { markdownSchema } from "../schema/markdown-schema.js";

export class CustomMarkdownParser {
    constructor() {
        const md = new MarkdownIt("commonmark", { html: false })
            .use(function underlinePlugin(md) {
                md.inline.ruler.before('emphasis', 'underline', function (state, silent) {
                    if (state.src.slice(state.pos, state.pos + 2) !== '__') return false;

                    let pos = state.pos + 2;
                    while (pos < state.src.length - 1) {
                        if (state.src.slice(pos, pos + 2) === '__') {

                            if (!silent) {
                                const token = state.push('underline_custom', '', 0);
                                token.content = state.src.slice(state.pos + 2, pos);
                            }
                            state.pos = pos + 2;
                            return true;
                        }
                        pos++;
                    }
                    return false;
                });
            }).use(function highlightPlugin(md) {
                md.inline.ruler.after('link', 'highlight', function (state, silent) {
                    if (state.src.slice(state.pos, state.pos + 2) !== '==') return false;

                    let pos = state.pos + 2;
                    while (pos < state.src.length - 1) {
                        if (state.src.slice(pos, pos + 2) === '==') {
                            if (!silent) {
                                const token = state.push('highlight_custom', '', 0);
                                token.content = state.src.slice(state.pos + 2, pos);
                            }
                            state.pos = pos + 2;
                            return true;
                        }
                        pos++;
                    }
                    return false;
                });
            }).use(function strikePlugin(md) {
                md.inline.ruler.after('link', 'strike', function (state, silent) {
                    if (state.src.slice(state.pos, state.pos + 2) !== '~~') return false;

                    let pos = state.pos + 2;
                    while (pos < state.src.length - 1) {
                        if (state.src.slice(pos, pos + 2) === '~~') {
                            if (!silent) {
                                const token = state.push('strike_custom', '', 0);
                                token.content = state.src.slice(state.pos + 2, pos);
                            }
                            state.pos = pos + 2;
                            return true;
                        }
                        pos++;
                    }
                    return false;
                });
            }).use(function paragraphPlugin(md) {
                md.block.ruler.at('paragraph', function (state, startLine, endLine, silent) {
                    const lineContent = state.getLines(startLine, startLine + 1, state.blkIndent, false).trim();

                    const tokenOpen = state.push('paragraph_open', 'p', 1);
                    tokenOpen.map = [startLine, startLine + 1];

                    const inlineToken = state.push('inline', '', 0);
                    inlineToken.content = lineContent;
                    inlineToken.map = [startLine, startLine + 1];
                    inlineToken.children = [];

                    state.push('paragraph_close', 'p', -1);
                    state.line = startLine + 1;
                    return true;
                });
            })

        const tokens = {
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
            html_block: {
                node: "html_comment",
                getAttrs: tok => ({
                    content: tok.content,
                    isInline: false
                })
            },
            html_inline: {
                node: "html_comment",
                getAttrs: tok => ({
                    content: tok.content,
                    isInline: true
                })
            },
            em: { mark: "em" },
            strong: { mark: "strong" },
            link: {
                mark: "link",
                getAttrs: tok => ({
                    href: tok.attrGet("href"),
                    title: tok.attrGet("title") || null
                })
            },
            code_inline: { mark: "code" },
            strike_custom: {
                mark: "strike",
                noCloseToken: true,
                getAttrs: tok => ({})
            },
            highlight_custom: {
                mark: "highlight",
                noCloseToken: true,
                getAttrs: tok => ({})
            },
            underline_custom: {
                mark: "underline",
                noCloseToken: true,
                getAttrs: tok => ({})
            }
        };

        this.parser = new MarkdownParser(markdownSchema, md, tokens);
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
}
