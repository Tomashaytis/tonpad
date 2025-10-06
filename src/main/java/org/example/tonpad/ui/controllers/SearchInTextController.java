package org.example.tonpad.ui.controllers;

import java.util.*;

import lombok.Getter;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.core.service.SearchService.Hit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchInTextController extends AbstractController {

    @FXML
    private VBox searchBarVBox;

    @FXML
    public HBox searchFieldHBox;

    @FXML
    public HBox searchButtonsHBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button prevHitButton;

    @FXML
    private Button nextHitButton;

    @FXML
    private TextField searchResultsField;

    @Setter
    private TabPane tabPane;

    private final VaultPath vaultPath;

    private final FileSystemService fileSystemService;

    private final SearchService searchService;

    private final FileTreeController fileTreeController;

    private int currentIndex = -1;

    private final List<Hit> hits = new ArrayList<>();

    @Getter
    private final Map<String, List<Hit>> hitsMap = new HashMap<>();

    private static final String JS_GET_HTML =
        "(function(){var r=document.getElementById('note-root')||document.body; return r.innerHTML;})()";

    private static String jsSetHtml(String html) {
        return "(function(h){var r=document.getElementById('note-root')||document.body; r.innerHTML=h;})("
           + html + ");";
    }

    public void init(AnchorPane parent) {
        handleSearchFieldInput();
        handleSearchButtons();
        parent.getChildren().add(searchBarVBox);

        AnchorPane.setTopAnchor(searchBarVBox, 0.0);
        AnchorPane.setBottomAnchor(searchBarVBox, 0.0);
        AnchorPane.setLeftAnchor(searchBarVBox, 0.0);
        AnchorPane.setRightAnchor(searchBarVBox, 0.0);
    }

    private void handleSearchFieldInput() {
        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> runSearch());
        searchField.textProperty().addListener((o, ov, nv) -> debounce.playFromStart());
    }

    private void handleSearchButtons() {
        prevHitButton.setOnAction(e -> selectPrevHit());
        nextHitButton.setOnAction(e -> selectNextHit());

        EventHandler<KeyEvent> nav = e -> {
            if (e.getTarget() == searchField) {
                if ((e.getCode() == KeyCode.F3 && e.isShiftDown()) || e.getCode() == KeyCode.UP) {
                    e.consume();
                    selectPrevHit();
                } else if (e.getCode() == KeyCode.F3 || e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    selectNextHit();
                }
            }
        };
        searchField.addEventFilter(KeyEvent.KEY_PRESSED,  nav);
    }

    private void selectPrevHit() {
        WebView wv = getActiveWebView();
        if (hits.isEmpty()) return;
        if (currentIndex <= 0) currentIndex = hits.size() - 1;
        else currentIndex--;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());

        searchResultsField.setText((currentIndex + 1) + "/" + hits.size());
    }

    private void selectNextHit() {
        WebView wv = getActiveWebView();
        if(hits.isEmpty()) return;
        if(currentIndex < 0 || currentIndex == hits.size() - 1) currentIndex = 0;
        else currentIndex++;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());

        searchResultsField.setText((currentIndex + 1) + "/" + hits.size());
    }

    public void showSearchBar() {
        searchField.requestFocus();
        searchField.selectAll();

        runSearch();
    }

    public void hideSearchBar() {
        WebView wv = getActiveWebView();
        if(wv != null) {
            currentIndex = -1;
            String innerHtml  = (String) wv.getEngine().executeScript(JS_GET_HTML);
            String clearText = clearHighlights(innerHtml);
            wv.getEngine().executeScript(jsSetHtml(toJsString(clearText)));
        }
    }


    private boolean selectGlobalRange(WebView wv, int start, int end) {
        if (wv == null) return false;
        Object res = execJS(wv, """
        const root = document.getElementById('note-root') || document.body;
        let s = %d, e = %d; if (e < s) { const t=s; s=e; e=t; }
        const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        let n, acc = 0, sn=null, so=0, en=null, eo=0;
        while (n = w.nextNode()) {
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
        """.formatted(start, end));
        return !(res instanceof String s && s.startsWith("JS_ERROR:")) && Boolean.TRUE.equals(res);
    }

    private void clearDomSelection(WebView wv) {
        if (wv == null) return;
        execJS(wv, """
            (function(){
            try {
                const sel = window.getSelection && window.getSelection();
                if (sel) sel.removeAllRanges();      // основное
                // На случай «прилипшего» фокуса:
                if (document.activeElement) document.activeElement.blur();
                // Ещё один способ (старые WebKit):
                if (window.getSelection && window.getSelection().empty) {
                window.getSelection().empty();
                }
                return true;
            } catch(e) { return false; }
            })();
        """);
    }

    public Map<String, List<SearchService.Hit>> runFileTreeSearch(String query) {
        Map<String, List<SearchService.Hit>> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;

        final String needle = query.toLowerCase(java.util.Locale.ROOT);
        final String rootAbs = vaultPath.getVaultPath();
        final java.nio.file.Path rootPath = java.nio.file.Paths.get(rootAbs);
        final String rootName = rootPath.getFileName() != null ? rootPath.getFileName().toString() : rootAbs;

        fileSystemService.findByNameContains(rootAbs, query).stream()
                .map(rel -> {
                    String fileName = rel.getFileName() == null ? "" : rel.getFileName().toString();
                    String hay = fileName.toLowerCase(java.util.Locale.ROOT);
                    int from = 0, idx;
                    var hits = new java.util.ArrayList<Hit>();
                    while ((idx = hay.indexOf(needle, from)) >= 0) {
                        hits.add(new Hit(idx, idx + needle.length()));
                        from = idx + needle.length(); // перекрытия → idx+1
                    }
                    // ключ в формате <rootName>/<rel>
                    String relStr = rel.toString().replace('\\','/');
                    String key = relStr.isEmpty() ? rootName : (rootName + "/" + relStr);
                    return java.util.Map.entry(key, hits);
                })
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> map.put(e.getKey(), e.getValue()));

        {
            String hay = rootName.toLowerCase(java.util.Locale.ROOT);
            int from = 0, idx;
            var hits = new java.util.ArrayList<Hit>();
            while ((idx = hay.indexOf(needle, from)) >= 0) {
                hits.add(new Hit(idx, idx + needle.length()));
                from = idx + needle.length();
            }
            if (!hits.isEmpty()) {
                map.put(rootName, hits); // ключ строго как getRelativePath для корня
            }
        }

        return map;
    }

        private void clearHighlights() {
        currentIndex = -1;
        WebView wv = getActiveWebView();
        if (wv == null || docReady(wv)) return;

        Object res = execJS(wv, """
            const root = document.body;
            const marks = root.querySelectorAll('mark.__hit');
            for (const m of marks) {
                const t = document.createTextNode(m.textContent);
                m.replaceWith(t);
            }
            return marks.length;
        """);
        if (res instanceof String s && s.startsWith("JS_ERROR:")) {
            System.err.println(s);
        }
        clearDomSelection(wv);
    }


    private boolean docReady(WebView wv) {
        return wv.getEngine().getDocument() == null || wv.getEngine().getLoadWorker().getState() != Worker.State.SUCCEEDED;
    }

    private Object execJS(WebView wv, String jsBody) {
        String wrapped = """
            (function(){
            try {
                %s
            } catch (e) {
                console.error("JS ERR:", e);
                return "JS_ERROR:" + (e && e.message ? e.message : e);
            }
            })();
            """.formatted(jsBody);
        return wv.getEngine().executeScript(wrapped);
    }

    private static String toJsString(String s) {
        if (s == null) return "''";
        return "'" + s
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("</script>", "<\\/script>")
        + "'";
    }

    private void runSearch() {
        currentIndex = -1;

        WebView wv = getActiveWebView();
        if (wv == null || wv.getEngine().getLoadWorker().getState() != Worker.State.SUCCEEDED) return;

        String innerHtml  = (String) wv.getEngine().executeScript(JS_GET_HTML);
        String clearText = clearHighlights(innerHtml);

        Document doc = Jsoup.parseBodyFragment(clearText);
        Element root = doc.body();
        List<TextNodeInfo> nodes = collectTextNodes(root);
        String linear = linearizeFromNodes(nodes);

        String query = searchField.getText().trim();
        hits.clear();
        if(query.isEmpty()) {
            fileTreeController.setHitsMap(new HashMap<>());
            fileTreeController.refreshTree();
            hitsMap.clear();
            wv.getEngine().executeScript(jsSetHtml(toJsString(clearText)));
            searchResultsField.clear();
            return;
        }
        fileTreeController.refreshTree();
        fileTreeController.setHitsMap(runFileTreeSearch(query));

        try (SearchService.Session session = searchService.openSession(() -> linear, () -> 0)) {
            this.hits.addAll(session.findAll(query));
        }
        System.out.println(hits);
        String highlightedText = getHighlightedText(doc, nodes);
        wv.getEngine().executeScript(jsSetHtml(toJsString(highlightedText)));

        searchResultsField.setText((currentIndex + 1) + "/" + hits.size());
    }

    private String getHighlightedText(Document doc, List<TextNodeInfo> nodes) {
        List<List<Segment>> segmentsPerNode = splitHitsPerNode(nodes);

        for(int i = nodes.size() - 1; i >= 0; i--)
        {
            List<Segment> segments = segmentsPerNode.get(i);
            if(segments == null || segments.isEmpty()) continue;

            TextNode node = nodes.get(i).textNode;
            String text = node.getWholeText();

            segments.sort(Comparator.comparingInt(s -> s.from));

            List<Node> parts = new ArrayList<>();
            int pos = 0;
            for(Segment seg: segments)
            {
                if(pos < seg.from)
                {
                    parts.add(new TextNode(text.substring(pos, seg.from)));
                }

                Element mark = new Element(Tag.valueOf("mark"), "");
                mark.addClass("__hit");
                mark.text(text.substring(seg.from, seg.to));
                parts.add(mark);
                pos = seg.to;
            }
            if(pos < text.length())
            {
                parts.add(new TextNode(text.substring(pos)));
            }

            Element parent = (Element)node.parent();
            int index = node.siblingIndex();
            node.remove();
            for(int p = parts.size() - 1; p >= 0; p--)
            {
                parent.insertChildren(index, parts.get(p));
            }
        }

        return doc.body().html();
    }

    private String clearHighlights(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("mark.__hit").forEach(Element::unwrap);
        return doc.body().html();
    }

    private WebView getActiveWebView() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return (tab != null && tab.getUserData() instanceof WebView wv) ? wv : null;
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search-bar.fxml";
    }

    private List<List<Segment>> splitHitsPerNode(List<TextNodeInfo> nodes)
    {
        List<List<Segment>> result = new ArrayList<>(Collections.nCopies(nodes.size(), null));
        int ni = 0, hi = 0;
        while(hi < hits.size() && ni < nodes.size())
        {
            Hit hit = hits.get(hi);
            TextNodeInfo node = nodes.get(ni);

            if(hit.end() <= node.start) {hi++; continue;}
            if(hit.start() >= node.end) {ni++; continue;}

            int from = Math.max(0, hit.start() - node.start);
            int to = Math.min(hit.end() - node.start, node.end - node.start);
            if(from < to)
            {
                List<Segment> list = result.get(ni);
                if(list == null)
                {
                    list = new ArrayList<>();
                    result.set(ni, list);
                }
                list.add(new Segment(from, to));
            }

            if (hit.end() > node.end) ni++;
            else hi++;
        }
        return result;
    }

    private List<TextNodeInfo> collectTextNodes(Node root) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.addLast(root);
        int total = 0;
        List<TextNodeInfo> out = new ArrayList<>();

        while (!stack.isEmpty()) {
            Node cur = stack.removeLast();
            if (cur instanceof Element el) {
                String tag = el.tagName();
                if ("script".equals(tag) || "style".equals(tag) || "template".equals(tag)) continue; // если есть свои тэги, которые скрывают текст и т.п. - сюда же их. Мб потом откуда-то список таких тэгов подсасываться будет
            }
            if (cur instanceof TextNode tn) {
                String text = tn.getWholeText();
                if (!text.isEmpty()) {
                    int start = total;
                    int end = start + text.length();
                    out.add(new TextNodeInfo(tn, start, end));
                    total = end;
                }
            }
            for (int i = cur.childNodeSize() - 1; i >= 0; i--) {
                stack.addLast(cur.childNode(i));
            }
        }
        return out;
    }

    private static String linearizeFromNodes(List<TextNodeInfo> nodes) {
        StringBuilder sb = new StringBuilder();
        for (TextNodeInfo info : nodes) sb.append(info.textNode.getWholeText());
        return sb.toString();
    }



    @AllArgsConstructor
    static class TextNodeInfo
    {
        TextNode textNode;
        int start;
        int end;
    }

    @AllArgsConstructor
    static class Segment
    {
        int from;
        int to;
    }
}
