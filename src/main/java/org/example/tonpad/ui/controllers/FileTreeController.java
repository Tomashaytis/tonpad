package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

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
    private Button sortFilesButton;

    @FXML
    private Button expandFilesButton;

    @FXML
    public void initialize() {
        setupFileTree();
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(fileTreeVBox);

        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);
    }

    private void setupFileTree() {
        // Заполнение File Tree
        System.out.println("File tree initialized from FXML");
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

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/file-tree-panel.fxml";
    }

}