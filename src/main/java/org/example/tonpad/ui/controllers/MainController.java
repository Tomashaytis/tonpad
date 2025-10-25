package org.example.tonpad.ui.controllers;

import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MainController extends AbstractController {

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
    private AnchorPane bookmarksPane;

    @FXML
    private AnchorPane searchInTextPane;

    @FXML
    private Button showFilesButton;

    @FXML
    private Button showSearchButton;

    @FXML
    private Button showBookmarksButton;

    @FXML
    private Button showSettingsButton;

    @FXML
    private Button enableReadingViewButton;

    @FXML
    private Button enablePlainViewButton;

    private final TitleBarController titleBarController;

    private final TabController tabController;

    private final SearchInTextController searchInTextController;

    private final FileTreeController fileTreeController;

    private final SearchInFileTreeController searchInFileTreeController;

    private final VaultPath vaultPath;

    public void init(Stage stage) {
        setupControllers();
        leftStackPane.setManaged(false);
        setStage(stage, mainVBox, StageStyle.TRANSPARENT);
        titleBarController.init(stage, mainVBox);
        setupEventHandlers();
    }

    private void setupControllers() {
        fileTreeController.init(fileTreePane, vaultPath.getVaultPath());

        tabController.setTabPane(tabPane);
        tabController.init("/test.md");

        searchInTextController.setTabPane(tabPane);
        searchInTextController.init(searchInTextPane);

        searchInFileTreeController.setTabPane(tabPane);
        searchInFileTreeController.init(searchInTextPane);
    }

    private void setupEventHandlers() {
        fileTreeController.setFileOpenHandler(this::openFileInEditor);

        showFilesButton.setOnAction(event -> togglePane(
                leftStackPane,
                fileTreePane,
                showFilesButton,
                () -> {},
                () -> {}
        ));

        showSearchButton.setOnAction(e -> this.showSearchInFileTreeOverlay());

        setSearchShortCut(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                new KeyCodeCombination(KeyCode.ESCAPE),
                this::showSearchOverlay,
                this::hideSearchOverlay
        );

        setSearchShortCut(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                new KeyCodeCombination(KeyCode.ESCAPE),
                this::showSearchInFileTreeOverlay,
                this::hideSearchInFileTreeOverlay
        );
    }

    private void showSearchOverlay()
    {
        if (!searchInTextPane.isVisible()) {
            searchInTextPane.setVisible(true);
        }
        searchInTextController.activateSearchBar();
        searchInFileTreeController.hideSearchBar();
    }

    private void hideSearchOverlay() {
        if (searchInTextPane.isVisible()) {
            searchInTextController.hideSearchBar();
            searchInTextPane.setVisible(false);
        }
    }

    private void showSearchInFileTreeOverlay()
    {
        if (!searchInTextPane.isVisible()) {
            searchInTextPane.setVisible(true);
        }
        if (!fileTreePane.isVisible())
        {
            togglePane(
                    leftStackPane,
                    fileTreePane,
                    showFilesButton,
                    () -> {},
                    () -> {}
            );
        }
        searchInFileTreeController.activateSearchBar();
        searchInTextController.hideSearchBar();
    }

    private void hideSearchInFileTreeOverlay() {
        if (searchInTextPane.isVisible()) {
            searchInFileTreeController.hideSearchBar();
            searchInTextPane.setVisible(false);
        }
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

    private void openFileInEditor(String path) {
        tabController.openFileInCurrentTab(path);
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/tonpad-ui.fxml";
    }
}
