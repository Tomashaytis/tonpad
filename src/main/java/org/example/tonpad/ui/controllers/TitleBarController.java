package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class TitleBarController extends AbstractController {

    @FXML
    private HBox titleBarMainHBox;

    @FXML
    private Button closeAppButton;

    @FXML
    private Button resizeAppButton;

    @FXML
    private Button collapseAppButton;

    private Stage stage;

    private double xOffset = 0;

    private double yOffset = 0;

    public void init(Stage stage, VBox mainVBox) {
        this.stage = stage;
        setupEventHandlers();
        mainVBox.getChildren().addFirst(titleBarMainHBox);
        setupDragHandlers();
    }

    private void setupEventHandlers() {
        closeAppButton.setOnAction(e -> stage.close());
        collapseAppButton.setOnAction(e -> stage.setIconified(true));
        resizeAppButton.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
    }


    private void setupDragHandlers() {
        titleBarMainHBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBarMainHBox.setOnMouseDragged(event -> {
            if (stage != null) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/tonpad-title-bar.fxml";
    }
}
