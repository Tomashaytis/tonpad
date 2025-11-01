function clearMarks() {
  const root = document.getElementById('note-root') || document.body;
  const marks = root.querySelectorAll('mark.__hit');
  let n = 0;
  marks.forEach(m => { m.replaceWith(document.createTextNode(m.textContent)); n++; });
  root.normalize();
  return n;
}
