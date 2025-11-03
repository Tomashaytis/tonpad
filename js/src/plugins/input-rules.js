import { inputRules, wrappingInputRule, InputRule } from "prosemirror-inputrules";
import { NodeConverter } from "../utils/node-converter.js";
import { NodeReconstructor } from "../utils/node-reconstructor.js";
import { markdownSchema } from "../schema/markdown-schema.js";
import { findTabLevel } from "../utils/utils.js";
import { TextSelection } from "prosemirror-state";

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
                const nodeText = fullText.slice(match[1].length);

                const nodeBlock = NodeConverter.constructHeading(nodeText, level);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^#$/, (state, match, start, end) => {
                const level = 1;

                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                let nodeText = fullText;

                if (nodeText.length === 0 || nodeText[0] !== ' ') {
                    return null;
                }

                nodeText = nodeText.slice(1);

                const nodeBlock = NodeConverter.constructHeading(nodeText, level);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize - 1;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^>\s$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText.slice(2);

                const nodeBlock = NodeConverter.constructBlockquote(nodeText);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^>$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                let nodeText = fullText;

                if (nodeText.length === 0 || nodeText[0] !== ' ') {
                    return null;
                }

                nodeText = nodeText.slice(1);

                const nodeBlock = NodeConverter.constructBlockquote(nodeText);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^((?:(?:    )|\t)+)$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const parent = $start.parent;
                const level = findTabLevel(match[1]);

                if (parent.type.name === 'spec_block') {
                    return null;
                }

                const notationParent = $start.node($start.depth - 1);
                if (notationParent && notationParent.type.name === 'notation_block' &&
                    notationParent.attrs.type === 'tab_list' &&
                    parent.type.name === 'tab_list_item') {

                    const currentLevel = notationParent.attrs.level || 1;
                    const newLevel = currentLevel + level;

                    const nodeText = parent.textContent.slice(match[1].length - 1);
                    const updatedBlock = NodeConverter.constructTabListItem(nodeText, newLevel);

                    const reconstructor = new NodeReconstructor();
                    const nodeWithMarks = reconstructor.reconstructMarksInNode(updatedBlock);

                    const blockStart = $start.before(1);
                    const blockEnd = $start.after(1);

                    let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || updatedBlock);

                    const containerPos = tr.mapping.map(blockStart);
                    const specNode = (nodeWithMarks || updatedBlock).content.child(0);
                    const cursorPos = containerPos + specNode.nodeSize;

                    let selection = TextSelection.create(tr.doc, cursorPos);

                    return tr.setSelection(selection);
                }

                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText.slice(match[1].length);

                const nodeBlock = NodeConverter.constructTabListItem(nodeText, level);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^((?:(?:    )|\t)*)([+-]) $/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const parent = $start.parent;
                if (!match[1]) {
                    match[1] = "";
                }

                const level = findTabLevel(match[1]);

                if (parent.type.name === 'spec_block') {
                    return null;
                }

                const notationParent = $start.node($start.depth - 1);
                if (notationParent && notationParent.type.name === 'notation_block' &&
                    notationParent.attrs.type === 'tab_list' &&
                    parent.type.name === 'tab_list_item') {

                    const currentLevel = notationParent.attrs.level || 1;
                    const newLevel = currentLevel + level;

                    const nodeText = parent.textContent.slice(match[1].length + 1);
                    const updatedBlock = NodeConverter.constructBulletListItem(nodeText, newLevel, match[2]);

                    const reconstructor = new NodeReconstructor();
                    const nodeWithMarks = reconstructor.reconstructMarksInNode(updatedBlock);

                    const blockStart = $start.before(1);
                    const blockEnd = $start.after(1);

                    let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || updatedBlock);

                    const containerPos = tr.mapping.map(blockStart);
                    const specNode = (nodeWithMarks || updatedBlock).content.child(0);
                    const cursorPos = containerPos + specNode.nodeSize;

                    let selection = TextSelection.create(tr.doc, cursorPos);

                    return tr.setSelection(selection);
                }

                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText.slice(match[1].length + 1);

                const nodeBlock = NodeConverter.constructBulletListItem(nodeText, level, match[2]);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^((?:(?:    )|\t)*)([+-])$/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const parent = $start.parent;
                if (!match[1]) {
                    match[1] = "";
                }

                const level = findTabLevel(match[1]);

                if (parent.type.name === 'spec_block') {
                    return null;
                }

                const notationParent = $start.node($start.depth - 1);
                if (notationParent && notationParent.type.name === 'notation_block' &&
                    notationParent.attrs.type === 'tab_list' &&
                    parent.type.name === 'tab_list_item') {

                    const currentLevel = notationParent.attrs.level || 1;
                    const newLevel = currentLevel + level;

                    const nodeText = parent.textContent.slice(match[1].length + 1);

                    if (nodeText.length === 0 || nodeText[0] !== ' ') {
                        return null;
                    }

                    const updatedBlock = NodeConverter.constructBulletListItem(nodeText, newLevel, match[2]);

                    const reconstructor = new NodeReconstructor();
                    const nodeWithMarks = reconstructor.reconstructMarksInNode(updatedBlock);

                    const blockStart = $start.before(1);
                    const blockEnd = $start.after(1);

                    let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || updatedBlock);

                    const containerPos = tr.mapping.map(blockStart);
                    const specNode = (nodeWithMarks || updatedBlock).content.child(0);
                    const cursorPos = containerPos + specNode.nodeSize;

                    let selection = TextSelection.create(tr.doc, cursorPos);

                    return tr.setSelection(selection);
                }

                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText.slice(match[1].length + 1);

                if (nodeText.length === 0 || nodeText[0] !== ' ') {
                    return null;
                }

                const nodeBlock = NodeConverter.constructBulletListItem(nodeText, level, match[2]);

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
            }),
            new InputRule(/^((?:(?:    )|\t)*)(\d+)\. $/, (state, match, start, end) => {
                const $start = state.doc.resolve(start);
                const parent = $start.parent;
                if (!match[1]) {
                    match[1] = "";
                }

                const level = findTabLevel(match[1]);

                if (parent.type.name === 'spec_block') {
                    return null;
                }

                const notationParent = $start.node($start.depth - 1);
                if (notationParent && notationParent.type.name === 'notation_block' &&
                    notationParent.attrs.type === 'tab_list' &&
                    parent.type.name === 'tab_list_item') {

                    const currentLevel = notationParent.attrs.level || 1;
                    const newLevel = currentLevel + level;

                    const nodeText = parent.textContent.slice(match[1].length + match[2].length + 1);
                    const updatedBlock = NodeConverter.constructOrderedListItem(nodeText, newLevel, parseInt(match[2]));

                    const reconstructor = new NodeReconstructor();
                    const nodeWithMarks = reconstructor.reconstructMarksInNode(updatedBlock);

                    const blockStart = $start.before(1);
                    const blockEnd = $start.after(1);

                    let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || updatedBlock);

                    const containerPos = tr.mapping.map(blockStart);
                    const specNode = (nodeWithMarks || updatedBlock).content.child(0);
                    const cursorPos = containerPos + specNode.nodeSize;

                    let selection = TextSelection.create(tr.doc, cursorPos);

                    return tr.setSelection(selection);
                }

                const blockStart = $start.before(1);
                const blockEnd = $start.after(1);
                const currentBlock = $start.node(1);

                const fullText = currentBlock.textContent;
                const nodeText = fullText.slice(match[1].length + match[2].length + 1);

                const nodeBlock = NodeConverter.constructOrderedListItem(nodeText, level, parseInt(match[2]));

                const reconstructor = new NodeReconstructor();
                const nodeWithMarks = reconstructor.reconstructMarksInNode(nodeBlock);

                let tr = state.tr.replaceWith(blockStart, blockEnd, nodeWithMarks || nodeBlock);

                const containerPos = tr.mapping.map(blockStart);
                const specNode = (nodeWithMarks || nodeBlock).content.child(0);

                const cursorPos = containerPos + specNode.nodeSize;
                let selection = TextSelection.create(tr.doc, cursorPos);

                return tr.setSelection(selection);
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
            /*(
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
            ),*/
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