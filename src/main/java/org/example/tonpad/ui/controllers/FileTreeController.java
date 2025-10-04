package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

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
    private Button sortFilesButton;

    @FXML
    private Button expandFilesButton;
    
    private final FileSystemService fileSystemService;

    public void init(AnchorPane parent) {
        parent.getChildren().add(fileTreeVBox);

        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);

        setupFileTree();
    }

    public void refreshTree() {
        // Обновление дерева файлов
    }

    public void expandAll() {
        // Развернуть все узлы
    }

    public void collapseAll() {
        // Свернуть все узлы
    }

    private void setupFileTree() {
        FileTree fileTree = fileSystemService.getFileTree("D:\\Projects\\Others\\Java\\test");

        TreeItem<String> rootItem = convertFileTreeToTreeItem(fileTree);

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