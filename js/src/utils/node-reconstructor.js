import { markdownSchema } from "../schema/markdown-schema.js";
import { NodeConverter } from "./node-converter.js";

export class NodeReconstructor {
    constructor() {
        this.rules = [
            {
                name: 'heading',
                pattern: /^(#{1,6}) (.*)$/,
                handler: this.reconstructHeading.bind(this)
            },
            {
                name: 'code_block',
                pattern: /^```(\w*)\n([\s\S]*?)\n```$/,
                handler: this.reconstructCodeBlock.bind(this)
            },
            {
                name: 'blockquote',
                pattern: /^> (.*)$/,
                handler: this.reconstructBlockquote.bind(this)
            },
        ];

        this.markRules = [
            {
                name: 'strong',
                pattern: /\*\*(.+?)\*\*/g,
                handler: this.wrapWithMark.bind(this, 'strong')
            },
            {
                name: 'em',
                pattern: /\*(.+?)\*/g,
                handler: this.wrapWithMark.bind(this, 'em')
            },
            {
                name: 'code',
                pattern: /`(.+?)`/g,
                handler: this.wrapWithMark.bind(this, 'code')
            },
            {
                name: 'strike',
                pattern: /~~(.+?)~~/g,
                handler: this.wrapWithMark.bind(this, 'strike')
            },
            {
                name: 'highlight',
                pattern: /==(.+?)==/g,
                handler: this.wrapWithMark.bind(this, 'highlight')
            },
            {
                name: 'underline',
                pattern: /__(.+?)__/g,
                handler: this.wrapWithMark.bind(this, 'underline')
            },
        ];
    }

    reconstructMarksInNode(node) {
        if (node.isTextblock) {
            return this.reconstructMarks(node);
        }

        if (node.content && node.content.size > 0) {
            const newContent = [];
            let hasChanges = false;

            node.content.forEach(child => {
                const reconstructedChild = this.reconstructMarksInNode(child);
                if (reconstructedChild && reconstructedChild !== child) {
                    newContent.push(reconstructedChild);
                    hasChanges = true;
                } else {
                    newContent.push(child);
                }
            });

            if (hasChanges) {
                return node.type.create(node.attrs, newContent, node.marks);
            }
        }

        return null;
    }

    reconstructMarks(node) {
        if (!node.isTextblock) return null;

        const text = node.textContent;
        if (!text) return null;

        let content = [];
        let hasChanges = false;

        let currentText = text;

        while (currentText.length > 0) {
            let bestMatch = null;
            let bestRule = null;

            for (const rule of this.markRules) {
                const pattern = new RegExp(rule.pattern.source);
                const match = currentText.match(pattern);

                if (match && match.index === 0) {
                    bestMatch = match;
                    bestRule = rule;
                    break;
                }
            }

            if (bestMatch) {
                const beforeText = currentText.slice(0, bestMatch.index);
                if (beforeText.length > 0) {
                    content.push(markdownSchema.text(beforeText));
                }

                const markedFragment = bestRule.handler(bestMatch[1]);
                markedFragment.forEach(node => content.push(node));
                hasChanges = true;

                currentText = currentText.slice(bestMatch[0].length);
            } else {
                if (currentText.length > 0) {
                    content.push(markdownSchema.text(currentText[0]));
                    currentText = currentText.slice(1);
                }
            }
        }

        if (hasChanges && content.length > 0) {
            return node.type.create(node.attrs, content, node.marks);
        }

        return null;
    }

    collectParagraphs(startPos, count, state) {
        const paragraphs = [];
        let collected = 0;

        state.doc.descendants((node, nodePos) => {
            if (collected >= count) return false;

            if (node.type.name === 'paragraph' && nodePos >= startPos) {
                paragraphs.push({
                    paragraph: node,
                    pos: nodePos,
                    text: node.textContent
                });
                collected++;
            }
        });

        return paragraphs;
    }

    applyBlockRules(paragraphs, startPos, targetCursorIndex = -1) {
        const results = [];
        let currentPos = startPos;
        let blocksBeforeCursor = 0;

        for (let i = 0; i < paragraphs.length; i++) {
            const paragraph = paragraphs[i];
            const text = paragraph.textContent;
            let reconstructed = null;

            for (const rule of this.rules) {
                const match = text.match(rule.pattern);
                if (match) {
                    reconstructed = rule.handler(match, paragraph, currentPos);
                    break;
                }
            }

            results.push(reconstructed || paragraph);

            if (targetCursorIndex !== -1 && i < targetCursorIndex) {
                const offset = targetCursorIndex - startPos;
                if (reconstructed && reconstructed.type.name === 'notation_block' && reconstructed.attrs.layout === 'row') {
                    if (offset > reconstructed.children[0].textContent.length * 2) {
                        blocksBeforeCursor += 3;
                    } else {
                        blocksBeforeCursor += 1;
                    }
                }
            }

            currentPos += paragraph.nodeSize;
        }

        return {
            paragraphs: results,
            blocksBeforeCursor: blocksBeforeCursor
        };
    }

    wrapWithMark(markName, text) {
        switch (markName) {
            case 'strong':
                return NodeConverter.constructStrong(text);
            case 'em':
                return NodeConverter.constructEm(text);
            case 'code':
                return NodeConverter.constructCode(text);
            case 'strike':
                return NodeConverter.constructStrike(text);
            case 'highlight':
                return NodeConverter.constructHighlight(text);
            case 'underline':
                return NodeConverter.constructUnderline(text);
            default:
                return [markdownSchema.text(text)];
        }
    }

    reconstructHeading(match, originalParagraph, pos) {
        const [_, hashes, content] = match;
        const level = hashes.length;
        return NodeConverter.constructHeading(content, level);
    }

    reconstructBlockquote(match, originalParagraph, pos) {
        const [_, content] = match;

        return NodeConverter.constructBlockquote(content);
    }

    reconstructCodeBlock(match, originalParagraph, pos) {
        const [_, language, content] = match;
        return null;
    }
}