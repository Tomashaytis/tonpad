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
            content: "block+",
            group: "block",
            parseDOM: [{ tag: "blockquote" }],
            toDOM() { return ["blockquote", 0]; },
        },
        horizontal_rule: {
            group: "block",
            parseDOM: [{ tag: "hr" }],
            toDOM() { return ["div", ["hr"]]; },
        },
        heading: {
            attrs: { level: { default: 1 } },
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
                return ["h" + node.attrs.level, { class: "heading" }, 0];
            },
        },
        heading_spec: {
            attrs: { level: { default: 1 } },
            content: "text*",
            group: "block",
            parseDOM: [{ tag: "heading-spec" }],
            toDOM(node) {
                return [
                    "h" + node.attrs.level,
                    {
                        class: "heading-spec",
                        'data-level': node.attrs.level
                    },
                    0
                ];
            },
        },
        code_block: {
            content: "text*",
            group: "block",
            code: true,
            defining: true,
            marks: "",
            attrs: { params: { default: "" } },
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
        ordered_list: {
            content: "list_item+",
            group: "block",
            attrs: { order: { default: 1 }, tight: { default: false } },
            parseDOM: [
                {
                    tag: "ol",
                    getAttrs: (dom) => ({
                        order: dom.hasAttribute("start") ? +dom.getAttribute("start") : 1,
                        tight: dom.hasAttribute("data-tight"),
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "ol",
                    {
                        start: node.attrs.order == 1 ? null : node.attrs.order,
                        "data-tight": node.attrs.tight ? "true" : null,
                    },
                    0,
                ];
            },
        },
        bullet_list: {
            content: "list_item+",
            group: "block",
            attrs: { tight: { default: false } },
            parseDOM: [
                { tag: "ul", getAttrs: (dom) => ({ tight: dom.hasAttribute("data-tight") }) },
            ],
            toDOM(node) {
                return ["ul", { "data-tight": node.attrs.tight ? "true" : null }, 0];
            },
        },
        list_item: {
            content: "block+",
            defining: true,
            parseDOM: [{ tag: "li" }],
            toDOM() { return ["li", 0]; },
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
        hard_break: {
            inline: true,
            group: "inline",
            selectable: false,
            parseDOM: [{ tag: "br" }],
            toDOM() { return ["br"]; },
        },
        customTag: {
            content: "block+",
            group: "block",
            attrs: {
                name: { default: "" },
                type: { default: "" },
                params: { default: {} },
                sourceText: { default: "" },
            },
            parseDOM: [
                {
                    tag: "div[data-custom-tag]",
                    getAttrs: (dom) => ({
                        name: dom.getAttribute("data-tag-name") || "",
                        type: dom.getAttribute("data-tag-type") || "",
                        params: JSON.parse(dom.getAttribute("data-tag-params") || "{}"),
                        sourceText: dom.getAttribute("data-source-text") || "",
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "div",
                    {
                        "data-custom-tag": "",
                        "data-tag-name": node.attrs.name,
                        "data-tag-type": node.attrs.type,
                        "data-tag-params": JSON.stringify(node.attrs.params),
                        "data-source-text": node.attrs.sourceText,
                        class: `custom-tag custom-tag-${node.attrs.name}`,
                    },
                    0,
                ];
            },
        },
        notation_block: {
            attrs: { type: { default: "none" } },
            content: "block+",
            group: "block",
            toDOM: () => ["div", { class: "notation-block" }, 0]
        }
    },
    marks: {
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
        link: {
            attrs: {
                href: {},
                title: { default: null },
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
            toDOM(node) { return ["a", node.attrs]; },
        },
        code: {
            code: true,
            parseDOM: [{ tag: "code" }],
            toDOM() { return ["code"]; },
        },
    },
});

/*import { Schema } from "prosemirror-model";

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
            content: "block+",
            group: "block",
            parseDOM: [{ tag: "blockquote" }],
            toDOM() { return ["blockquote", 0]; },
        },
        horizontal_rule: {
            group: "block",
            parseDOM: [{ tag: "hr" }],
            toDOM() { return ["div", ["hr"]]; },
        },
        heading: {
            attrs: { level: { default: 1 } },
            content: "(text | image)*",
            group: "block",
            defining: true,
            parseDOM: [
                { tag: "h1", attrs: { level: 1 } },
                { tag: "h2", attrs: { level: 2 } },
                { tag: "h3", attrs: { level: 3 } },
                { tag: "h4", attrs: { level: 4 } },
                { tag: "h5", attrs: { level: 5 } },
                { tag: "h6", attrs: { level: 6 } },
            ],
            toDOM(node) { return ["h" + node.attrs.level, 0]; },
        },
        // В схеме:
        heading_focus: {
            attrs: { level: { default: 1 } },
            content: "(text | image)*",
            group: "block",
            defining: true,
            toDOM(node) {
                return [
                    "h" + node.attrs.level,
                    {
                        class: `heading-node heading-focus heading-level-${node.attrs.level}`,
                        "data-level": node.attrs.level
                    },
                    0
                ];
            },
        },
        code_block: {
            content: "text*",
            group: "block",
            code: true,
            defining: true,
            marks: "",
            attrs: { params: { default: "" } },
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
        ordered_list: {
            content: "list_item+",
            group: "block",
            attrs: { order: { default: 1 }, tight: { default: false } },
            parseDOM: [
                {
                    tag: "ol",
                    getAttrs: (dom) => ({
                        order: dom.hasAttribute("start") ? +dom.getAttribute("start") : 1,
                        tight: dom.hasAttribute("data-tight"),
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "ol",
                    {
                        start: node.attrs.order == 1 ? null : node.attrs.order,
                        "data-tight": node.attrs.tight ? "true" : null,
                    },
                    0,
                ];
            },
        },
        bullet_list: {
            content: "list_item+",
            group: "block",
            attrs: { tight: { default: false } },
            parseDOM: [
                { tag: "ul", getAttrs: (dom) => ({ tight: dom.hasAttribute("data-tight") }) },
            ],
            toDOM(node) {
                return ["ul", { "data-tight": node.attrs.tight ? "true" : null }, 0];
            },
        },
        list_item: {
            content: "block+",
            defining: true,
            parseDOM: [{ tag: "li" }],
            toDOM() { return ["li", 0]; },
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
        hard_break: {
            inline: true,
            group: "inline",
            selectable: false,
            parseDOM: [{ tag: "br" }],
            toDOM() { return ["br"]; },
        },
        customTag: {
            content: "block+",
            group: "block",
            attrs: {
                name: { default: "" },
                type: { default: "" },
                params: { default: {} },
                sourceText: { default: "" }, // Для хранения исходного Markdown
            },
            parseDOM: [
                {
                    tag: "div[data-custom-tag]",
                    getAttrs: (dom) => ({
                        name: dom.getAttribute("data-tag-name") || "",
                        type: dom.getAttribute("data-tag-type") || "",
                        params: JSON.parse(dom.getAttribute("data-tag-params") || "{}"),
                        sourceText: dom.getAttribute("data-source-text") || "",
                    }),
                },
            ],
            toDOM(node) {
                return [
                    "div",
                    {
                        "data-custom-tag": "",
                        "data-tag-name": node.attrs.name,
                        "data-tag-type": node.attrs.type,
                        "data-tag-params": JSON.stringify(node.attrs.params),
                        "data-source-text": node.attrs.sourceText,
                        class: `custom-tag custom-tag-${node.attrs.name}`,
                    },
                    0,
                ];
            },
        },
    },
    marks: {
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
        link: {
            attrs: {
                href: {},
                title: { default: null },
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
            toDOM(node) { return ["a", node.attrs]; },
        },
        code: {
            code: true,
            parseDOM: [{ tag: "code" }],
            toDOM() { return ["code"]; },
        },
    },
});*/