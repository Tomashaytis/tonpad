package org.example.tonpad.controllers;

import javafx.scene.layout.AnchorPane;
import org.springframework.stereotype.Component;

@Component
public class FileTreePanelController {
    private AnchorPane fileTreePane;

    private boolean isLeftPaneVisible = false;

    public void setFileTreePane(AnchorPane fileTreePane) {
        this.fileTreePane = fileTreePane;
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
