package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
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
    private StackPane leftStackPane;

    @FXML
    private TabPane tabPane;

    @FXML
    private AnchorPane fileTreePane;

    @FXML
    private AnchorPane searchPane;

    @FXML
    private AnchorPane tagPane;

    @FXML
    private AnchorPane searchInTextPane;

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

    private final FileTreePanelController fileTreePanelController;

    private final TabController tabController;

    private final SearchTextController searchTextController;

    private final FileTreeController fileTreeController;

    public void initializeFileTreePanel(VBox fileTreeVBox) {
        fileTreePane.getChildren().add(fileTreeVBox);
        
        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);
    }

    public void initializeSearchInTextPanel(VBox fileTreeVBox) {
        searchInTextPane.getChildren().add(fileTreeVBox);

        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);
    }

    public void postInitialize() {
        setupEventHandlers();
        setupControllers();

        leftStackPane.setManaged(false);
    }

    private void setupControllers() {
        fileTreePanelController.setMainController(this);
        fileTreePanelController.init();

        tabController.setTabPane(tabPane);

        searchTextController.setMainController(this);
        searchTextController.init();
    }

    private void setupEventHandlers() {
        showFilesButton.setOnAction(event -> fileTreePanelController.toggleLeftPanel());
    }
}
