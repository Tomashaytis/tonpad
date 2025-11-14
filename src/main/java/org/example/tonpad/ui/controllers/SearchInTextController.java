package org.example.tonpad.ui.controllers;

import java.util.*;

import org.example.tonpad.core.editor.Editor;

import javafx.application.Platform;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javafx.scene.control.Tab;
import org.example.tonpad.core.editor.dto.SearchResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchInTextController {

    @Setter
    private TabPane tabPane;

    @Setter
    private Map<Tab, Editor> EditorMap;

    private final SearchFieldController searchFieldController;

    public void init(AnchorPane parent) {
        searchFieldController.init(parent);
        searchFieldController.setOnQueryChanged(q -> runSearch());
        searchFieldController.setOnNext(this::selectNextHit);
        searchFieldController.setOnPrev(this::selectPrevHit);
    }

    public void activateSearchBar() {
        searchFieldController.setOnQueryChanged(q -> runSearch());
        searchFieldController.setOnNext(this::selectNextHit);
        searchFieldController.setOnPrev(this::selectPrevHit);
        showSearchBar();
    }

    private void selectPrevHit() {
        Editor editor = getActiveEditor();
        if (editor == null) {
            return;
        }

        editor.findPrevious().thenAccept(this::handleSearchResult);
    }

    private void selectNextHit() {
        Editor editor = getActiveEditor();
        if (editor == null) {
            return;
        }

        editor.findNext().thenAccept(this::handleSearchResult);
    }

    public void showSearchBar() {
        searchFieldController.focus();
        Platform.runLater(() -> {
            String q = searchFieldController.getQuery();
            if (!q.isEmpty()) {
                runSearch();
            } else {
                searchFieldController.setResults(0, 0);
            }
        });
    }

    public void hideSearchBar() {
        Editor editor = getActiveEditor();
        if (editor == null) {
            searchFieldController.clearResults();
            return;
        }

        editor.clearSearch();
        searchFieldController.clearResults();
    }

    private void runSearch() {
        Editor editor = getActiveEditor();
        if (editor == null) {
            return;
        }

        String query = searchFieldController.getQuery();
        if (query == null) {
            query = "";
        }

        editor.find(query).thenAccept(this::handleSearchResult);
    }

    private Editor getActiveEditor() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return EditorMap.getOrDefault(tab, null);
    }

    private void handleSearchResult(SearchResult searchResult) {
        if (searchResult != null && searchResult.isActive()) {
            searchFieldController.setResults(searchResult.getCurrent(), searchResult.getTotal());
        } else {
            searchFieldController.clearResults();
        }
    }
}
