import { Editor } from "./editor.js";

window.CustomEditor = Editor;

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('editor');
    if (container) {
        window.editor = new Editor(container, `
sss
sss
sss
# Heading 1
Marks: **em**
sss
sss
sss
`
        );
    }
});

/*
Marks: *em* **strong** ~~strike~~ __underline__ ==highlight== \`code\`

\`\`\`javascript
console.log("Hello, world!");
\`\`\`
Paragraph text
 */