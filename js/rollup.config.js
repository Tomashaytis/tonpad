import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';

export default {
    input: 'src/index.js',
    output: { 
        file: 'dist/bundle.js', 
        format: 'umd',
        name: 'ProseMirrorBundle',
        globals: {
            'prosemirror-model': 'prosemirrorModel',
            'prosemirror-state': 'prosemirrorState',
            'prosemirror-view': 'prosemirrorView',
            'prosemirror-markdown': 'prosemirrorMarkdown',
            'js-yaml': 'jsyaml'
        }
    },
    plugins: [resolve(), commonjs()]
}