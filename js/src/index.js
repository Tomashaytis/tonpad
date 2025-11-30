import { Editor } from "./editor.js";

(function () {
    'use strict';

    window.debugAlerts = {
        enabled: false,

        enable: function () {
            if (this.enabled) return;
            this.enabled = true;

            const originalConsole = window.console;

            function formatErrorWithStack(error) {
                if (!error) return 'No error object';

                let message = error.message || String(error);
                let stack = error.stack || '';

                if (stack.includes(message)) {
                    return stack;
                }

                return `${message}\n${stack}`;
            }

            window.console = {
                log: function (...args) {
                    originalConsole.log.apply(originalConsole, args);
                    try {
                        const message = args.map(arg =>
                            typeof arg === 'object' ? JSON.stringify(arg, null, 2) : String(arg)
                        ).join(' ');
                        alert('LOG: ' + message);
                    } catch (e) { }
                },

                error: function (...args) {
                    originalConsole.error.apply(originalConsole, args);
                    try {
                        const formattedArgs = args.map(arg => {
                            if (arg instanceof Error) {
                                return formatErrorWithStack(arg);
                            }
                            return typeof arg === 'object' ? JSON.stringify(arg, null, 2) : String(arg);
                        });
                        alert('ERROR: ' + formattedArgs.join('\n'));
                    } catch (e) { }
                },

                warn: function (...args) {
                    originalConsole.warn.apply(originalConsole, args);
                    try {
                        const formattedArgs = args.map(arg => {
                            if (arg instanceof Error) {
                                return formatErrorWithStack(arg);
                            }
                            return String(arg);
                        });
                        alert('WARN: ' + formattedArgs.join('\n'));
                    } catch (e) { }
                },

                info: function (...args) {
                    originalConsole.info.apply(originalConsole, args);
                    try {
                        alert('INFO: ' + args.join(' '));
                    } catch (e) { }
                },

                debug: originalConsole.debug,
                table: originalConsole.table,
                time: originalConsole.time,
                timeEnd: originalConsole.timeEnd,
                clear: originalConsole.clear,
                trace: originalConsole.trace
            };

            window.onerror = function (msg, url, lineNo, columnNo, error) {
                const errorDetails = `
Message: ${msg}
URL: ${url}
Line: ${lineNo}
Column: ${columnNo}
${error ? formatErrorWithStack(error) : 'No stack trace available'}
                `.trim();

                originalConsole.error('Global Error:', errorDetails);
                alert('ERROR: ' + errorDetails);

                return false;
            };

            window.addEventListener('unhandledrejection', function (event) {
                const error = event.reason;
                const errorDetails = error instanceof Error ?
                    formatErrorWithStack(error) :
                    String(error);

                originalConsole.error('Unhandled Promise Rejection:', error);
                alert('ERROR: ' + errorDetails);

                event.preventDefault();
            });

            const originalTrace = originalConsole.trace;
            originalConsole.trace = function (...args) {
                const stack = new Error().stack;
                const traceMessage = `${args.join(' ')}\n${stack}`;
                alert('ERROR: ' + traceMessage);
                originalTrace.apply(originalConsole, args);
            };

            const originalSetTimeout = window.setTimeout;
            const originalSetInterval = window.setInterval;

            window.setTimeout = function (callback, delay, ...args) {
                return originalSetTimeout(function () {
                    try {
                        callback.apply(this, args);
                    } catch (error) {
                        const errorDetails = formatErrorWithStack(error);
                        alert('ERROR: ' + errorDetails);
                        throw error;
                    }
                }, delay);
            };

            window.setInterval = function (callback, delay, ...args) {
                return originalSetInterval(function () {
                    try {
                        callback.apply(this, args);
                    } catch (error) {
                        const errorDetails = formatErrorWithStack(error);
                        alert('ERROR: ' + errorDetails);
                        throw error;
                    }
                }, delay);
            };

            const originalAddEventListener = EventTarget.prototype.addEventListener;
            EventTarget.prototype.addEventListener = function (type, listener, options) {
                const wrappedListener = function (...args) {
                    try {
                        return listener.apply(this, args);
                    } catch (error) {
                        const errorDetails = formatErrorWithStack(error);
                        alert('ERROR: ' + errorDetails);
                        throw error;
                    }
                };

                return originalAddEventListener.call(this, type, wrappedListener, options);
            };

            alert('INFO: Debug alerts enabled');
        },

        disable: function () {
            if (!this.enabled) return;
            this.enabled = false;

            console.log('Debug alerts disabled');
        },

        toggle: function () {
            if (this.enabled) {
                this.disable();
            } else {
                this.enable();
            }
            return this.enabled;
        }
    };

    window.createEditor = function (mode = 'note') {
        const container = document.getElementById('editor');
        if (container) {
            window.editor = new Editor(container, mode);

            console.log('Editor created successfully');
        }
    };

    window.deleteEditor = function () {
        if (window.editor) {
            if (typeof window.editor.destroy === 'function') {
                window.editor.destroy();
            }

            window.editor = null;

            const container = document.getElementById('editor');
            if (container) {
                container.innerHTML = '';
            }

            console.log('Editor deleted successfully');
        }
    };

})();

/*
document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('editor');
    if (container) {
        window.editor = new Editor(container);
    }
});

window.editor.setNoteContent(`---
author: pavel
time: 12:15
message: Hi
---

# Heading 1
## Heading 2

Quotes:
> "It's easy!"
> ProseMirror

Lists:
    1
        2
- item 1
- item 2
    - item 3
    - item 4
1. one
2. two
3. thee

Marks: *em* **strong** ~~strike~~ ==highlight== __underline__ \`code\`

Links: [note] [link](https://example.com) https://example.com my_email@mail.ru #tag`);
 */