import { Plugin, PluginKey } from "prosemirror-state";
import { Decoration, DecorationSet } from "prosemirror-view";

export const searchPluginKey = new PluginKey('search');

class SearchState {
    constructor(decorations = DecorationSet.empty, query = null, results = [], currentIndex = -1) {
        this.decorations = decorations;
        this.query = query;
        this.results = results;
        this.currentIndex = currentIndex;
        this.isActive = !!query;
        this.total = results.length;
    }

    static init() {
        return new SearchState();
    }

    updateResults(results, query, doc) {
        const decorations = this.createDecorations(results, 0, doc);
        return new SearchState(
            decorations,
            query,
            results,
            results.length > 0 ? 0 : -1
        );
    }

    clear() {
        return SearchState.init();
    }

    next(doc) {
        if (this.results.length === 0) return this;
        const nextIndex = (this.currentIndex + 1) % this.results.length;
        const decorations = this.createDecorations(this.results, nextIndex, doc);
        return new SearchState(
            decorations,
            this.query,
            this.results,
            nextIndex
        );
    }

    previous(doc) {
        if (this.results.length === 0) return this;
        const prevIndex = this.currentIndex > 0
            ? this.currentIndex - 1
            : this.results.length - 1;
        const decorations = this.createDecorations(this.results, prevIndex, doc);
        return new SearchState(
            decorations,
            this.query,
            this.results,
            prevIndex
        );
    }

    createDecorations(results, currentIndex, doc) {
        const decorations = results.map((result, index) => {
            const isCurrent = index === currentIndex;
            return Decoration.inline(result.from, result.to, {
                class: isCurrent ? 'search-match current-match' : 'search-match'
            });
        });

        return DecorationSet.create(doc, decorations);
    }

    getCurrentResult() {
        return this.results[this.currentIndex];
    }

    getInfo() {
        return {
            isActive: this.isActive,
            query: this.query,
            current: this.currentIndex + 1,
            total: this.total,
            hasResults: this.total > 0
        };
    }
}

export function searchPlugin() {
    return new Plugin({
        key: searchPluginKey,
        state: {
            init: SearchState.init,
            apply(tr, prev) {
                const action = tr.getMeta(searchPluginKey);

                if (tr.selectionSet && prev.isActive) {
                    return prev.clear();
                }

                if (action?.type === 'FIND') {
                    return prev.updateResults(action.results, action.query, tr.doc);
                }

                if (action?.type === 'CLEAR') {
                    return prev.clear();
                }

                if (action?.type === 'NEXT') {
                    return prev.next(tr.doc);
                }

                if (action?.type === 'PREVIOUS') {
                    return prev.previous(tr.doc);
                }

                return prev;
            }
        },
        props: {
            decorations(state) {
                const searchState = searchPluginKey.getState(state);
                return searchState ? searchState.decorations : DecorationSet.empty;
            }
        }
    });
}

export const searchCommands = {
    find(query, caseSensitive = false) {
        return (state, dispatch) => {
            if (!query || query.trim() === '') {
                return this.clearSearch()(state, dispatch);
            }

            const results = findInDocument(state.doc, query, caseSensitive);

            if (dispatch) {
                dispatch(state.tr.setMeta(searchPluginKey, {
                    type: 'FIND',
                    results,
                    query
                }));
            }

            return true;
        };
    },

    findNext() {
        return (state, dispatch) => {
            const searchState = searchPluginKey.getState(state);
            if (!searchState?.isActive || searchState.results.length === 0) {
                return false;
            }

            if (dispatch) {
                dispatch(state.tr.setMeta(searchPluginKey, { type: 'NEXT' }));
            }

            return true;
        };
    },

    findPrevious() {
        return (state, dispatch) => {
            const searchState = searchPluginKey.getState(state);
            if (!searchState?.isActive || searchState.results.length === 0) {
                return false;
            }

            if (dispatch) {
                dispatch(state.tr.setMeta(searchPluginKey, { type: 'PREVIOUS' }));
            }

            return true;
        };
    },

    clearSearch() {
        return (state, dispatch) => {
            if (dispatch) {
                dispatch(state.tr.setMeta(searchPluginKey, { type: 'CLEAR' }));
            }
            return true;
        };
    },

    getSearchInfo() {
        return (state) => {
            const searchState = searchPluginKey.getState(state);
            return searchState ? searchState.getInfo() : {
                isActive: false,
                query: null,
                current: 0,
                total: 0,
                hasResults: false
            };
        };
    },

    getCurrentResult() {
        return (state) => {
            const searchState = searchPluginKey.getState(state);
            return searchState.getCurrentResult();
        }
    },
};

function findInDocument(doc, query, caseSensitive = false) {
    const results = [];
    const searchText = caseSensitive ? query : query.toLowerCase();

    doc.descendants((node, pos) => {
        if (!node.isTextblock) return;

        const blockText = caseSensitive ?
            node.textContent :
            node.textContent.toLowerCase();

        let index = -1;
        while ((index = blockText.indexOf(searchText, index + 1)) !== -1) {
            const from = pos + 1 + index;
            const to = from + query.length;

            results.push({
                from,
                to,
                text: node.textContent.substring(index, index + query.length)
            });
        }
    });

    return results;
}