package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
public class MainController {
    @FXML
    private VBox mainVBox;

    @FXML
    private HBox mainHBox;

    @FXML
    private VBox leftToolsPane;

    @FXML
    private VBox rightToolsPane;

    @FXML
    private StackPane noteStackPane;

    @FXML
    private TabPane tabPane;

    @FXML
    private AnchorPane fileTreePane;

    @FXML
    private AnchorPane searchPane;

    @FXML
    private AnchorPane tagPane;

    @FXML
    private Button showFilesButton;

    @FXML
    private Button showSearchButton;

    @FXML
    private Button showTagsButton;

    @FXML
    private Button showSettingsButton;

    @FXML
    private Button enableReadingViewButton;

    @FXML
    private Button enablePlainViewButton;

    @Autowired
    private SearchTextController searchBarIncludeController;

    @Autowired
    private FileTreePanelController fileTreePanelController;

    @Autowired
    private TabController tabController;

    @Autowired
    private SearchTextController searchTextController;

    @Autowired
    private FileTreeController fileTreeController;

    @FXML
    public void initialize() {
    }

    public void initializeFileTreePanel(VBox fileTreeVBox, FileTreeController fileTreeController) {
        this.fileTreeController = fileTreeController;
        
        fileTreePane.getChildren().add(fileTreeVBox);
        
        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);
    }

    public void postInitialize()
    {
        setupEventHandlers();
        setupControllers();

        searchBarIncludeController.setTabPane(tabPane);
        searchBarIncludeController.init();

    }

    private void setupControllers() {
        fileTreePanelController.setFileTreePane(fileTreePane);
        tabController.setTabPane(tabPane);
    }

    private void setupEventHandlers() {
        showFilesButton.setOnAction(event -> fileTreePanelController.toggleLeftPanel());
    }

    public TabController getTabManagerController() {
        return tabController;
    }
}
