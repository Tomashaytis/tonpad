package org.example.tonpad.ui.controllers.search;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.editor.dto.SearchResult;
import javafx.application.Platform;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchInTextController extends AbstractController {

    @Getter
    @FXML
    private VBox searchBarVBox;

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

    @Setter
    private Map<Tab, Editor> editorMap;

    @FXML
    private void initialize() {
        var debounce = new PauseTransition(Duration.millis(400));
        searchField.textProperty().addListener((o, ov, nv) -> {
            debounce.stop();
            debounce.setOnFinished(e -> runSearch());
            debounce.playFromStart();
        });

        searchField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            var code = e.getCode();
            if ((code == KeyCode.F3 && e.isShiftDown()) || code == KeyCode.UP) {
                e.consume();
                selectPrevHit();
            } else if (code == KeyCode.F3 || code == KeyCode.DOWN || code == KeyCode.ENTER) {
                e.consume();
                selectNextHit();
            }
        });

        prevHitButton.setOnAction(e -> selectPrevHit());
        nextHitButton.setOnAction(e -> selectNextHit());
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(searchBarVBox);
        AnchorPane.setTopAnchor(searchBarVBox, 0.0);
        AnchorPane.setRightAnchor(searchBarVBox, 14.0);
    }

    private void selectPrevHit() {
        Editor editor = getActiveEditor();
        if (editor == null) return;
        editor.findPrevious().thenAccept(this::handleSearchResult);
    }

    private void selectNextHit() {
        Editor editor = getActiveEditor();
        if (editor == null) return;
        editor.findNext().thenAccept(this::handleSearchResult);
    }

    public void showSearchBar() {
        focus();
        Platform.runLater(() -> {
            String q = getQuery();
            if (!q.isEmpty()) {
                runSearch();
            } else {
                clearResults();
            }
        });
    }

    public void hideSearchBar() {
        Editor editor = getActiveEditor();
        if (editor != null) {
            editor.clearSearch();
        }
        clearResults();
    }

    private void runSearch() {
        Editor editor = getActiveEditor();
        if (editor == null) return;

        String query = getQuery();
        editor.find(query).thenAccept(this::handleSearchResult);
    }

    private Editor getActiveEditor() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return editorMap.get(tab);
    }

    private void handleSearchResult(SearchResult searchResult) {
        if (searchResult != null && searchResult.isActive()) {
            setResults(searchResult.getCurrent(), searchResult.getTotal());
        } else {
            clearResults();
        }
    }

    public void focus() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void clearResults() {
        searchResultsField.setText("");
    }

    public void setResults(int current1based, int total) {
        searchResultsField.setText(total <= 0 ? "" : (current1based + "/" + total));
    }

    public void setQuery(String q) {
        searchField.setText(q == null ? "" : q);
    }

    public String getQuery() {
        return searchField.getText().trim();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search-in-text-bar.fxml";
    }
}