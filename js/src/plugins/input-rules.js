import { inputRules, wrappingInputRule, InputRule } from "prosemirror-inputrules";
import { NodeConverter } from "../utils/node-converter.js";
import { markdownSchema } from "../schema/markdown-schema.js";

export function inputRulesPlugin() {
    return inputRules({
        rules: [
            new InputRule(/^(#{1,6})\s$/, (state, match, start, end) => {
                const level = match[1].length;

                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const headingText = fullText.slice(match[1].length);

                const headingNode = markdownSchema.nodes.heading.create(
                    { level: level },
                    headingText ? markdownSchema.text(headingText) : null
                );

                const headingBlock = NodeConverter.constructHeading(headingNode)
                let tr = state.tr.replaceWith(blockStart, blockEnd, headingBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = headingBlock.content.child(0);

                const cursorPos = containerPos + 1 + specNode.nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/^(#)$/, (state, match, start, end) => {
                const level = match[1].length;

                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                let headingText = fullText.slice(match[1].length - 1);

                if (headingText.length === 0 || headingText[0] !== ' ') {
                    return null;
                }

                headingText = headingText.slice(1);

                const headingNode = markdownSchema.nodes.heading.create(
                    { level: level },
                    headingText ? markdownSchema.text(headingText) : null
                );

                const headingBlock = NodeConverter.constructHeading(headingNode)
                let tr = state.tr.replaceWith(blockStart, blockEnd, headingBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = headingBlock.content.child(0);

                const cursorPos = containerPos + specNode.nodeSize - 1;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            wrappingInputRule(
                /^\s*([-+*])\s$/,
                markdownSchema.nodes.bullet_list,
                null,
                () => true
            ),
            wrappingInputRule(
                /^\s*(\d+)\.\s$/,
                markdownSchema.nodes.ordered_list,
                match => ({ order: +match[1] }),
                () => true
            ),
            new InputRule(/\*\*([^\*]+)\*\*$/, (state, match, start, end) => {
                return state.tr.replaceWith(
                    start,
                    end,
                    state.schema.text(match[1], [markdownSchema.marks.strong.create()])
                );
            }),
            new InputRule(/\*([^\*]+)\*$/, (state, match, start, end) => {
                return state.tr.replaceWith(
                    start,
                    end,
                    state.schema.text(match[1], [markdownSchema.marks.em.create()])
                );
            }),
            new InputRule(/^```(\w*)\s$/, (state, match, start, end) => {
                const language = match[1] || "";
                return state.tr.replaceWith(
                    start,
                    end,
                    markdownSchema.nodes.code_block.create(
                        { params: language },
                        language ? [] : [markdownSchema.text("")]
                    )
                );
            })
        ]
    });
}