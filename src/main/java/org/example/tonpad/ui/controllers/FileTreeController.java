package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FileTreeController {

    @FXML
    private TreeView<String> fileTreeView;

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
}