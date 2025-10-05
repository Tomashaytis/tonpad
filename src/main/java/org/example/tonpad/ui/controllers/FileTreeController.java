package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.ui.extentions.FileTreeItem;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class FileTreeController extends AbstractController {

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

    private final FileSystemService fileSystemService;

    private TreeItem<String> rootItem;

    @FXML
    public void initialize() {
        setupFileTree();
    }

    private TreeItem<String> selectedItem;

    private String vaultPath;

    private boolean isCollapsed = true;

    @Setter
    private Consumer<String> fileOpenHandler;

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

        fileTreeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                onOpenFile();
            }
        });

        addNoteButton.setOnAction(e -> {
            addNote();
            onOpenFile();
            refreshTree();
        });

        addDirectoryButton.setOnAction(e -> {
            addDir();
            refreshTree();
        });
    }

    private void onOpenFile() {
        if (selectedItem != null && selectedItem.isLeaf()) {
            String filePath = getFullPath(selectedItem);
            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath);
            }
        }
    }

    private void refreshTree() {
        saveAllExpandedStates();

        FileTree fileTree = fileSystemService.getFileTree(vaultPath);
        TreeItem<String> newRoot = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(newRoot);
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
        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current != null && !current.getValue().isEmpty()) {
            pathSegments.addFirst(current.getValue());
            current = current.getParent();
        }

        return String.join("/", pathSegments);
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

    private void onFileSelected(TreeItem<String> selectedItem) {
        this.selectedItem = selectedItem;
    }

    private void onRightClick(ContextMenuEvent event) {

    }

    public void searchInFileTree(String strToSearch)
    {
//        fileTreeView.getSelectionModel().selectionModeProperty().addListener();
        System.out.println("wwwwwwwwwwwwww");
        var res = fileSystemService.findByNameContains("./test", "t");
        System.out.println(res.toString());
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

    private void addNote() {
        Path path = selectedItem == null ? Path.of(vaultPath) : Path.of(getRelativePath(selectedItem)).getParent();
        Path filePath = path.resolve("Untitled.md");

        int index = 1;
        while (fileSystemService.exists(filePath)) {
            filePath = path.resolve("Untitled " + index + ".md");
            index += 1;
        }

        fileSystemService.makeFile(filePath);
    }

    private void addDir() {
        Path path = selectedItem == null ? Path.of(vaultPath) : Path.of(getRelativePath(selectedItem)).getParent();
        Path filePath = path.resolve("Untitled");

        int index = 1;
        while (fileSystemService.exists(filePath)) {
            filePath = path.resolve("Untitled " + index);
            index += 1;
        }

        fileSystemService.makeDir(filePath);
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/file-tree-panel.fxml";
    }

}