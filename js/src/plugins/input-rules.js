import { inputRules, wrappingInputRule, InputRule } from "prosemirror-inputrules";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeReconstructor } from "../utils/node-reconstructor.js";
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

                const headingBlock = NodeConverter.constructHeading(headingText, level);

                const reconstructor = new NodeReconstructor();
                const headingWithMarks = reconstructor.reconstructMarksInNode(headingBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, headingWithMarks || headingBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (headingWithMarks || headingBlock).content.child(0);

                const cursorPos = containerPos + 1 + specNode.nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/^#$/, (state, match, start, end) => {
                const level = 1;

                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                let headingText = fullText;

                if (headingText.length === 0 || headingText[0] !== ' ') {
                    return null;
                }

                headingText = headingText.slice(1);

                const headingBlock = NodeConverter.constructHeading(headingText, level);

                const reconstructor = new NodeReconstructor();
                const headingWithMarks = reconstructor.reconstructMarksInNode(headingBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, headingWithMarks || headingBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (headingWithMarks || headingBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize - 1;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/^>\s$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const blockquoteText = fullText.slice(2);

                const blockquoteBlock = NodeConverter.constructBlockquote(blockquoteText);

                const reconstructor = new NodeReconstructor();
                const blockquoteWithMarks = reconstructor.reconstructMarksInNode(blockquoteBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, blockquoteWithMarks || blockquoteBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (blockquoteWithMarks || blockquoteBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize + 1;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/^>$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                let blockquoteText = fullText;

                if (blockquoteText.length === 0 || blockquoteText[0] !== ' ') {
                    return null;
                }

                blockquoteText = blockquoteText.slice(1);

                const blockquoteBlock = NodeConverter.constructBlockquote(blockquoteText);

                const reconstructor = new NodeReconstructor();
                const blockquoteWithMarks = reconstructor.reconstructMarksInNode(blockquoteBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, blockquoteWithMarks || blockquoteBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (blockquoteWithMarks || blockquoteBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize - 1;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            /*new InputRule(/\*\*([^\*]+)\*\*$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const strongBlock = NodeConverter.constructStrong(textNode);

                let tr = state.tr.replaceWith(start, end, strongBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + strongBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/\*([^\*]+)\*$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const emBlock = NodeConverter.constructEm(textNode);

                let tr = state.tr.replaceWith(start, end, emBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + emBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/~~([^~]+)~~$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const strikeBlock = NodeConverter.constructStrike(textNode);

                let tr = state.tr.replaceWith(start, end, strikeBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + strikeBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/==([^=]+)==$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const highlightBlock = NodeConverter.constructHighlight(textNode);

                let tr = state.tr.replaceWith(start, end, highlightBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + highlightBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/__([^_]+)__$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const underlineBlock = NodeConverter.constructUnderline(textNode);

                let tr = state.tr.replaceWith(start, end, underlineBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + underlineBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),
            new InputRule(/`([^`]+)`$/, (state, match, start, end) => {
                const textContent = match[1];
                const textNode = markdownSchema.text(textContent);
                const codeBlock = NodeConverter.constructCode(textNode);

                let tr = state.tr.replaceWith(start, end, codeBlock);
                const containerPos = tr.mapping.map(start);
                const cursorPos = containerPos + codeBlock.content.child(0).nodeSize;
                return tr.setSelection(state.selection.constructor.near(tr.doc.resolve(cursorPos)));
            }),*/
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