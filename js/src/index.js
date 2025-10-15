import { Editor } from "./editor.js";

window.CustomEditor = Editor;

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('editor');
    if (container) {
        window.editor = new Editor(container, `
# Heading 1

Marks: *em* **strong** ~~strike~~ __underline__ ==highlight== \`code\`

\`\`\`javascript
console.log("Hello, world!");
\`\`\`
Paragraph text

{{ note type="info" }}
Custom tag content
{{ /note }}
`
        );
    }
});
