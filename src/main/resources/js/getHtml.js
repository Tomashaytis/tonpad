function getHtml() {
  const r = document.getElementById('note-root') || document.body;
  return r.innerHTML;
}
