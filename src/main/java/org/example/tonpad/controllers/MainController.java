package org.example.tonpad.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import org.springframework.context.ApplicationContext;

public class MainController {
    @FXML
    private AnchorPane viewingPane;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button showFilesButton;

    private ApplicationContext springContext;
    @Getter
    private LeftPanelController leftPanelController;
    @Getter
    private TabController tabController;

    @FXML
    public void initialize() {
        leftPanelController = new LeftPanelController(viewingPane, showFilesButton);
        tabController = new TabController(tabPane, springContext);

        setupControllers();
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