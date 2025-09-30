package org.example.tonpad.ui.controllers;

import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MainController {

    static String FXML_SOURCE = "/ui/fxml/tonpad-ui.fxml";

    @Getter
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

    private final TabController tabController;

    private final SearchInTextController searchInTextController;

    private final FileTreeController fileTreeController;

    public void postInitialize() {
        setupEventHandlers();
        setupControllers();

        leftStackPane.setManaged(false);
    }

    private void setupControllers() {
        fileTreeController.init(fileTreePane);

        tabController.setTabPane(tabPane);
        tabController.init();

        searchInTextController.setTabPane(tabPane);
        searchInTextController.init(searchInTextPane);
    }

    private void setupEventHandlers() {
        showFilesButton.setOnAction(event -> togglePane(
                leftStackPane,
                fileTreePane,
                showFilesButton,
                () -> {},
                () -> {}
        ));

        setSearchShortCut(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                new KeyCodeCombination(KeyCode.ESCAPE),
                () -> showPane(leftStackPane, searchInTextPane, searchInTextController::showSearchBar),
                () -> hidePane(leftStackPane, searchInTextPane, searchInTextController::hideSearchBar)
        );
    }

    private void togglePane(StackPane stackPane, AnchorPane anchorPane, Button button, Runnable show, Runnable hide) {
        if (anchorPane.isVisible()) {
            button.getStyleClass().remove("toggled-icon-button");
            anchorPane.setVisible(false);
            stackPane.setManaged(false);

            hide.run();
        } else {
            for (Node child : stackPane.getChildren()) {
                child.setVisible(false);
            }

            for (Node child : stackPane.getChildren()) {
                child.getStyleClass().remove("toggled-icon-button");
            }

            button.getStyleClass().add("toggled-icon-button");
            stackPane.setManaged(true);
            anchorPane.setVisible(true);

            show.run();
        }
    }

    private void showPane(StackPane stackPane, AnchorPane anchorPane, Runnable show) {
        if (!anchorPane.isVisible()) {
            for (Node child : stackPane.getChildren()) {
                child.setVisible(false);
            }

            for (Node child : stackPane.getChildren()) {
                child.getStyleClass().remove("toggled-icon-button");
            }

            stackPane.setManaged(true);
            anchorPane.setVisible(true);

            show.run();
        }
    }

    private void hidePane(StackPane stackPane, AnchorPane anchorPane, Runnable hide) {
        if (anchorPane.isVisible()) {
            anchorPane.setVisible(false);
            stackPane.setManaged(false);

            hide.run();
        }
    }

    private void setSearchShortCut(KeyCodeCombination openKeyComb, KeyCodeCombination closeKeyComb, Runnable show, Runnable hide) {
        if (tabPane.getScene() != null) {
            attachAccelerator(tabPane.getScene(), openKeyComb, show);
            attachAccelerator(tabPane.getScene(), closeKeyComb, hide);
            tabPane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    attachAccelerator(newS, openKeyComb, show);
                    attachAccelerator(newS, closeKeyComb, hide);
                }
            });
        }
    }

    private void attachAccelerator(Scene scene, KeyCodeCombination keyComb, Runnable callback) {
        scene.getAccelerators().put(
                keyComb,
                () -> Platform.runLater(callback)
        );
    }

    @PostConstruct
    public void postConstruct() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource(FXML_SOURCE))
        );
        loader.setControllerFactory(i -> this);
        loader.load();
    }

}
