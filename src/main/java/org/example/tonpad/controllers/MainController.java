package org.example.tonpad.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;

public class MainController {
    @FXML
    private AnchorPane viewingPane;
    @FXML
    private AnchorPane notePane;
    @FXML
    private WebView noteWebView;
    @FXML
    private Button showFilesButton;
    @FXML
    private TabPane tabPane;

    private boolean isViewPaneVisible = false;

    @FXML
    public void initialize() {
        updatePanelVisibility();
        addNewTabButton();
        showFilesButton.setOnAction(event -> toggleLeftPanel());
    }

    @FXML
    private void toggleLeftPanel() {
        isViewPaneVisible = !isViewPaneVisible;
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        if (isViewPaneVisible) {
            AnchorPane.setLeftAnchor(noteWebView, 5.0);
            notePane.requestLayout();
            viewingPane.setVisible(true);
            viewingPane.setManaged(true);
            viewingPane.setPrefWidth(viewingPane.getMaxWidth());
        } else {
            AnchorPane.setLeftAnchor(noteWebView, 120.0);
            notePane.requestLayout();
            viewingPane.setVisible(false);
            viewingPane.setManaged(false);
            viewingPane.setPrefWidth(0);
        }
    }

    private void addNewTabButton() {
        Tab addTab = new Tab();
        addTab.setClosable(false);
        addTab.setDisable(true);

        Label plusLabel = new Label("+");
        plusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        addTab.setGraphic(plusLabel);

        tabPane.getTabs().add(addTab);

        plusLabel.setOnMouseClicked(event -> {
            createNewTab();
        });

        addTab.setOnSelectionChanged(event -> {
            if (addTab.isSelected()) {
                createNewTab();
                tabPane.getSelectionModel().selectPrevious();
            }
        });
    }

    private void createNewTab() {
        Tab newTab = new Tab("New tab" + (tabPane.getTabs().size()));

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        newTab.setContent(content);

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);
        tabPane.getSelectionModel().select(newTab);
    }
}
