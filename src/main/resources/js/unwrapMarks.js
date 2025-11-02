function unwrapMarks() {
  const root = document.getElementById('note-root') || document.body;
  const marks = root.querySelectorAll('mark.__hit');
  let cnt = 0;
  for (const m of marks) {
    const t = document.createTextNode(m.textContent);
    m.replaceWith(t);
    cnt++;
  }
  return cnt;
}
