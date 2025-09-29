package org.example.tonpad.ui.controllers;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.core.service.SearchService.Hit;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchTextController {
    @FXML
    private VBox searchBarVBox;

    @FXML
    private HBox searchFieldHBox;

    @FXML
    private HBox searchButtonsHBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button prevHitButton;

    @FXML
    private Button nextHitButton;

    @FXML
    private TextField searchResultsField;

    @Setter
    private MainController mainController;

    private final SearchService searchService;

    private int currentIndex = -1;

    private final List<Hit> hits = new ArrayList<>();

    public void init()
    {
        setSearchShortCut();
        handleSearchFieldInput();
        handleSearchButtons();
    }

    private void selectPrevHit()
    {
        WebView wv = getActiveWebView();
        if (hits.isEmpty()) return;
        if (currentIndex <= 0) currentIndex = hits.size() - 1;
        else currentIndex--;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());

        searchResultsField.setText((currentIndex + 1) + "/" + hits.size());
    }

    private void selectNextHit()
    {
        WebView wv = getActiveWebView();
        if(hits.isEmpty()) return;
        if(currentIndex < 0 || currentIndex == hits.size() - 1) currentIndex = 0;
        else currentIndex++;
        selectGlobalRange(wv, hits.get(currentIndex).start(), hits.get(currentIndex).end());

        searchResultsField.setText((currentIndex + 1) + "/" + hits.size());
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

    private void handleSearchButtons()
    {
        prevHitButton.setOnAction(e -> selectPrevHit());
        nextHitButton.setOnAction(e -> selectNextHit());

        searchField.setOnKeyPressed(e -> {
            if((e.getCode() == KeyCode.F3 && e.isShiftDown()) || e.getCode() == KeyCode.UP) {selectPrevHit(); e.consume();}
            else if(e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.F3 || e.getCode() == KeyCode.DOWN) {selectNextHit(); e.consume();};
        });
    }

    private void handleSearchFieldInput()
    {
        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> runSearch());
        searchField.textProperty().addListener((o, ov, nv) -> debounce.playFromStart());
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

    private void runSearch() {
        WebView wv = getActiveWebView();
        if(wv == null) {
            return;
        }
    
        String query = searchField.getText().trim();
        if(query.isEmpty()) {
            clearHighlights();
            clearDomSelection(wv);
            hits.clear();
            currentIndex = -1;
            return;
        }
        
        String visibleText = (String) wv.getEngine().executeScript("document.body.textContent");
        this.hits.addAll(searchService.openSession(() -> visibleText, () -> 0).findAll(query));
        highlightInDom();

        searchResultsField.setText("1/" + hits.size());
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

    private void highlightInDom() {
        WebView wv = getActiveWebView();
        if (wv == null || docReady(wv)) {
            return;
        }

        if (hits.isEmpty()) {
            clearHighlights(); return;
        }

        clearHighlights();

        String rangesJson = toJsonRanges(hits);

        String jsBody = """
        const ranges = %s;           // [[start,end), ...] по document.body.textContent
        const root = document.body;

        // Фильтр текстовых нод (как раньше)
        const textFilter = {
            acceptNode(node) {
            const el = node.parentElement;
            if (!el) return NodeFilter.FILTER_REJECT;
            if (el.closest('script,style,template,mark.__hit')) return NodeFilter.FILTER_REJECT;
            for (let e = el; e; e = e.parentElement) {
                const cs = getComputedStyle(e);
                if (cs.display === 'none' || cs.visibility === 'hidden') return NodeFilter.FILTER_REJECT;
            }
            return NodeFilter.FILTER_ACCEPT;
            }
        };

        // 1) Собираем текстовые ноды и их глобальные диапазоны [start,end) в textContent
        const nodes = [];
        {
            const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, textFilter);
            let n, acc = 0;
            while (n = w.nextNode()) {
                const len = n.data.length;
                if (len > 0) nodes.push({ node:n, start:acc, end:acc+len });
                acc += len;
            }
        }

        if (!nodes.length || !ranges.length) return 0;

        // 2) Разложим глобальные диапазоны на локальные сегменты по нодам
        // segments[i] = список {from,to} локальных индексов внутри nodes[i].node
        const segments = Array(nodes.length); // возможно sparse

        let ni = 0;        // индекс текущей ноды
        let ri = 0;        // индекс текущего диапазона
        while (ri < ranges.length && ni < nodes.length) {
            const [rs, re] = ranges[ri];
            const {start:ns, end:ne} = nodes[ni];

            if (re <= ns) { ri++; continue; }   // диапазон целиком до этой ноды
            if (rs >= ne) { ni++; continue; }   // диапазон целиком после этой ноды

            // пересечение диапазона и ноды
            const from = Math.max(0, rs - ns);
            const to   = Math.min(ne - ns, re - ns);
            if (from < to) {
                (segments[ni] ||= []).push({from, to});
            }

            // сдвиг: если диапазон вышел за пределы ноды — двигаем ноду; иначе — следующий диапазон
            if (re > ne) ni++; else ri++;
        }

        // 3) Заменяем ноды фрагментами с <mark>, начиная с конца массива,
        //    чтобы предыдущие индексы не инвалидировались
        for (let i = nodes.length - 1; i >= 0; i--) {
            const segs = segments[i];
            if (!segs || !segs.length) continue;

            const text = nodes[i].node.data;
            // segs уже не перекрываются глобально, но на всякий проверим и отсортируем
            segs.sort((a,b) => a.from - b.from);

            const frag = document.createDocumentFragment();
            let pos = 0;
            for (let s = 0; s < segs.length; s++) {
                const {from, to} = segs[s];
                if (pos < from) frag.appendChild(document.createTextNode(text.slice(pos, from)));
                const mark = document.createElement('mark');
                mark.className = '__hit';
                mark.textContent = text.slice(from, to);
                frag.appendChild(mark);
                pos = to;
            }
            if (pos < text.length) frag.appendChild(document.createTextNode(text.slice(pos)));

            nodes[i].node.parentNode.replaceChild(frag, nodes[i].node);
        }

        return root.querySelectorAll('mark.__hit').length;
        """.formatted(rangesJson);

        Object res = execJS(wv, jsBody);
        if (res instanceof String s && s.startsWith("JS_ERROR:")) {
            System.err.println(s);
        }
    }

    private static String toJsonRanges(List<Hit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(int i = 0; i < hits.size(); i++) {
            Hit hit = hits.get(i);
            if(i > 0) sb.append(',');
            sb.append('[').append(hit.start()).append(',').append(hit.end()).append(']');
        }
        sb.append(']');
        return sb.toString();
    }

    private void setSearchShortCut() {
        if (mainController.getTabPane().getScene() != null) {
            attachAccelerators(mainController.getTabPane().getScene());
            mainController.getTabPane().sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) attachAccelerators(newS);
            });
        }
    }

    private void attachAccelerators(Scene scene) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
            () -> Platform.runLater(this::showSearchBar)
        );

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.ESCAPE),
            () -> Platform.runLater(this::hideSearchBar)
        );
    }

    private WebView getActiveWebView() {
        Tab tab = mainController.getTabPane().getSelectionModel().getSelectedItem();
        return (tab != null && tab.getUserData() instanceof WebView wv) ? wv : null;
    }

    private void showSearchBar() {
        for (Node child : mainController.getLeftStackPane().getChildren()) {
            child.setVisible(false);
        }

        for (Node child : mainController.getLeftToolsPane().getChildren()) {
            child.getStyleClass().remove("toggled-icon-button");
        }

        mainController.getLeftStackPane().setManaged(true);
        mainController.getSearchInTextPane().setVisible(true);

        searchField.requestFocus();
        searchField.selectAll();
        runSearch();
    }

    private void hideSearchBar() {
        mainController.getSearchInTextPane().setVisible(false);
        mainController.getLeftStackPane().setManaged(false);

        WebView wv = getActiveWebView();
        if(wv != null) {
            clearHighlights();
            clearDomSelection(wv);
        }
    }
}
