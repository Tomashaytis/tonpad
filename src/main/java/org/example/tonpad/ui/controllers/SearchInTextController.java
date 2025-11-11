package org.example.tonpad.ui.controllers;

import java.util.*;

import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.js.funtcion.ClearDomSelection;
import org.example.tonpad.core.js.funtcion.GetHtml;
import org.example.tonpad.core.js.funtcion.JsFunction;
import org.example.tonpad.core.js.funtcion.SelectGlobalRange;
import org.example.tonpad.core.js.funtcion.SetHtml;
import org.example.tonpad.core.js.service.JsExecutionService;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.core.service.SearchService.Hit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

import javafx.application.Platform;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javafx.scene.control.Tab;
import javafx.scene.web.WebView;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchInTextController {

    private final JsExecutionService jsExecutionService;

    private volatile int searchEpoch = 0;
    private volatile boolean active = false;

    @Setter
    private TabPane tabPane;

    @Setter
    private Map<Tab, Editor> EditorMap;

    private final SearchService searchService;

    private final SearchFieldController searchFieldController;

    private int currentIndex = -1;

    private final List<Hit> hits = new ArrayList<>();

    public void init(AnchorPane parent) {
        searchFieldController.init(parent);
        searchFieldController.setOnQueryChanged(q -> runSearch());
        searchFieldController.setOnNext(this::selectNextHit);
        searchFieldController.setOnPrev(this::selectPrevHit);
    }

    public void activateSearchBar() {
        active = true;
        searchFieldController.setOnQueryChanged(q -> runSearch());
        searchFieldController.setOnNext(this::selectNextHit);
        searchFieldController.setOnPrev(this::selectPrevHit);
        showSearchBar();
    }

    private void selectPrevHit() {
        WebView wv = getActiveWebView();
        if (hits.isEmpty()) { searchFieldController.setResults(0, 0); return; }
        if (currentIndex <= 0) currentIndex = hits.size() - 1; else currentIndex--;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());
        searchFieldController.setResults(currentIndex + 1, hits.size());
    }

    private void selectNextHit() {
        WebView wv = getActiveWebView();
        if (hits.isEmpty()) { searchFieldController.setResults(0, 0); return; }
        if (currentIndex < 0 || currentIndex == hits.size() - 1) currentIndex = 0; else currentIndex++;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());
        searchFieldController.setResults(currentIndex + 1, hits.size());
    }

    public void showSearchBar() {
        searchFieldController.focus();
        Platform.runLater(() -> {
            if(!active) return;
            String q = searchFieldController.getQuery();
            if (!q.isEmpty()) {
                runSearch();
            } else {
                hits.clear();
                currentIndex = -1;
                searchFieldController.setResults(0, 0);
            }
        });
    }

    public void hideSearchBar() {
        WebView wv = getActiveWebView();
        if (wv == null) {
            hits.clear();
            currentIndex = -1;
            searchFieldController.clearResults();
            active = false;
            searchEpoch++;
            return;
        }

        String innerHtml = js(wv, GetHtml.class);
        String cleared   = clearHighlights(innerHtml);
        js(wv, SetHtml.class, cleared);
        js(wv, ClearDomSelection.class);
        defocusWebView(wv);

        hits.clear();
        currentIndex = -1;
        searchFieldController.clearResults();
        active = false;
        searchEpoch++;
    }

    private void defocusWebView(WebView wv) {
        if (wv == null || wv.getScene() == null) return;
        wv.getScene().getRoot().requestFocus();
    }

    private boolean selectGlobalRange(WebView wv, int start, int end) {
        if (wv == null) return false;
        Boolean ok = js(wv, SelectGlobalRange.class, start, end);
        return Boolean.TRUE.equals(ok);
    }

    private <T> T js(WebView wv, Class<? extends JsFunction<T>> fn, Object... args) {
        try {
            return jsExecutionService.executeJs(wv, fn, args).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void runSearch() {
        final int epoch = searchEpoch;
        currentIndex = -1;

        WebView wv = getActiveWebView();
        if (wv == null || wv.getEngine().getLoadWorker().getState() != javafx.concurrent.Worker.State.SUCCEEDED) {
            return;
        }

        String query = searchFieldController.getQuery();
        if (query == null) query = "";

        String innerHtml  = js(wv, GetHtml.class);
        String clearText  = clearHighlights(innerHtml);

        Document doc = Jsoup.parseBodyFragment(clearText);
        Element root = doc.body();
        List<TextNodeInfo> nodes = collectTextNodes(root);
        String linear = linearizeFromNodes(nodes);

        hits.clear();

        if (query.isEmpty()) {
            if(active && epoch == searchEpoch) {
                js(wv, SetHtml.class, clearText);
                searchFieldController.clearResults();
            }
            return;
        }

        try (SearchService.Session session = searchService.openSession(() -> linear, () -> 0)) {
            this.hits.addAll(session.findAll(query));
        }

        if(!active || epoch != searchEpoch) return;

        String highlightedText = getHighlightedText(doc, nodes);
        js(wv, SetHtml.class, highlightedText);
        searchFieldController.setResults(0, hits.size());
    }

    private String getHighlightedText(Document doc, List<TextNodeInfo> nodes) {
        List<List<Segment>> segmentsPerNode = splitHitsPerNode(nodes);

        for (int i = nodes.size() - 1; i >= 0; i--) {
            List<Segment> segments = segmentsPerNode.get(i);
            if (segments == null || segments.isEmpty()) continue;

            TextNode node = nodes.get(i).textNode;
            String text = node.getWholeText();

            segments.sort(Comparator.comparingInt(s -> s.from));

            List<Node> parts = new ArrayList<>();
            int pos = 0;
            for (Segment seg : segments) {
                if (pos < seg.from) {
                    parts.add(new TextNode(text.substring(pos, seg.from)));
                }

                Element mark = new Element(Tag.valueOf("mark"), "");
                mark.addClass("__hit");
                mark.text(text.substring(seg.from, seg.to));
                parts.add(mark);
                pos = seg.to;
            }
            if (pos < text.length()) {
                parts.add(new TextNode(text.substring(pos)));
            }

            Element parent = (Element) node.parent();
            int index = node.siblingIndex();
            node.remove();
            for (int p = parts.size() - 1; p >= 0; p--) {
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
    static class TextNodeInfo {
        TextNode textNode;
        int start;
        int end;
    }

    @AllArgsConstructor
    static class Segment {
        int from;
        int to;
    }
}
