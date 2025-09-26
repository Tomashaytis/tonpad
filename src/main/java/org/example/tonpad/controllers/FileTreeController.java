package org.example.tonpad.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import lombok.Getter;

@Getter
public class FileTreeController {

    @FXML
    private TreeView<String> fileTreeView;

    @FXML
    private HBox fileTreeToolsHBox;

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