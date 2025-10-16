package org.example.tonpad.ui.controllers;

import javafx.concurrent.Worker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchInFileTreeController
{

    @Setter
    private TabPane tabPane;

    @Getter
    private final Map<String, List<SearchService.Hit>> hitsMap = new HashMap<>();

    private final SearchFieldController searchFieldController;

    private final FileTreeController fileTreeController;

    private final FileSystemService fileSystemService;

    private final VaultPath vaultPath;

    private int currentIndex = -1;

    public void init(AnchorPane parent) {
        if (!parent.getChildren().contains(searchFieldController.getSearchBarVBox())) {
            searchFieldController.init(parent);
        }
        searchFieldController.setOnQueryChanged(q -> runSearch());
//        searchFieldController.setOnNext(this::selectNextHit);
//        searchFieldController.setOnPrev(this::selectPrevHit);
    }

    public void showSearchBar() {
        searchFieldController.focus();
        runSearch();
    }

    public void hideSearchBar() {
        fileTreeController.setHitsMap(new HashMap<>());
        fileTreeController.refreshTree();
        hitsMap.clear();
        searchFieldController.clearResults();
    }

    private WebView getActiveWebView() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return (tab != null && tab.getUserData() instanceof WebView wv) ? wv : null;
    }

    public void activateSearchBar() {
        searchFieldController.setOnQueryChanged(q -> runSearch());
        searchFieldController.setOnNext(null);
        searchFieldController.setOnPrev(null);

        showSearchBar();
    }

    private void runSearch() {
        currentIndex = -1;

        String query = searchFieldController.getQuery();
        if(query.isEmpty()) {
            fileTreeController.setHitsMap(new HashMap<>());
            hitsMap.clear();
            fileTreeController.refreshTree();
            searchFieldController.clearResults();
            return;
        }
        fileTreeController.setHitsMap(runFileTreeSearch(query));
        fileTreeController.refreshTree();

        searchFieldController.setResults(currentIndex + 1, hitsMap.size());
    }

    private Map<String, List<SearchService.Hit>> runFileTreeSearch(String query) {
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
                    var hits = new java.util.ArrayList<SearchService.Hit>();
                    while ((idx = hay.indexOf(needle, from)) >= 0) {
                        hits.add(new SearchService.Hit(idx, idx + needle.length()));
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
            var hits = new java.util.ArrayList<SearchService.Hit>();
            while ((idx = hay.indexOf(needle, from)) >= 0) {
                hits.add(new SearchService.Hit(idx, idx + needle.length()));
                from = idx + needle.length();
            }
            if (!hits.isEmpty()) {
                map.put(rootName, hits);
            }
        }

        return map;
    }
}
