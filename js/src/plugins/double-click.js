import { Plugin } from 'prosemirror-state';
import { TextSelection } from 'prosemirror-state';

export function doubleClickPlugin() {
    let clickCount = 0;
    let clickTimer = null;
    
    return new Plugin({
        props: {
            handleDOMEvents: {
                mousedown: (view, event) => {
                    clickCount++;
                    
                    if (clickCount === 1) {
                        clickTimer = setTimeout(() => {
                            clickCount = 0;
                            clickTimer = null;
                        }, 300);
                    } else if (clickCount === 2) {
                        clearTimeout(clickTimer);
                        clickCount = 0;
                        clickTimer = null;
                        
                        const pos = view.posAtCoords({ left: event.clientX, top: event.clientY });
                        if (!pos) return false;

                        const $pos = view.state.doc.resolve(pos.pos);
                        const text = $pos.parent.textContent;
                        const offset = $pos.parentOffset;

                        const isLetterOrDigit = (char) => {
                            if (!char) return false;
                            return /[\p{L}\p{N}]/u.test(char);
                        };

                        let start = offset;
                        let end = offset;

                        while (start > 0 && isLetterOrDigit(text[start - 1])) {
                            start--;
                        }

                        while (end < text.length && isLetterOrDigit(text[end])) {
                            end++;
                        }

                        if (start < end) {
                            const nodeStart = $pos.start();
                            const from = nodeStart + start;
                            const to = nodeStart + end;

                            const tr = view.state.tr.setSelection(
                                TextSelection.create(view.state.doc, from, to)
                            );
                            view.dispatch(tr);
                            
                            event.preventDefault();
                            event.stopPropagation();
                            return true;
                        }
                    }
                    return false;
                },
                dblclick: (view, event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    return true;
                }
            }
        }
    });
}