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
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
@RequiredArgsConstructor
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

    private String vaultPath;

    private boolean isCollapsed = true;

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
            String path = getItemPath(item);
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
            String path = getItemPath(item);
            Boolean wasExpanded = expandedState.get(path);
            if (wasExpanded != null) {
                item.setExpanded(wasExpanded);
            }

            for (TreeItem<String> child : item.getChildren()) {
                restoreExpandedStateRecursive(child);
            }
        }
    }
    private String getItemPath(TreeItem<String> item) {
        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current != null && !current.getValue().isEmpty()) {
            pathSegments.addFirst(current.getValue());
            current = current.getParent();
        }

        return String.join("/", pathSegments);
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

    private void saveExpandedState(TreeItem<String> item) {
        if (item != null) {
            item.setExpanded(item.isExpanded());
            for (TreeItem<String> child : item.getChildren()) {
                saveExpandedState(child);
            }
        }
    }

    private void restoreExpandedState(TreeItem<String> item) {
        if (item != null) {
            for (TreeItem<String> child : item.getChildren()) {
                restoreExpandedState(child);
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

    private void onFileSelected(TreeItem<String> newValue) {
    }

    private void onRightClick(ContextMenuEvent event) {
    }

    private TreeItem<String> convertFileTreeToTreeItem(FileTree fileTree) {
        Path path = fileTree.getPath();
        String fileName = path.getFileName() != null ? path.getFileName().toString() : path.toString();

        TreeItem<String> treeItem = new TreeItem<>(fileName);

        treeItem.setValue(fileName);

        if (fileTree.getChildren() != null && !fileTree.getChildren().isEmpty()) {
            for (FileTree child : fileTree.getChildren()) {
                TreeItem<String> childItem = convertFileTreeToTreeItem(child);
                treeItem.getChildren().add(childItem);
            }
        }

        return  treeItem;
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/file-tree-panel.fxml";
    }

}