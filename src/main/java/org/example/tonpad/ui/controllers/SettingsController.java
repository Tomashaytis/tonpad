package org.example.tonpad.ui.controllers;

import java.util.function.Consumer;

import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.ui.service.ThemeService;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;

@Component
@RequiredArgsConstructor
public class SettingsController extends AbstractController {

    @FXML
    private VBox settingsPanel;

    @FXML
    private Button closeButton;

    @FXML
    private ChoiceBox<String> themeChoice;

    private final ThemeService themeService;

    private static final double OFFSET = 12.0;

    private AnchorPane host;
    private boolean isShowing = false;

    public void init(AnchorPane hostPane) {
        this.host = hostPane;
        ensureLoaded();

        settingsPanel.setManaged(false);
        settingsPanel.setVisible(false);
        settingsPanel.setTranslateX(offscreenX());

        closeButton.setOnAction(e -> hide());

        setupThemeChoice();
    }

    private void setupThemeChoice() {
        themeChoice.getItems().setAll("Light", "Dark");
        if(themeChoice.getValue() == null) themeChoice.setValue("Light");
        themeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if(newV == null) return;
            Scene scene = settingsPanel.getScene();
            if(scene == null) return;
            if("Light".equalsIgnoreCase(newV)) {
                themeService.apply(scene, ThemeService.Theme.LIGHT);
            }
            else themeService.apply(scene, ThemeService.Theme.DARK);
        });

        Scene scene = settingsPanel.getScene();
        if(scene != null) themeService.apply(scene, ThemeService.Theme.LIGHT);
    }

    public void toggle() { if (isShowing) hide(); else show(); }

    public void show() {
        if (isShowing) return;
        ensureLoaded();
        settingsPanel.setTranslateX(0);
        settingsPanel.setVisible(true);
        settingsPanel.setManaged(true);
        isShowing = true;
    }

    public void hide() {
        if (!isShowing) return;
        ensureLoaded();
        settingsPanel.setTranslateX(offscreenX());
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        isShowing = false;
    }

    private double offscreenX() {
        return getPanelWidth() + OFFSET;
    }

    private double getPanelWidth() {
        double w = settingsPanel.getWidth();
        if (w <= 0) w = settingsPanel.getPrefWidth() > 0 ? settingsPanel.getPrefWidth() : 360.0;
        return w;
    }

    private void ensureLoaded() {
        if (settingsPanel == null) {
            throw new CustomIOException("settings panel is not loaded (FXML)");
        }
        if (settingsPanel.getParent() == null) {
            AnchorPane.setTopAnchor(settingsPanel, 0.0);
            AnchorPane.setRightAnchor(settingsPanel, 0.0);
            AnchorPane.setBottomAnchor(settingsPanel, 0.0);
            host.getChildren().add(settingsPanel);
        }
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/settings-panel.fxml";
    }
}
