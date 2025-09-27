package org.example.tonpad.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.context.ApplicationContext;

@Getter
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

    private ApplicationContext springContext;

    private FileTreePanelController fileTreePanelController;

    private TabController tabController;

    private FileTreeController fileTreeController;

    @FXML
    public void initialize() {
        fileTreePanelController = new FileTreePanelController(fileTreePane, showFilesButton);
        tabController = new TabController(tabPane, springContext);

        setupControllers();
    }

    public void initializeFileTreePanel(VBox fileTreeVBox, FileTreeController fileTreeController) {
        this.fileTreeController = fileTreeController;

        fileTreePane.getChildren().add(fileTreeVBox);

        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);
    }

    private void setupControllers() {
        // Взаимодействие контроллеров
    }

    public TabController getTabManagerController() {
        return tabController;
    }

    public void setSpringContext(ApplicationContext springContext) {
        this.springContext = springContext;
        if (tabController != null) {
            tabController.setSpringContext(springContext);
        }
    }
}