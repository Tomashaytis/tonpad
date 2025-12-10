import { Plugin, PluginKey } from "prosemirror-state";
import { Decoration, DecorationSet } from "prosemirror-view";
import { searchPluginKey } from "./search.js";

export function hideSpecPlugin() {
    const pluginKey = new PluginKey("hideSpecPlugin");

    return new Plugin({
        key: pluginKey,
        state: {
            init(_, { doc }) {
                return getDecorations(doc, null, -1, new Set(), null, null, null, -1);
            },
            apply(tr, oldDecoSet, oldState, newState) {
                const focusedNode = getFocusedNode(newState);
                const bulletMarkPos = getBulletMarkPosition(newState);
                const formatMarkPositions = getFormatMarkPositions(newState);
                const selectionRange = getSelectionRange(newState);
                const linkMarkPositions = getLinkMarkPositions(newState);

                const searchState = searchPluginKey ?
                    searchPluginKey.getState(newState) : null;

                const oldFocusedNode = getFocusedNode(oldState);
                const oldBulletMarkPos = getBulletMarkPosition(oldState);
                const oldFormatMarkPositions = getFormatMarkPositions(oldState);
                const oldSelectionRange = getSelectionRange(oldState);
                const oldLinkMarkPositions = getLinkMarkPositions(oldState);
                const oldSearchState = searchPluginKey ?
                    searchPluginKey.getState(oldState) : null;

                const searchChanged = !searchStatesEqual(searchState, oldSearchState);

                if (tr.docChanged || tr.selectionSet ||
                    focusedNode !== oldFocusedNode ||
                    bulletMarkPos !== oldBulletMarkPos ||
                    !areSetsEqual(formatMarkPositions, oldFormatMarkPositions) ||
                    !areSetsEqual(linkMarkPositions, oldLinkMarkPositions) ||
                    !areRangesEqual(selectionRange, oldSelectionRange) ||
                    searchChanged) {
                    return getDecorations(
                        newState.doc,
                        focusedNode,
                        bulletMarkPos,
                        formatMarkPositions,
                        selectionRange,
                        linkMarkPositions,
                        searchState,
                        newState.selection.$from.pos
                    );
                }
                return oldDecoSet;
            }
        },
        props: {
            decorations(state) {
                return pluginKey.getState(state);
            }
        }
    });
}

function getDecorations(doc, focusedNode, cursorBulletPos, formatMarkPositions, selectionRange, linkMarkPositions, searchState, cursorPos) {
    const decorations = [];

    const currentSearchResult = searchState &&
        searchState.isActive &&
        searchState.getCurrentResult ?
        searchState.getCurrentResult() : null;

    const allFormatMarks = [];
    const allLinkMarks = [];

    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            for (const mark of node.marks) {
                if (mark.type.name === "spec" && mark.attrs.type === "format") {
                    allFormatMarks.push({
                        mark,
                        node,
                        pos,
                        endPos: pos + node.nodeSize,
                        formatType: mark.attrs.formatType,
                        text: node.text
                    });
                }
                if (mark.type.name === "link") {
                    allLinkMarks.push({
                        mark,
                        node,
                        pos,
                        endPos: pos + node.nodeSize,
                        href: mark.attrs.href,
                        hidden: mark.attrs.hidden
                    });
                }
            }
        }

        if (node.type.name === "horizontal_rule") {
            const $pos = doc.resolve(pos);
            let isInFocusedNode = false;
            let isInSelection = false;
            let isInSearchResult = false;

            if (focusedNode && focusedNode === node) {
                isInFocusedNode = true;
            }

            if (selectionRange) {
                const nodeEnd = pos + node.nodeSize;
                if (pos < selectionRange.to && nodeEnd > selectionRange.from) {
                    isInSelection = true;
                }
            }

            if (currentSearchResult) {
                const nodeEnd = pos + node.nodeSize;
                if (pos < currentSearchResult.to && nodeEnd > currentSearchResult.from) {
                    isInSearchResult = true;
                }
            }

            if (!isInFocusedNode && !isInSelection && !isInSearchResult) {
                decorations.push(
                    Decoration.node(pos, pos + node.nodeSize, {
                        class: "horizontal-rule-hidden"
                    })
                );
            }
        }
        return true;
    });

    const markerPairs = findAllMarkerPairs(allFormatMarks);
    const linkConstructions = findLinkConstructions(allFormatMarks, allLinkMarks);

    // Находим конструкцию, в которой находится курсор (если есть)
    const cursorInLinkConstruction = linkConstructions.find(construction =>
        cursorPos >= construction.startPos && cursorPos <= construction.endPos
    );

    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            // Проверяем, находится ли этот узел в link конструкции с курсором
            let isInLinkConstructionWithCursor = false;
            if (cursorInLinkConstruction) {
                if (pos >= cursorInLinkConstruction.startPos && pos < cursorInLinkConstruction.endPos) {
                    isInLinkConstructionWithCursor = true;
                }
            }

            for (const mark of node.marks) {
                if (mark.type.name === "spec") {
                    if (mark.attrs.type !== "format") {
                        // Обработка heading и blockquote
                        if (mark.attrs.type === 'heading' || mark.attrs.type === 'blockquote') {
                            const $pos = doc.resolve(pos);
                            let isInFocusedNode = false;
                            let isInSelection = false;
                            let isInSearchResult = false;

                            if (focusedNode) {
                                for (let i = 0; i <= $pos.depth; i++) {
                                    if ($pos.node(i) === focusedNode) {
                                        isInFocusedNode = true;
                                        break;
                                    }
                                }
                            }

                            if (selectionRange) {
                                const nodeEnd = pos + node.nodeSize;
                                if (pos < selectionRange.to && nodeEnd > selectionRange.from) {
                                    isInSelection = true;
                                }
                            }

                            if (currentSearchResult) {
                                const nodeEnd = pos + node.nodeSize;
                                if (pos < currentSearchResult.to && nodeEnd > currentSearchResult.from) {
                                    isInSearchResult = true;
                                }
                            }

                            // Не скрываем если в link конструкции с курсором
                            if (!isInFocusedNode && !isInSelection && !isInSearchResult && !isInLinkConstructionWithCursor) {
                                const cssClass = mark.attrs.type === 'heading'
                                    ? 'heading-hidden'
                                    : 'blockquote-hidden';
                                decorations.push(
                                    Decoration.inline(pos, pos + node.nodeSize, {
                                        class: cssClass
                                    })
                                );
                            }
                        }
                    } else {
                        // Обработка format spec маркеров
                        let shouldHide = true;

                        // Не скрываем если в link конструкции с курсором
                        if (isInLinkConstructionWithCursor) {
                            shouldHide = false;
                        }

                        if (shouldHide && formatMarkPositions.has(pos)) {
                            shouldHide = false;
                        }

                        if (shouldHide) {
                            for (const pair of markerPairs) {
                                if (pair.leftPos === pos || pair.rightPos === pos) {
                                    if (cursorPos >= pair.leftPos && cursorPos <= pair.rightPos) {
                                        shouldHide = false;
                                        break;
                                    }
                                }
                            }
                        }

                        if (shouldHide && selectionRange) {
                            const nodeEnd = pos + node.nodeSize;
                            if (pos < selectionRange.to && nodeEnd > selectionRange.from) {
                                shouldHide = false;
                            }
                        }

                        if (shouldHide && currentSearchResult) {
                            const nodeEnd = pos + node.nodeSize;
                            if (pos < currentSearchResult.to && nodeEnd > currentSearchResult.from) {
                                shouldHide = false;
                            }
                        }

                        if (shouldHide) {
                            decorations.push(
                                Decoration.inline(pos, pos + node.nodeSize, {
                                    class: 'format-hidden'
                                })
                            );
                        }
                    }
                    break;
                } else if (mark.type.name === "marker" && mark.attrs.type === "bullet") {
                    // Обработка bullet маркеров
                    let isInSelection = false;
                    let isInSearchResult = false;

                    if (selectionRange) {
                        const nodeEnd = pos + node.nodeSize;
                        if (pos < selectionRange.to && nodeEnd > selectionRange.from) {
                            isInSelection = true;
                        }
                    }

                    if (currentSearchResult) {
                        const nodeEnd = pos + node.nodeSize;
                        if (pos < currentSearchResult.to && nodeEnd > currentSearchResult.from) {
                            isInSearchResult = true;
                        }
                    }

                    // Не скрываем если в link конструкции с курсором
                    if (pos !== cursorBulletPos && !isInSelection && !isInSearchResult && !isInLinkConstructionWithCursor) {
                        decorations.push(
                            Decoration.inline(pos, pos + node.nodeSize, {
                                class: 'bullet-list-hidden'
                            })
                        );
                    }
                    break;
                } else if (mark.type.name === "link") {
                    // Обработка link маркеров
                    let isInSelection = false;
                    let isInSearchResult = false;
                    let isCursorInside = linkMarkPositions.has(pos);

                    if (selectionRange) {
                        const nodeEnd = pos + node.nodeSize;
                        if (pos < selectionRange.to && nodeEnd > selectionRange.from) {
                            isInSelection = true;
                        }
                    }

                    if (currentSearchResult) {
                        const nodeEnd = pos + node.nodeSize;
                        if (pos < currentSearchResult.to && nodeEnd > currentSearchResult.from) {
                            isInSearchResult = true;
                        }
                    }

                    // Для скрытых link маркеров
                    if (mark.attrs.hidden) {
                        // Не скрываем если: курсор внутри, в выделении, в результатах поиска или в link конструкции с курсором
                        if (!isCursorInside && !isInSelection && !isInSearchResult && !isInLinkConstructionWithCursor) {
                            decorations.push(
                                Decoration.inline(pos, pos + node.nodeSize, {
                                    class: 'link-hidden'
                                })
                            );
                        }
                    }
                    break;
                }
            }
        }
        return true;
    });

    return DecorationSet.create(doc, decorations);
}

function findLinkConstructions(allFormatMarks, allLinkMarks) {
    const constructions = [];

    const linkSpecs = allFormatMarks.filter(m =>
        m.mark.attrs.formatType === 'link' ||
        m.mark.attrs.formatType === 'hidden-link' ||
        m.mark.attrs.formatType === 'note_link' ||
        m.mark.attrs.formatType === 'embedded_link'
    );

    const sortedSpecs = [...linkSpecs].sort((a, b) => a.pos - b.pos);

    let i = 0;
    while (i < sortedSpecs.length) {
        const current = sortedSpecs[i];

        if (current.mark.attrs.formatType === 'link') {
            if (i + 3 < sortedSpecs.length) {
                const next1 = sortedSpecs[i + 1];
                const next2 = sortedSpecs[i + 2];
                const next3 = sortedSpecs[i + 3];
                
                if (next1.mark.attrs.formatType === 'link' && 
                    next2.mark.attrs.formatType === 'hidden-link' && 
                    next3.mark.attrs.formatType === 'hidden-link') {
                    
                    const linksInRange = allLinkMarks.filter(l =>
                        l.pos >= current.pos && l.pos <= next3.endPos
                    );

                    constructions.push({
                        type: 'external',
                        startPos: current.pos,
                        endPos: next3.endPos,
                        links: linksInRange,
                        specMarkers: [current, next1, next2, next3]
                    });

                    i += 4;
                    continue;
                }
            }
            
            if (i + 2 < sortedSpecs.length) {
                const next1 = sortedSpecs[i + 1];
                const next2 = sortedSpecs[i + 2];
                
                if (next1.mark.attrs.formatType === 'link' && 
                    next2.mark.attrs.formatType === 'hidden-link') {
                    
                    const linksInRange = allLinkMarks.filter(l =>
                        l.pos >= current.pos && l.pos <= next2.endPos
                    );

                    constructions.push({
                        type: 'external',
                        startPos: current.pos,
                        endPos: next2.endPos,
                        links: linksInRange,
                        specMarkers: [current, next1, next2]
                    });

                    i += 3;
                    continue;
                }
            }
            
            if (i + 1 < sortedSpecs.length) {
                const next1 = sortedSpecs[i + 1];
                
                if (next1.mark.attrs.formatType === 'hidden-link') {
                    const linksInRange = allLinkMarks.filter(l =>
                        l.pos >= current.pos && l.pos <= next1.endPos
                    );

                    constructions.push({
                        type: 'external',
                        startPos: current.pos,
                        endPos: next1.endPos,
                        links: linksInRange,
                        specMarkers: [current, next1]
                    });

                    i += 2;
                    continue;
                }
            }
            
            const linksInRange = allLinkMarks.filter(l =>
                l.pos >= current.pos && l.pos <= current.endPos
            );
            
            if (linksInRange.length > 0 || current.text?.includes('[')) {
                constructions.push({
                    type: 'external',
                    startPos: current.pos,
                    endPos: current.endPos,
                    links: linksInRange,
                    specMarkers: [current]
                });
            }
        }
        else if (current.mark.attrs.formatType === 'note_link') {
            if (i + 1 < sortedSpecs.length && 
                sortedSpecs[i + 1].mark.attrs.formatType === 'note_link') {
                
                const closing = sortedSpecs[i + 1];
                const linksInRange = allLinkMarks.filter(l =>
                    l.pos >= current.pos && l.pos <= closing.endPos
                );

                constructions.push({
                    type: 'note',
                    startPos: current.pos,
                    endPos: closing.endPos,
                    links: linksInRange,
                    specMarkers: [current, closing]
                });

                i += 2;
                continue;
            }
        }
        else if (current.mark.attrs.formatType === 'embedded_link') {
            if (i + 1 < sortedSpecs.length && 
                sortedSpecs[i + 1].mark.attrs.formatType === 'embedded_link') {
                
                const closing = sortedSpecs[i + 1];
                const linksInRange = allLinkMarks.filter(l =>
                    l.pos >= current.pos && l.pos <= closing.endPos
                );

                constructions.push({
                    type: 'embedded',
                    startPos: current.pos,
                    endPos: closing.endPos,
                    links: linksInRange,
                    specMarkers: [current, closing]
                });

                i += 2;
                continue;
            }
        }

        i++;
    }

    return constructions;
}

function getLinkMarkPositions(state) {
    const { $from, $to } = state.selection;
    const positions = new Set();

    if ($from.pos !== $to.pos) return positions;

    const cursorPos = $from.pos;
    const doc = state.doc;

    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            if (pos <= cursorPos && cursorPos <= pos + node.nodeSize) {
                for (const mark of node.marks) {
                    if (mark.type.name === "link" && mark.attrs.hidden === true) {
                        positions.add(pos);
                        break;
                    }
                }
            }
        }
        return true;
    });

    return positions;
}

function searchStatesEqual(state1, state2) {
    if (!state1 && !state2) return true;
    if (!state1 || !state2) return false;

    return state1.isActive === state2.isActive &&
        state1.currentIndex === state2.currentIndex &&
        state1.query === state2.query &&
        state1.total === state2.total;
}

function findAllMarkerPairs(allFormatMarks) {
    const pairs = [];
    const stack = [];

    const sortedMarks = [...allFormatMarks].sort((a, b) => a.pos - b.pos);

    for (const mark of sortedMarks) {
        if (stack.length === 0) {
            stack.push(mark);
        } else {
            const last = stack[stack.length - 1];

            if (last.formatType === mark.formatType) {
                pairs.push({
                    formatType: mark.formatType,
                    leftPos: last.pos,
                    rightPos: mark.pos,
                    leftText: last.text,
                    rightText: mark.text,
                    depth: stack.length
                });
                stack.pop();
            } else {
                stack.push(mark);
            }
        }
    }

    return pairs;
}

function getFormatMarkPositions(state) {
    const { $from, $to } = state.selection;
    const positions = new Set();

    if ($from.pos !== $to.pos) return positions;

    const cursorPos = $from.pos;
    const doc = state.doc;

    const allFormatMarks = [];
    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            for (const mark of node.marks) {
                if (mark.type.name === "spec" && mark.attrs.type === "format") {
                    allFormatMarks.push({
                        mark,
                        node,
                        pos,
                        endPos: pos + node.nodeSize,
                        formatType: mark.attrs.formatType
                    });
                }
            }
        }
        return true;
    });

    const markerPairs = findAllMarkerPairs(allFormatMarks);

    let cursorMarkInfo = null;
    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            if (pos <= cursorPos && cursorPos <= pos + node.nodeSize) {
                for (const mark of node.marks) {
                    if (mark.type.name === "spec" && mark.attrs.type === "format") {
                        cursorMarkInfo = { mark, pos, formatType: mark.attrs.formatType };
                        positions.add(pos);
                        break;
                    }
                }
                if (cursorMarkInfo) return false;
            }
        }
        return true;
    });

    if (cursorMarkInfo) {
        for (const pair of markerPairs) {
            if (pair.leftPos === cursorMarkInfo.pos) {
                positions.add(pair.rightPos);
            } else if (pair.rightPos === cursorMarkInfo.pos) {
                positions.add(pair.leftPos);
            }
        }
    }

    return positions;
}

function areSetsEqual(set1, set2) {
    if (set1.size !== set2.size) return false;
    for (const item of set1) {
        if (!set2.has(item)) return false;
    }
    return true;
}

function getFocusedNode(state) {
    const { $from } = state.selection;

    for (let i = $from.depth; i >= 0; i--) {
        const node = $from.node(i);
        if (node.type.name !== "doc" && !node.isText) {
            return node;
        }
    }

    return null;
}

function getBulletMarkPosition(state) {
    const { $from, $to } = state.selection;

    if ($from.pos !== $to.pos) return -1;

    const doc = state.doc;
    let markerPos = -1;

    doc.descendants((node, pos) => {
        if (node.isText && node.marks) {
            if (pos <= $from.pos && $from.pos <= pos + node.nodeSize) {
                for (const mark of node.marks) {
                    if (mark.type.name === "marker" && mark.attrs.type === "bullet") {
                        markerPos = pos;
                        return false;
                    }
                }
            }
        }
        return true;
    });

    return markerPos;
}

function getSelectionRange(state) {
    const { from, to } = state.selection;

    if (from === to) return null;

    return { from, to };
}

function areRangesEqual(range1, range2) {
    if (!range1 && !range2) return true;
    if (!range1 || !range2) return false;
    return range1.from === range2.from && range1.to === range2.to;
}