function insertTextAtPosition(html, position) {
    const body = document.body;
    const walker = document.createTreeWalker(body, NodeFilter.SHOW_TEXT, null, false);

    let currentNode;
    let offset = 0;

    while ((currentNode = walker.nextNode())) {
        const nextOffset = offset + currentNode.textContent.length;
        if (position <= nextOffset) {
            const range = document.createRange();
            const innerOffset = position - offset;
            range.setStart(currentNode, innerOffset);
            range.setEnd(currentNode, innerOffset);

            const fragment = range.createContextualFragment(html);
            range.insertNode(fragment);
            return;
        }
        offset = nextOffset;
    }

    body.insertAdjacentHTML('beforeend', html);
}
