function clearDomSelection() {
  try {
    const sel = window.getSelection && window.getSelection();
    if (sel && sel.rangeCount) {
      const r = document.createRange();
      const b = document.body;
      r.setStart(b, 0);
      r.collapse(true);
      sel.removeAllRanges();
      sel.addRange(r);
      sel.removeAllRanges();
    } else if (sel) {
      sel.removeAllRanges();
    }
    if (document.activeElement) document.activeElement.blur();
    return true;
  } catch (e) {
    return "JS_ERROR:" + e;
  }
}
