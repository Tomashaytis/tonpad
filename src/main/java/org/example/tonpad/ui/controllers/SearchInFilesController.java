package org.example.tonpad.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.service.crypto.EncryptionService;
import org.example.tonpad.core.service.crypto.Impl.EncryptionServiceImpl;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.ui.extentions.SearchResultCell;
import org.example.tonpad.ui.extentions.VaultPath;
import org.example.tonpad.ui.extentions.SearchTreeItem;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class SearchInFilesController extends AbstractController {
    @Getter
    @FXML
    private VBox searchBarVBox;

    @FXML
    public HBox searchFieldHBox;

    @FXML
    private TextField searchResultsField;

    @FXML
    public HBox searchButtonsHBox;

    @FXML
    private TextField searchField;

    @FXML
    private TreeView<String> searchTreeView;

    @Setter
    private Runnable onSearchStarted;

    @Setter
    private Runnable onCancel;

    @Setter
    private Consumer<Path> fileOpenHandler;

    @Setter
    private java.util.function.Consumer<String> onQueryChanged;

    private Thread searchThread;

    private final FileSystemService fileService;

    private volatile boolean searchCancelled = false;

    private final VaultPath vaultPath;

    private Path rootPath;

    private TreeItem<String> rootItem;

    private TreeItem<String> selectedItem;

    private final VaultSession vaultSession;

    @FXML
    private void initialize() {
        var debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        searchField.textProperty().addListener((o, ov, nv) -> {
            debounce.stop();
            debounce.setOnFinished(e -> {
                String query = nv.trim();
                if (!query.isEmpty() && onQueryChanged != null) {
                    onQueryChanged.accept(query);
                }
            });
            debounce.playFromStart();
        });

        searchField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            var code = e.getCode();
            if (code == javafx.scene.input.KeyCode.ENTER) {
                e.consume();
                startSearch();
            } else if (code == javafx.scene.input.KeyCode.ESCAPE) {
                e.consume();
                cancelSearch();
            }
        });
    }

    public void startSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        if (onSearchStarted != null) {
            onSearchStarted.run();
        }

        cancelSearch();

        searchCancelled = false;
        searchThread = new Thread(() -> performSearch(query));
        searchThread.setDaemon(true);
        searchThread.start();
    }

    public void cancelSearch() {
        searchCancelled = true;
        if (searchThread != null && searchThread.isAlive()) {
            searchThread.interrupt();
        }

        if (onCancel != null) {
            onCancel.run();
        }
    }

    public String openFile(Path filePath) {
        if(vaultSession.isOpendWithNoPassword())
        {
            EncryptionService encoder = new EncryptionServiceImpl();
            if (encoder.isOpeningWithNoPasswordAllowed(filePath)) {
                return fileService.readFile(filePath);
            }
            else {
                throw new DecryptionException("Invalid password");
            }
        }
        else
        {
            try
            {
                byte[] key = vaultSession.getMasterKeyIfPresent().map(Key::getEncoded).orElse(null);
                EncryptionService encoder = new EncryptionServiceImpl(key);
                return encoder.decrypt(fileService.readFile(filePath), null);
            }
            catch(DecryptionException e) {
                throw new DecryptionException("Invalid password", e);
            }
        }
    }
    private void performSearch(String query) {
        FileTree fileTree = fileService.getFileTree(rootPath);

        List<SearchTreeItem> mdFiles = collectMdFiles(fileTree);
        SearchTreeItem root = new SearchTreeItem("", true);

        searchTreeView.setCellFactory(tv -> new SearchResultCell(query));

        int totalMatches = 0;
        int filesWithMatches = 0;

        for (SearchTreeItem fileItem : mdFiles) {
            if (searchCancelled) break;

            String filePath = fileItem.getValue();
            Path fullPath = rootPath.resolve(filePath);

            SearchTreeItem fileNode = new SearchTreeItem(filePath, true);
            boolean hasMatches = false;
            int fileMatches = 0;

            String fileContent = openFile(fullPath);
            List<String> lines = Arrays.asList(fileContent.split("\n"));
            for (int i = 0; i < lines.size(); i++) {
                if (searchCancelled) break;

                String line = lines.get(i);
                if (line.toLowerCase().contains(query.toLowerCase())) {
                    SearchTreeItem matchItem = new SearchTreeItem(
                            String.format("%d: %s", i + 1, line.trim()),
                            false
                    );
                    fileNode.getChildren().add(matchItem);
                    hasMatches = true;
                    fileMatches++;
                    totalMatches++;
                }
            }

            if (hasMatches) {
                root.getChildren().add(fileNode);
                filesWithMatches++;
            }
        }

        if (!searchCancelled) {
            int finalTotalMatches = totalMatches;
            int finalFilesWithMatches = filesWithMatches;
            Platform.runLater(() -> {
                searchTreeView.setRoot(root);
                searchTreeView.setShowRoot(false);
                root.setExpanded(true);

                for (TreeItem<String> fileNode : root.getChildren()) {
                    fileNode.setExpanded(true);
                }

                if (finalTotalMatches == 0) {
                    searchResultsField.setText("No matches found");
                } else {
                    if (finalFilesWithMatches == 1) {
                        searchResultsField.setText(String.format("Found %d matches in %d file",
                                finalTotalMatches, finalFilesWithMatches));

                        if (finalTotalMatches == 1) {
                            searchResultsField.setText(String.format("Found %d match in %d file",
                                    finalTotalMatches, finalFilesWithMatches));
                        } else {
                            searchResultsField.setText(String.format("Found %d matches in %d file",
                                    finalTotalMatches, finalFilesWithMatches));
                        }
                    } else {
                        searchResultsField.setText(String.format("Found %d matches in %d files",
                                finalTotalMatches, finalFilesWithMatches));
                    }
                }
            });
        }
    }

    public void focus() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void clearResults() {
        searchTreeView.setRoot(null);
    }

    public void setQuery(String query) {
        searchField.setText(query == null ? "" : query);
    }

    public String getQuery() {
        return searchField.getText().trim();
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(searchBarVBox);

        AnchorPane.setTopAnchor(searchBarVBox, 0.0);
        AnchorPane.setBottomAnchor(searchBarVBox, 0.0);
        AnchorPane.setLeftAnchor(searchBarVBox, 0.0);
        AnchorPane.setRightAnchor(searchBarVBox, 0.0);

        focus();
        rootPath = Path.of(vaultPath.getVaultPath()).resolve("notes");

        searchTreeView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {

            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                var ti = searchTreeView.getSelectionModel().getSelectedItem();
                if (ti != null && ti.isLeaf())
                    onOpenFile(ti);
                e.consume();
            }
        });
    }

    private List<SearchTreeItem> collectMdFiles(FileTree fileTree) {
        List<SearchTreeItem> mdFiles = new ArrayList<>();
        collectMdFilesRecursive(fileTree, mdFiles);
        return mdFiles;
    }

    private void collectMdFilesRecursive(FileTree node, List<SearchTreeItem> result) {
        if (node == null) return;

        if (node.getChildren() == null) {
            String fileName = node.getPath().getFileName().toString();
            if (fileName.toLowerCase().endsWith(".md")) {
                Path relativePath = rootPath.relativize(node.getPath());
                SearchTreeItem item = new SearchTreeItem(relativePath.toString(), true);
                result.add(item);
            }
        } else {
            for (FileTree child : node.getChildren()) {
                collectMdFilesRecursive(child, result);
            }
        }
    }

    private void onOpenFile(TreeItem<String> target) {
        if (target != null && target.isLeaf()) {
            Path filePath;

            if (target.isLeaf()) {
                filePath = rootPath.resolve(target.getParent().getValue());
            } else {
                filePath = rootPath.resolve(target.getValue());
            }

            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath);
            }
        }
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search-in-files-bar.fxml";
    }
}