import { Plugin } from 'prosemirror-state';

export function wordCounterPlugin() {
    let debounceTimer = null;
    
    return new Plugin({
        view(editorView) {
            const wordCountEl = document.getElementById('word-count');
            const charCountEl = document.getElementById('char-count');
            
            if (!wordCountEl || !charCountEl) {
                return {};
            }
            
            const isValidWord = (str) => {
                return /[\p{L}\p{N}]/u.test(str);
            };
            
            const updateStats = () => {
                const doc = editorView.state.doc;
                let words = 0;
                let chars = 0;
                
                doc.descendants((node) => {
                    if (node.isText && node.text) {
                        const text = node.text;
                        chars += text.length;
                        
                        const trimmed = text.trim();
                        if (trimmed) {
                            // Разбиваем по пробельным символам
                            const potentialWords = trimmed.split(/\s+/);
                            
                            for (const word of potentialWords) {
                                if (isValidWord(word)) {
                                    words++;
                                }
                            }
                        }
                    }
                });
                
                wordCountEl.textContent = words;
                charCountEl.textContent = chars;
            };
            
            const debouncedUpdate = () => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(updateStats, 300);
            };
            
            updateStats();
            
            const originalDispatch = editorView.dispatch;
            editorView.dispatch = function(transaction) {
                const result = originalDispatch.call(this, transaction);
                if (transaction.docChanged) {
                    debouncedUpdate();
                }
                return result;
            };
            
            return {
                destroy() {
                    clearTimeout(debounceTimer);
                }
            };
        }
    });
}