package org.example.tonpad.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.models.SortKey;
import org.example.tonpad.core.models.SortOptions;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.ui.extentions.FileTreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(FileTreeController.class);
    @FXML
    private TreeView<String> fileTreeView;

    @FXML
    private VBox fileTreeVBox;

    @FXML
    private HBox fileTreeToolsHBox;

    @FXML
    private Button addNoteButton;

    @FXML
    private Button addDirectoryButton;

    @FXML
    private Button addCollectionButton;

    @FXML
    private Button refreshFilesButton;

    @FXML
    private Button sortFilesButton;

    @FXML
    private Button expandFilesButton;

    @FXML
    private ContextMenu sortMenu;
    @FXML
    private ToggleGroup sortToggleGroup;
    @FXML
    private RadioMenuItem miNameAsc, miNameDesc, miCreatedNewest, miCreatedOldest;
    @FXML
    private CheckBox cbFoldersFirst, cbRelevantOnly;
    
    private SortKey sortKey = SortKey.NAME_ASC;

    private final FileSystemService fileSystemService;

    private static final javafx.css.PseudoClass MATCHED = javafx.css.PseudoClass.getPseudoClass("matched");

    private TreeItem<String> rootItem;

    private TreeItem<String> selectedItem;

    private String vaultPath;

    private boolean isCollapsed = true;

    @Setter
    private Consumer<String> fileOpenHandler;

    @Setter
    private Map<String, List<SearchService.Hit>> hitsMap = Collections.emptyMap();

    private final Map<String, Boolean> expandedState = new HashMap<>();

    public void init(AnchorPane parent, String path) {
        this.vaultPath = path;

        parent.getChildren().add(fileTreeVBox);
        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);

        setupEventHandlers();
        setupFileTree();
    }

    private void setupEventHandlers() {
        expandFilesButton.setOnAction(e -> {
            ImageView imageView = (ImageView) expandFilesButton.getGraphic();

            if (isCollapsed) {
                expandAll();
                imageView.getStyleClass().remove("expand-icon");
                imageView.getStyleClass().add("collapse-icon");
            } else {
                collapseAll();
                imageView.getStyleClass().remove("collapse-icon");
                imageView.getStyleClass().add("expand-icon");
            }
            isCollapsed = !isCollapsed;
        });

        refreshFilesButton.setOnAction(e -> refreshTree());

        setDeleteShortCut();

        sortFilesButton.setOnAction(e -> {
            if (sortMenu == null) {
                throw new IllegalStateException("sortMenu is null");
            }
            if (sortMenu.isShowing()) sortMenu.hide();
            else sortMenu.show(sortFilesButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        cbFoldersFirst.selectedProperty().addListener((obs, o, n) -> applySortOptions());
        cbRelevantOnly.selectedProperty().addListener((obs, o, n) -> applySortOptions());
        miCreatedNewest.setUserData(SortKey.CREATED_NEWEST);
        miCreatedOldest.setUserData(SortKey.CREATED_OLDEST);
        miNameAsc.setUserData(SortKey.NAME_ASC);
        miNameDesc.setUserData(SortKey.NAME_DESC); // при смене порядка меню убирается и рефреш сразу, при выборе нижних двух не закрывается сразу, а по ESC, и не обновляется, вручную надо рефрешить, или изменить порядок

        if(sortToggleGroup.getSelectedToggle() != null)
        {
            Object ud = sortToggleGroup.getSelectedToggle().getUserData();
            if(ud instanceof SortKey sk) sortKey = sk;
        }

        miNameAsc.setOnAction(e -> onSortPicked(SortKey.NAME_ASC));
        miNameDesc.setOnAction(e -> onSortPicked(SortKey.NAME_DESC));
        miCreatedNewest.setOnAction(e -> onSortPicked(SortKey.CREATED_NEWEST));
        miCreatedOldest.setOnAction(e -> onSortPicked(SortKey.CREATED_OLDEST));
        
        
        
        fileTreeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                onOpenFile();
            }
        });
        
        addNoteButton.setOnAction(e -> {
            String newFilePath = addNote();
            refreshTree();
            selectItem(newFilePath);
            fileOpenHandler.accept(newFilePath);
        });
        
        addDirectoryButton.setOnAction(e -> {
            String dirPath = addDir();
            refreshTree();
            selectItem(dirPath);
        });
        
        applySortOptions();
    }

    private void onSortPicked(SortKey picked)
    {
        if(sortKey != picked) sortKey = picked;
        applySortOptions();
        if(sortMenu != null && sortMenu.isShowing()) sortMenu.hide();
    }

    private void onSortFlagsChanged()
    {
    }
    
    private void applySortOptions()
    {
        refreshTree();        
    }

    private void onOpenFile() {
        if (selectedItem != null && selectedItem.isLeaf()) {
            String filePath = getFullPath(selectedItem);
            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath);
            }
        }
    }

    private void selectItem(String path) {
        Path relativePath = Path.of(vaultPath).relativize(Path.of(path));
        TreeItem<String> item = findTreeItemByPath(rootItem, relativePath);
        if (item != null) {
            fileTreeView.getSelectionModel().select(item);
            selectedItem = item;
            fileTreeView.scrollTo(fileTreeView.getRow(item));
        }
    }

    private TreeItem<String> findTreeItemByPath(TreeItem<String> root, Path path) {
        if (path.getNameCount() == 0) return root;

        String firstSegment = path.getName(0).toString();
        for (TreeItem<String> child : root.getChildren()) {
            if (child.getValue().equals(firstSegment)) {
                if (path.getNameCount() == 1) {
                    return child;
                } else {
                    return findTreeItemByPath(child, path.subpath(1, path.getNameCount()));
                }
            }
        }
        return null;
    }

    public void refreshTree() {
        saveAllExpandedStates();
        SortOptions opt = new SortOptions(sortKey, cbFoldersFirst.isSelected(), cbRelevantOnly.isSelected());
        FileTree fileTree = fileSystemService.getFileTreeSorted(vaultPath, opt);
        TreeItem<String> newRoot = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(newRoot);

        fileTreeView.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    pseudoClassStateChanged(MATCHED, false);
                    return;
                }

                String rel = norm(getRelativePath(getTreeItem()));

                List<SearchService.Hit> ranges = hitsMap != null ? hitsMap.get(rel) : null;
                if (ranges == null || ranges.isEmpty()) {
                    setGraphic(null);
                    setText(item);                  // обычный текст
                    pseudoClassStateChanged(MATCHED, false);
                } else {
                    setText(null);
                    setGraphic(buildHighlightedName(item, ranges)); // TextFlow с подсветкой
                    pseudoClassStateChanged(MATCHED, true);         // фон строки (см. CSS)
                }
            }
        });

        rootItem = newRoot;

        restoreAllExpandedStates();
    }

    private void saveAllExpandedStates() {
        expandedState.clear();
        if (rootItem != null) {
            saveExpandedStateRecursive(rootItem);
        }
    }

    private void saveExpandedStateRecursive(TreeItem<String> item) {
        if (item != null) {
            String path = getRelativePath(item);
            expandedState.put(path, item.isExpanded());

            for (TreeItem<String> child : item.getChildren()) {
                saveExpandedStateRecursive(child);
            }
        }
    }

    private void restoreAllExpandedStates() {
        if (rootItem != null) {
            restoreExpandedStateRecursive(rootItem);
        }
    }

    private void restoreExpandedStateRecursive(TreeItem<String> item) {
        if (item != null) {
            String path = getRelativePath(item);
            Boolean wasExpanded = expandedState.get(path);
            if (wasExpanded != null) {
                item.setExpanded(wasExpanded);
            }

            for (TreeItem<String> child : item.getChildren()) {
                restoreExpandedStateRecursive(child);
            }
        }
    }

    private String getRelativePath(TreeItem<String> item) {
        var parts = new java.util.ArrayList<String>();
        for (TreeItem<String> cur = item; cur != null; cur = cur.getParent()) {
            if (cur.getValue() == null || cur.getValue().isEmpty()) break;
            parts.add(0, cur.getValue()); // корень тоже включаем
        }
        return String.join("/", parts).replace('\\','/');
    }

    private String getFullPath(TreeItem<String> item) {
        if (item == null) {
            return vaultPath;
        }

        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current.getParent() != null) {
            pathSegments.addFirst(current.getValue());
            current = current.getParent();
        }

        return Path.of(vaultPath, pathSegments.toArray(new String[0])).toString();
    }

    private void expandAll() {
        expandAllRecursive(rootItem);
    }

    private void collapseAll() {
        collapseAllRecursive(rootItem);
    }

    private void expandAllRecursive(TreeItem<String> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<String> child : item.getChildren()) {
                expandAllRecursive(child);
            }
        }
    }

    private void collapseAllRecursive(TreeItem<String> item) {
        if (item != null) {
            item.setExpanded(false);
            for (TreeItem<String> child : item.getChildren()) {
                collapseAllRecursive(child);
            }
        }
    }

    private void setupFileTree() {
        FileTree fileTree = fileSystemService.getFileTree(vaultPath);

        rootItem = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(rootItem);

        rootItem.setExpanded(true);

        fileTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onFileSelected(newValue)
        );

        fileTreeView.setOnContextMenuRequested(this::onRightClick);
    }

    private static String norm(String s) {
        if (s == null) return "";
        s = s.replace('\\', '/');
        if (s.startsWith("./")) s = s.substring(2);
        return s;
    }

    private javafx.scene.Node buildHighlightedName(String name, java.util.List<SearchService.Hit> hits) {
        // предполагаем, что hits отсортированы и не пересекаются
        var flow = new javafx.scene.text.TextFlow();
        int i = 0, n = name.length();
        for (var h : hits) {
            int s = Math.max(0, Math.min(h.start(), n));
            int e = Math.max(0, Math.min(h.end(),   n));
            if (e <= s) continue;

            if (i < s) flow.getChildren().add(new javafx.scene.text.Text(name.substring(i, s)));
            var t = new javafx.scene.text.Text(name.substring(s, e));
            t.getStyleClass().add("filetree-hit");   // стиль подсвеченного фрагмента
            flow.getChildren().add(t);
            i = e;
        }
        if (i < n) flow.getChildren().add(new javafx.scene.text.Text(name.substring(i)));
        return flow;
    }


    private void onFileSelected(TreeItem<String> selectedItem) {
        this.selectedItem = selectedItem;
    }

    private void onRightClick(ContextMenuEvent event) {

    }

    private TreeItem<String> convertFileTreeToTreeItem(FileTree fileTree) {
        Path path = fileTree.getPath();
        String fileName = path.getFileName() != null ? path.getFileName().toString() : path.toString();

        boolean isDirectory = fileTree.getChildren() != null;

        FileTreeItem treeItem = new FileTreeItem(fileName, isDirectory);

        treeItem.setValue(fileName);

        if (isDirectory && !fileTree.getChildren().isEmpty()) {
            for (FileTree child : fileTree.getChildren()) {
                TreeItem<String> childItem = convertFileTreeToTreeItem(child);
                treeItem.getChildren().add(childItem);
            }
        }

        return  treeItem;
    }

    private String addNote() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = Path.of(vaultPath);
        } else {
            String fullPath = getFullPath(selectedItem);
            Path selectedPath = Path.of(fullPath);

            if (selectedItem.isLeaf()) {
                targetPath = selectedPath.getParent();
            } else {
                targetPath = selectedPath;
            }
        }

        Path filePath = targetPath.resolve("Untitled.md");

        int index = 1;
        while (fileSystemService.exists(filePath)) {
            filePath = targetPath.resolve("Untitled " + index + ".md");
            index += 1;
        }

        fileSystemService.makeFile(filePath);
        return filePath.toString();
    }

    private String addDir() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = Path.of(vaultPath);
        } else {
            String fullPath = getFullPath(selectedItem);
            Path selectedPath = Path.of(fullPath);

            if (selectedItem.isLeaf()) {
                targetPath = selectedPath.getParent();
            } else {
                targetPath = selectedPath;
            }
        }

        Path dirPath = targetPath.resolve("Untitled");

        int index = 1;
        while (fileSystemService.exists(dirPath)) {
            dirPath = targetPath.resolve("Untitled " + index);
            index += 1;
        }

        fileSystemService.makeDir(dirPath);

        return dirPath.toString();
    }

    private void setDeleteShortCut() {
        fileTreeVBox.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                attachAccelerator(newS, new KeyCodeCombination(javafx.scene.input.KeyCode.DELETE), this::deleteFileTreeItem);
            }
        });
    }

    private void deleteFileTreeItem()
    {
        var sel = fileTreeView.getSelectionModel().getSelectedItem();

        String name = sel.getValue();
        var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        var btnDelete = new javafx.scene.control.ButtonType("Да давайте", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        var btnCancel = new javafx.scene.control.ButtonType("Нет, я хочу к мамочке",      javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnDelete, btnCancel);
        alert.setHeaderText("delete selected file or folder?");
        var owner = fileTreeVBox.getScene() != null ? fileTreeVBox.getScene().getWindow() : null;
        if (owner != null) alert.initOwner(owner);

        var res = alert.showAndWait();
//        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.OK) return;
        fileSystemService.delete(getFullPath(sel));
        refreshTree();
    }

    private void attachAccelerator(Scene scene, KeyCodeCombination keyComb, Runnable callback) {
        scene.getAccelerators().put(
                keyComb,
                () -> Platform.runLater(callback)
        );
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/file-tree-panel.fxml";
    }

}