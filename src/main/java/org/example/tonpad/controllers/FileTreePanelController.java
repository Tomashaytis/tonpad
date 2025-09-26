package org.example.tonpad.controllers;

import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class FileTreePanelController {
    private final AnchorPane fileTreePane;

    private boolean isLeftPaneVisible = false;

    public FileTreePanelController(AnchorPane fileTreePane, Button showFilesButton) {
        this.fileTreePane = fileTreePane;
        showFilesButton.setOnAction(event -> toggleLeftPanel());
        updatePanelVisibility();
    }

    public void toggleLeftPanel() {
        isLeftPaneVisible = !isLeftPaneVisible;
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        if (isLeftPaneVisible) {
            showFilePanel();

        } else {
            hideFilePanel();
        }
    }

    private void showFilePanel() {
        fileTreePane.setPrefWidth(fileTreePane.getMaxWidth());
        fileTreePane.setVisible(true);
        fileTreePane.setManaged(true);
    }

    private void hideFilePanel() {
        fileTreePane.setVisible(false);
        fileTreePane.setManaged(false);
        fileTreePane.setPrefWidth(0);
    }

    public void setLeftPaneVisible(boolean visible) {
        this.isLeftPaneVisible = visible;
        updatePanelVisibility();
    }
}
