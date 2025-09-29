package org.example.tonpad.ui.controllers;

import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileTreePanelController {
    @Setter
    private MainController mainController;

    private boolean isLeftPaneVisible = false;

    public void toggleLeftPanel() {
        isLeftPaneVisible = !isLeftPaneVisible;
        updatePanelVisibility();
    }

    public void init() {
        updatePanelVisibility();
    }

    private void showFilePanel() {
        for (Node child : mainController.getLeftStackPane().getChildren()) {
            child.setVisible(false);
        }

        for (Node child : mainController.getLeftToolsPane().getChildren()) {
            child.getStyleClass().remove("toggled-icon-button");
        }

        mainController.getShowFilesButton().getStyleClass().add("toggled-icon-button");
        mainController.getLeftStackPane().setManaged(true);
        mainController.getFileTreePane().setVisible(true);
    }

    private void hideFilePanel() {
        mainController.getShowFilesButton().getStyleClass().remove("toggled-icon-button");
        mainController.getFileTreePane().setVisible(false);
        mainController.getLeftStackPane().setManaged(false);
    }

    private void updatePanelVisibility() {
        if (isLeftPaneVisible) {
            showFilePanel();
        } else {
            hideFilePanel();
        }
    }
}
