import { Schema } from "prosemirror-model";

export const markdownSchema = new Schema({
    nodes: {
        doc: {
            content: "block+",
        },
        paragraph: {
            content: "inline*",
            group: "block",
            parseDOM: [{ tag: "p" }],
            toDOM() { return ["p", 0]; },
        },
        blockquote: {
            attrs: {
                renderAs: { default: "blockquote" },
                specOffset: { defult: 0 }
            },
            content: "text*",
            group: "block",
            defining: true,
            parseDOM: [{ tag: "blockquote" }],
            toDOM(node) {
                const tag = node.attrs.renderAs === "span" ? "span" : "blockquote";
                const className = node.attrs.renderAs === "span" ? "blockquote-content" : "blockquote";

                return [tag, { class: className }, 0];
            },
        },
        horizontal_rule: {
            group: "block",
            parseDOM: [{ tag: "hr" }],
            toDOM() { return ["div", ["hr"]]; },
        },
        heading: {
            attrs: {
                level: { default: 1 },
                renderAs: { default: "h" },
                specOffset: { defult: 0 }
            },
            content: "(text | image)*",
            group: "block",
            defining: true,
            parseDOM: [
                { tag: "h1", attrs: { level: 1 } },
                { tag: "h2", attrs: { level: 2 } },
                { tag: "h3", attrs: { level: 3 } },
                { tag: "h4", attrs: { level: 4 } },
                { tag: "h5", attrs: { level: 5 } },
                { tag: "h6", attrs: { level: 6 } }
            ],
            toDOM(node) {
                const tag = node.attrs.renderAs === "span" ? "span" : "h" + node.attrs.level;
                const className = node.attrs.renderAs === "span" ? "heading-content" : "heading";

                return [tag, { class: className }, 0];
            },
        },
        code_block: {
            content: "text*",
            group: "block",
            code: true,
            defining: true,
            marks: "",
            attrs: {
                params: { default: "" }
            },
            parseDOM: [
                {
                    tag: "pre",
                    preserveWhitespace: "full",
                    getAttrs: (dom) => ({
                        params: dom.getAttribute("data-params") || "",
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "pre",
                    node.attrs.params ? { "data-params": node.attrs.params } : {},
                    ["code", 0],
                ];
            },
        },
        tab_list_item: {
            attrs: {
                level: { default: 1 },
                renderAs: { default: "li" },
                specOffset: { defult: 0 },
                tabs: { default: [] }
            },
            content: "(text | image)*",
            group: "block",
            defining: true,
            parseDOM: [{ tag: "li" }],
            toDOM(node) { 
                const tag = node.attrs.renderAs === "span" ? "span" : "li";
                const className = node.attrs.renderAs === "span" ? "li-content" : "li";

                return [tag, { class: className }, 0];
            },
        },
        bullet_list_item: {
            attrs: {
                level: { default: 1 },
                marker: { default: ' ' },
                renderAs: { default: "li" },
                specOffset: { defult: 0 },
                tabs: { default: [] }
            },
            content: "(text | image)*",
            group: "block",
            defining: true,
            parseDOM: [{ tag: "li" }],
            toDOM(node) { 
                const tag = node.attrs.renderAs === "span" ? "span" : "li";
                const className = node.attrs.renderAs === "span" ? "li-content" : "li";

                return [tag, { class: className }, 0];
            },
        },
        ordered_list_item: {
            attrs: {
                level: { default: 1 },
                number: { default: 0 },
                renderAs: { default: "li" },
                specOffset: { defult: 0 },
                tabs: { default: [] }
            },
            content: "(text | image)*",
            group: "block",
            defining: true,
            parseDOM: [{ tag: "li" }],
            toDOM(node) { 
                const tag = node.attrs.renderAs === "span" ? "span" : "li";
                const className = node.attrs.renderAs === "span" ? "li-content" : "li";

                return [tag, { class: className }, 0];
            },
        },
        text: {
            group: "inline",
        },
        image: {
            inline: true,
            attrs: {
                src: {},
                alt: { default: null },
                title: { default: null },
            },
            group: "inline",
            draggable: true,
            parseDOM: [
                {
                    tag: "img[src]",
                    getAttrs: (dom) => ({
                        src: dom.getAttribute("src"),
                        title: dom.getAttribute("title"),
                        alt: dom.getAttribute("alt"),
                    }),
                },
            ],
            toDOM(node) { return ["img", node.attrs]; },
        },
        html_comment: {
            group: "block",
            content: "text*",
            marks: "",
            parseDOM: [{ tag: "div.html-comment" }],
            toDOM(node) {
                return ["div", {
                    class: node.attrs.isInline ? "html-comment-inline" : "html-comment-block",
                    "data-inline": node.attrs.isInline
                }, `<!-- ${node.attrs.content} -->`];
            },
            attrs: {
                content: { default: "" },
                isInline: { default: false }
            }
        },
    },
    marks: {
        spec: {
            attrs: {
                specClass: { default: "mark-spec" },
            },
            parseDOM: [
                {
                    tag: "span.mark-spec",
                    getAttrs: (dom) => ({
                        specClass: dom.getAttribute("class"),
                    })
                }
            ],
            toDOM(node) {
                return [
                    "span",
                    {
                        class: node.attrs.specClass,
                    }
                ];
            },
        },
        tab: {
            attrs: {
                tabClass: { default: "tab" },
                tabIndex: { default: 0 }
            },
            parseDOM: [
                {
                    tag: "span.tab",
                    getAttrs: (dom) => ({
                        tabClass: dom.getAttribute("class"),
                    })
                }
            ],
            toDOM(node) {
                return [
                    "span",
                    {
                        class: node.attrs.tabClass,
                    }
                ];
            },
        },
        marker: {
            attrs: {
                specClass: { default: "marker" },
            },
            parseDOM: [
                {
                    tag: "span.marker",
                    getAttrs: (dom) => ({
                        specClass: dom.getAttribute("class"),
                    })
                }
            ],
            toDOM(node) {
                return [
                    "span",
                    {
                        class: node.attrs.specClass,
                    }
                ];
            },
        },
        em: {
            parseDOM: [
                { tag: "i" },
                { tag: "em" },
                { style: "font-style=italic" },
                { style: "font-style=normal", clearMark: (m) => m.type.name === "em" },
            ],
            toDOM() { return ["em"]; },
        },
        strong: {
            parseDOM: [
                { tag: "strong" },
                { tag: "b", getAttrs: (node) => node.style.fontWeight !== "normal" && null },
                { style: "font-weight=400", clearMark: (m) => m.type.name === "strong" },
                {
                    style: "font-weight",
                    getAttrs: (value) => /^(bold(er)?|[5-9]\d{2,})$/.test(value) && null,
                },
            ],
            toDOM() { return ["strong"]; },
        },
        strike: {
            parseDOM: [
                { tag: "s" },
                { tag: "strike" },
                { tag: "del" },
                { style: "text-decoration=line-through" },
            ],
            toDOM() { return ["s"]; },
        },
        highlight: {
            parseDOM: [
                { tag: "mark" },
                { style: "background-color", getAttrs: value => value && null },
            ],
            toDOM() { return ["mark"]; },
        },
        underline: {
            parseDOM: [
                { tag: "u" },
                { style: "text-decoration=underline" },
            ],
            toDOM() { return ["u"]; },
        },
        link: {
            attrs: {
                href: { default: "#" },
                title: { default: null },
                linkClass: { default: "" },
            },
            inclusive: false,
            parseDOM: [
                {
                    tag: "a[href]",
                    getAttrs: (dom) => ({
                        href: dom.getAttribute("href"),
                        title: dom.getAttribute("title"),
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "a",
                    {
                        href: node.attrs.href,
                        title: node.attrs.title,
                        class: node.attrs.linkClass,
                    }
                ];
            },
        },
        code: {
            code: true,
            parseDOM: [{ tag: "code" }],
            toDOM() { return ["code"]; },
        },
    }
});

