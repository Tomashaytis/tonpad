package org.example.tonpad.ui.controllers.search;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.extentions.TriConsumer;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.Impl.AesGcmEncryptor;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.ui.controllers.AbstractController;
import org.example.tonpad.ui.extentions.SearchResultCell;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.example.tonpad.ui.extentions.SearchTreeItem;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
    private TriConsumer<Path, Boolean, EditorMode> fileOpenHandler;

    @Setter
    private java.util.function.Consumer<String> onQueryChanged;

    private Thread searchThread;

    private final FileSystemService fileSystemService;

    private volatile boolean searchCancelled = false;

    private final VaultPathsContainer vaultPathsContainer;

    private final VaultSession vaultSession;

    @FXML
    private void initialize() {
        var debounce = new PauseTransition(Duration.millis(500));
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

        searchField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            var code = e.getCode();
            if (code == KeyCode.ENTER) {
                e.consume();
                startSearch();
            } else if (code == KeyCode.ESCAPE) {
                e.consume();
                cancelSearch();
            }
        });
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(searchBarVBox);

        AnchorPane.setTopAnchor(searchBarVBox, 0.0);
        AnchorPane.setBottomAnchor(searchBarVBox, 0.0);
        AnchorPane.setLeftAnchor(searchBarVBox, 0.0);
        AnchorPane.setRightAnchor(searchBarVBox, 0.0);

        focus();

        searchTreeView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {

            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                var ti = searchTreeView.getSelectionModel().getSelectedItem();
                onOpenFile(ti);
                e.consume();
            } else if ((e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.SECONDARY) &&
                    e.getClickCount() == 1) {
                if (e.getTarget() == searchTreeView ||
                        (e.getTarget() instanceof TreeCell && ((TreeCell<?>) e.getTarget()).getItem() == null)) {
                    searchTreeView.getSelectionModel().clearSelection();
                }
            }
        });
    }

    private void onOpenFile(TreeItem<String> target) {
        if (target != null) {
            Path filePath;

            if (target.isLeaf()) {
                filePath = vaultPathsContainer.getNotesPath().resolve(target.getParent().getValue());
            } else {
                filePath = vaultPathsContainer.getNotesPath().resolve(target.getValue());
            }

            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath, false, EditorMode.NOTE);
            }
        }
    }

    public void startSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            clearResults();
            return;
        }

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
            Encryptor encoder = new AesGcmEncryptor();
            if (encoder.isOpeningWithNoPasswordAllowed(filePath)) {
                return fileSystemService.readFile(filePath);
            }
            else {
                throw new DecryptionException("Invalid password");
            }
        }
        else
        {
            try
            {
                byte[] key = vaultSession.getKeyIfPresent().map(Key::getEncoded).orElse(null);
                Encryptor encoder = new AesGcmEncryptor(key);
                return encoder.decrypt(fileSystemService.readFile(filePath), null);
            }
            catch(DecryptionException e) {
                throw new DecryptionException("Invalid password", e);
            }
        }
    }
    private void performSearch(String query) {
        FileTree fileTree = fileSystemService.getFileTree(vaultPathsContainer.getNotesPath());

        List<SearchTreeItem> mdFiles = collectMdFiles(fileTree);
        SearchTreeItem root = new SearchTreeItem("", true);

        searchTreeView.setCellFactory(tv -> new SearchResultCell(query));

        int totalMatches = 0;
        int filesWithMatches = 0;

        for (SearchTreeItem fileItem : mdFiles) {
            if (searchCancelled) break;

            String filePath = fileItem.getValue();
            Path fullPath = vaultPathsContainer.getNotesPath().resolve(filePath);

            SearchTreeItem fileNode = new SearchTreeItem(filePath, true);
            boolean hasMatches = false;

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

    private List<SearchTreeItem> collectMdFiles(FileTree fileTree) {
        List<SearchTreeItem> mdFiles = new ArrayList<>();
        collectMdFilesRecursive(fileTree, mdFiles);
        return mdFiles;
    }

    private void collectMdFilesRecursive(FileTree node, List<SearchTreeItem> result) {
        if (node == null) return;

        if (node.getChildren() == null) {
            String fileName = node.getPath().getFileName().toString();
            if (fileSystemService.isMarkdownFile(fileName)) {
                Path relativePath = vaultPathsContainer.getNotesPath().relativize(node.getPath());
                SearchTreeItem item = new SearchTreeItem(relativePath.toString(), true);
                result.add(item);
            }
        } else {
            for (FileTree child : node.getChildren()) {
                collectMdFilesRecursive(child, result);
            }
        }
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search/search-in-files-bar.fxml";
    }
}