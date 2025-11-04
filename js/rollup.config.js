import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';

export default {
    input: 'src/index.js',
    output: [
        { 
            file: 'editor.js', 
            format: 'iife',
            name: 'BrowserEditor',
            globals: {
                'prosemirror-model': 'prosemirrorModel',
                'prosemirror-state': 'prosemirrorState',
                'prosemirror-view': 'prosemirrorView',
                'prosemirror-markdown': 'prosemirrorMarkdown',
                'js-yaml': 'jsyaml'
            }
        },
        { 
            file: '../src/main/resources/editor/editor.js', 
            format: 'iife',
            name: 'WebKitEditor',
            globals: {
                'prosemirror-model': 'prosemirrorModel',
                'prosemirror-state': 'prosemirrorState',
                'prosemirror-view': 'prosemirrorView',
                'prosemirror-markdown': 'prosemirrorMarkdown',
                'js-yaml': 'jsyaml'
            }
        }
    ],
    plugins: [resolve(), commonjs()]
}