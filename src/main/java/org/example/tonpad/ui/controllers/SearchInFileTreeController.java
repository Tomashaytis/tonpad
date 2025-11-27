package org.example.tonpad.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.ui.controllers.AbstractController;
import org.example.tonpad.ui.controllers.FileTreeController;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchInFileTreeController extends AbstractController {

    @Getter
    @FXML
    private VBox searchBarVBox;

    @FXML
    private TextField searchField;

    private final FileTreeController fileTreeController;

    private final FileSystemService fileSystemService;

    private final VaultPathsContainer vaultPathsContainer;

    public SearchInFileTreeController(FileTreeController fileTreeController,
                                      FileSystemService fileSystemService,
                                      VaultPathsContainer vaultPathsContainer) {
        this.fileTreeController = fileTreeController;
        this.fileSystemService = fileSystemService;
        this.vaultPathsContainer = vaultPathsContainer;
    }

    @FXML
    private void initialize() {
        var debounce = new PauseTransition(Duration.millis(400));
        searchField.textProperty().addListener((o, ov, nv) -> {
            debounce.stop();
            debounce.setOnFinished(e -> runSearch());
            debounce.playFromStart();
        });
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(searchBarVBox);
        AnchorPane.setTopAnchor(searchBarVBox, 0.0);
        AnchorPane.setLeftAnchor(searchBarVBox, 0.0);
    }

    public void showSearchBar() {
        focus();
        runSearch();
    }

    public void hideSearchBar() {
        fileTreeController.setHitsMap(new HashMap<>());
        fileTreeController.refreshTree();
        setQuery("");
    }

    public void activateSearchBar() {
        showSearchBar();
    }

    public void runSearch() {
        String query = getQuery();
        if (query.isEmpty()) {
            fileTreeController.setHitsMap(new HashMap<>());
            fileTreeController.refreshTree();
            return;
        }

        Map<String, List<SearchService.Hit>> searchResults = runFileTreeSearch(query);
        fileTreeController.setHitsMap(searchResults);
        fileTreeController.refreshTree();
    }

    private Map<String, List<SearchService.Hit>> runFileTreeSearch(String query) {
        Map<String, List<SearchService.Hit>> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;

        final String needle = query.toLowerCase();
        final Path rootPath = vaultPathsContainer.getNotesPath();
        final String rootAbs = rootPath.toString();
        final String rootName = rootPath.getFileName() != null ? rootPath.getFileName().toString() : rootAbs;

        fileSystemService.findByNameContains(rootAbs, query).stream()
                .map(rel -> {
                    String fileName = rel.getFileName() == null ? "" : rel.getFileName().toString();
                    String hay = fileName.toLowerCase();
                    int from = 0, idx;
                    var hits = new ArrayList<SearchService.Hit>();
                    while ((idx = hay.indexOf(needle, from)) >= 0) {
                        hits.add(new SearchService.Hit(idx, idx + needle.length()));
                        from = idx + needle.length();
                    }
                    String relStr = rel.toString().replace('\\', '/');
                    String key = relStr.isEmpty() ? rootName : (rootName + "/" + relStr);
                    return Map.entry(key, hits);
                })
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> map.put(e.getKey(), e.getValue()));

        // Поиск в корневом имени
        String hay = rootName.toLowerCase();
        int from = 0, idx;
        var hits = new ArrayList<SearchService.Hit>();
        while ((idx = hay.indexOf(needle, from)) >= 0) {
            hits.add(new SearchService.Hit(idx, idx + needle.length()));
            from = idx + needle.length();
        }
        if (!hits.isEmpty()) {
            map.put(rootName, hits);
        }

        return map;
    }

    public void focus() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void setQuery(String q) {
        searchField.setText(q == null ? "" : q);
    }

    public String getQuery() {
        return searchField.getText().trim();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search-in-file-tree-bar.fxml";
    }
}