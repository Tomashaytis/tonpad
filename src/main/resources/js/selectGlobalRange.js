function selectGlobalRange(start, end) {
  const root = document.getElementById('note-root') || document.body;
  let s = start|0, e = end|0;
  if (e < s) { const t = s; s = e; e = t; }
  const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  let n, acc = 0, sn=null, so=0, en=null, eo=0;
  while ((n = w.nextNode())) {
    const len = n.data.length, nextAcc = acc + len;
    if (!sn && s <= nextAcc) { sn=n; so=Math.max(0, s-acc); }
    if (!en && e <= nextAcc) { en=n; eo=Math.max(0, e-acc); break; }
    acc = nextAcc;
  }
  if (!sn) return false;
  if (!en) { en = sn; eo = so; }
  const r = document.createRange();
  r.setStart(sn, so); r.setEnd(en, eo);
  const sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(r);
  (sn.parentElement||root).scrollIntoView({block:'center'});
  return true;
}
