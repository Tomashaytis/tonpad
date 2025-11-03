import { Editor } from "./editor.js";

window.CustomEditor = Editor;

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('editor');
    if (container) {
        window.editor = new Editor(container, 
`---
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

Links: [note] [link](https://example.com) https://example.com my_email@mail.ru #tag`
        );
    }
});

/*
sss
sss
    ss
        sjjjs
sss
# Heading 1dd
Marks: **em**
sss
[sss](sss)
sss

Marks: *em* **strong** ~~strike~~ __underline__ ==highlight== \`code\`

\`\`\`javascript
console.log("Hello, world!");
\`\`\`
Paragraph text
 */