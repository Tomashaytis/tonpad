package org.example.tonpad.controllers;

import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
public class LeftPanelController {
    private final AnchorPane viewingPane;
    private boolean isLeftPaneVisible = false;

    public LeftPanelController(AnchorPane viewingPane, Button showFilesButton) {
        this.viewingPane = viewingPane;
        showFilesButton.setOnAction(event -> toggleLeftPanel());
        updatePanelVisibility();
    }

    public void toggleLeftPanel() {
        isLeftPaneVisible = !isLeftPaneVisible;
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        if (isLeftPaneVisible) {
            viewingPane.setVisible(true);
            viewingPane.setManaged(true);
            viewingPane.setPrefWidth(viewingPane.getMaxWidth());
        } else {
            viewingPane.setVisible(false);
            viewingPane.setManaged(false);
            viewingPane.setPrefWidth(0);
        }
    }

    public boolean isLeftPaneVisible() {
        return isLeftPaneVisible;
    }

    public void setLeftPaneVisible(boolean visible) {
        this.isLeftPaneVisible = visible;
        updatePanelVisibility();
    }
}
