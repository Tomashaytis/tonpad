function setHtml(html) {
  const r = document.getElementById('note-root') || document.body;
  r.innerHTML = html;
  return true;
}
