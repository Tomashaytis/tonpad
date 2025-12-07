import { markdownSchema } from "../schema/markdown-schema.js";
import { NodeConverter } from "./node-converter.js";
import { findTabs } from "../utils/utils.js";
import { Fragment } from "prosemirror-model";

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
            {
                name: 'bullet_list',
                pattern: /^((?:(?:    )|\t)*)([+-]) (.*)$/,
                handler: this.reconstructBulletListItem.bind(this)
            },
            {
                name: 'ordered_list',
                pattern: /^((?:(?:    )|\t)*)(\d+)\. (.*)$/,
                handler: this.reconstructOrderedListItem.bind(this)
            },
            {
                name: 'tab_list',
                pattern: /^((?:(?:    )|\t)+)(.*)$/,
                handler: this.reconstructTabListItem.bind(this)
            },
            {
                name: 'paragraph',
                pattern: /^(.*)$/,
                handler: this.reconstructParagraph.bind(this)
            },
        ];

        this.markRules = [
            {
                name: 'strong',
                pattern: /\*\*(.*?)\*\*/g,
                handler: this.wrapWithMark.bind(this, 'strong')
            },
            {
                name: 'em',
                pattern: /(?<!\*)\*(.*?)\*(?!\*)/g,
                handler: this.wrapWithMark.bind(this, 'em')
            },
            {
                name: 'strike',
                pattern: /~~(.*?)~~/g,
                handler: this.wrapWithMark.bind(this, 'strike')
            },
            {
                name: 'highlight',
                pattern: /==(.*?)==/g,
                handler: this.wrapWithMark.bind(this, 'highlight')
            },
            {
                name: 'underline',
                pattern: /__(.*?)__/g,
                handler: this.wrapWithMark.bind(this, 'underline')
            },
            {
                name: 'italic',
                pattern: /(?<!_)_(.*?)_(?!_)/g,
                handler: this.wrapWithMark.bind(this, 'italic')
            },
            {
                name: 'code',
                pattern: /`(.*?)`/g,
                handler: this.wrapWithMark.bind(this, 'code')
            },
            {
                name: 'comment',
                pattern: /%%(.*?)%%/g,
                handler: this.wrapWithMark.bind(this, 'comment')
            },
            {
                name: 'math',
                pattern: /\$(.*?)\$/g,
                handler: this.wrapWithMark.bind(this, 'math')
            },
            {
                name: 'embedded_link',
                pattern: /!\[\[(.*?)\]\]/g,
                handler: this.wrapWithMark.bind(this, 'embedded_link')
            },
            {
                name: 'note_link',
                pattern: /\[\[(.*?)\]\]/g,
                handler: this.wrapWithMark.bind(this, 'note_link')
            },
            {
                name: 'empty_link',
                pattern: /\[(.*?)\](?!\()/g,
                handler: this.wrapWithMark.bind(this, 'empty_link')
            },
            {
                name: 'link',
                pattern: /\[(.*?)\]\((.*?)\)/g,
                handler: this.wrapWithMark.bind(this, 'link')
            },
            {
                name: 'url',
                pattern: /((?:https?:\/\/|ftp:\/\/|www\.)[^\s<>"{}`|\\^\[\]]+)/g,
                handler: this.wrapWithMark.bind(this, 'url')
            },
            {
                name: 'email',
                pattern: /(\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b)/g,
                handler: this.wrapWithMark.bind(this, 'email')
            },
            {
                name: 'tag',
                pattern: /(#\w+)/g,
                handler: this.wrapWithMark.bind(this, 'tag')
            },
        ];

        this.nestingMarks = new Set(['strong', 'em', 'italic', 'strike', 'highlight', 'underline']);
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

                const href = bestMatch.length > 2 ? bestMatch[2] : ""
                const markedFragment = bestRule.handler(bestMatch[1], href);
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

    applyBlockRules(paragraphs, startPos) {
        const results = [];
        let currentPos = startPos;

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

            currentPos += paragraph.nodeSize;
        }

        return results
    }

    reconstructTextContent(text) {
        if (!text) return null;

        const textNode = markdownSchema.text(text);
        const tempParagraph = markdownSchema.nodes.paragraph.create({}, [textNode]);
        const reconstructed = this.reconstructMarks(tempParagraph);
        if (reconstructed) {
            return reconstructed.content;
        }

        return [textNode];
    }

    wrapWithMark(markName, text, href = "") {
        if (this.nestingMarks.has(markName)) {
            const processedContent = this.processNestedMarks(text);
            
            switch (markName) {
                case 'strong':
                    return NodeConverter.constructStrong(processedContent);
                case 'em':
                    return NodeConverter.constructEm(processedContent);
                case 'italic':
                    return NodeConverter.constructItalic(processedContent);
                case 'strike':
                    return NodeConverter.constructStrike(processedContent);
                case 'highlight':
                    return NodeConverter.constructHighlight(processedContent);
                case 'underline':
                    return NodeConverter.constructUnderline(processedContent);
                default:
                    return [markdownSchema.text(text)];
            }
        } else {
            switch (markName) {
                case 'code':
                    return NodeConverter.constructCode(text);
                case 'comment':
                    return NodeConverter.constructComment(text);
                case 'math':
                    return NodeConverter.constructMath(text);
                case 'link':
                    return NodeConverter.constructLink(text, href);
                case 'empty_link':
                    return NodeConverter.constructEmptyLink(text);
                case 'note_link':
                    return NodeConverter.constructNoteLink(text);
                case 'embedded_link':
                    return NodeConverter.constructEmbeddedLink(text);
                case 'url':
                    return NodeConverter.constructUrl(text);
                case 'email':
                    return NodeConverter.constructEmail(text);
                case 'tag':
                    return NodeConverter.constructTag(text);
                default:
                    return [markdownSchema.text(text)];
            }
        }
    }

    processNestedMarks(text) {
        if (!text) return Fragment.from([]);

        const textNode = markdownSchema.text(text);
        const tempParagraph = markdownSchema.nodes.paragraph.create({}, [textNode]);
        
        const reconstructed = this.reconstructMarks(tempParagraph);
        
        if (reconstructed) {
            return Fragment.from(reconstructed.content);
        }
        
        return Fragment.from([textNode]);
    }

    reconstructParagraph(match, originalParagraph, pos) {
        const [_, content] = match;
        const textNodes = this.reconstructTextContent(content)

        return NodeConverter.constructParagraphWithMarks(textNodes);
    }

    reconstructHeading(match, originalParagraph, pos) {
        const [_, hashes, content] = match;
        const level = hashes.length;

        const textNodes = this.reconstructTextContent(content)

        return NodeConverter.constructHeading(textNodes, level);
    }

    reconstructBlockquote(match, originalParagraph, pos) {
        const [_, content] = match;

        const textNodes = this.reconstructTextContent(content);

        return NodeConverter.constructBlockquote(textNodes);
    }

    reconstructTabListItem(match, originalParagraph, pos) {
        const [_, spaces, content] = match;

        const tabs = findTabs(spaces);

        const textNodes = this.reconstructTextContent(content);

        return NodeConverter.constructTabListItem(textNodes, tabs);
    }

    reconstructBulletListItem(match, originalParagraph, pos) {
        const [_, spaces, marker, content] = match;

        const tabs = findTabs(spaces);

        const textNodes = this.reconstructTextContent(content);

        return NodeConverter.constructBulletListItem(textNodes, tabs, marker);
    }

    reconstructOrderedListItem(match, originalParagraph, pos) {
        const [_, spaces, number, content] = match;

        const tabs = findTabs(spaces);

        const textNodes = this.reconstructTextContent(content);

        return NodeConverter.constructOrderedListItem(textNodes, tabs, parseInt(number));
    }

    reconstructCodeBlock(match, originalParagraph, pos) {
        const [_, language, content] = match;
        return null;
    }
}