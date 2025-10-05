package org.example.tonpad.ui.controllers;

import jakarta.annotation.PostConstruct;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileSystemServiceImpl;
import org.example.tonpad.core.files.directory.DirectoryServiceImpl;
import org.springframework.stereotype.Component;

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
    private Button sortFilesButton;

    @FXML
    private Button expandFilesButton;

    private final FileSystemService fileSystemService;

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

        fileSystemService.makeDir("./test");
        fileSystemService.makeDir("./test/test2");
        fileSystemService.makeFile("./test/test2/testfile.md");

    }

    private void setupFileTree() {
        // Заполнение File Tree
        System.out.println("wwwwwwwwwwwwww");

        addCollectionButton.setOnAction(e -> searchInFileTree("e"));
        System.out.println("File tree initialized from FXML");
    }

    public void refreshTree() {
        // Обновление дерева файлов
    }

    public void searchInFileTree(String strToSearch)
    {
        fileTreeView.getSelectionModel().selectionModeProperty().addListener();
        System.out.println("wwwwwwwwwwwwww");
        var res = fileSystemService.findByNameContains("./test", "t");
        System.out.println(res.toString());
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