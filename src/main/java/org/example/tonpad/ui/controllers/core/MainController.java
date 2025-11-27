package org.example.tonpad.ui.controllers.core;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.ui.controllers.*;
import org.example.tonpad.ui.controllers.file.FileTreeController;
import org.example.tonpad.ui.controllers.search.SearchInFileTreeController;
import org.example.tonpad.ui.controllers.search.SearchInFilesController;
import org.example.tonpad.ui.controllers.search.SearchInTextController;
import org.example.tonpad.ui.controllers.settings.SettingsController;
import org.example.tonpad.ui.service.ThemeService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MainController extends AbstractController {

    @FXML
    private VBox mainVBox;

    @FXML
    private AnchorPane settingsPane;

    @FXML
    private StackPane leftStackPane;

    @FXML
    private TabPane tabPane;

    @FXML
    private AnchorPane fileTreePane;

    @FXML
    private AnchorPane searchInFilesPane;

    @FXML
    private AnchorPane bookmarksPane;

    @FXML
    private AnchorPane searchInTextPane;

    @FXML
    private AnchorPane searchInFileTreePane;

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

    private final SearchInFilesController searchInFilesController;

    private final FileTreeController fileTreeController;

    private final SearchInFileTreeController searchInFileTreeController;

    private final SettingsController settingsController;

    private final ThemeService themeService;

    public void init(Stage stage) {
        setupControllers();
        leftStackPane.setManaged(false);
        setStage(stage, mainVBox, StageStyle.TRANSPARENT);
        themeService.apply(mainVBox.getScene(), ThemeService.Theme.LIGHT);
        titleBarController.init(stage, mainVBox);
        setupEventHandlers();
        setupGlobalClickHandler();
    }

    private void setupControllers() {
        fileTreeController.init(fileTreePane);
        searchInFilesController.init(searchInFilesPane);

        tabController.setTabPane(tabPane);
        try {
            tabController.init(Objects.requireNonNull(getClass().getResource("/Welcome.md")).toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        searchInTextController.setTabPane(tabPane);
        searchInTextController.setEditorMap(tabController.getEditorMap());
        searchInTextController.init(searchInTextPane);

        settingsController.init(settingsPane);

        searchInFileTreeController.init(searchInFileTreePane);
    }

    private void setupEventHandlers() {
        fileTreeController.setNoteOpenHandler(this::openNoteInEditor);
        fileTreeController.setNoteCloseHandler(this::closeNoteInEditor);
        fileTreeController.setNoteRenameHandler(this::renameNoteInEditor);
        searchInFilesController.setFileOpenHandler(this::openNoteInEditor);

        showFilesButton.setOnAction(event -> togglePane(
                leftStackPane, fileTreePane, showFilesButton, () -> {}, () -> {}
        ));

        showSearchButton.setOnAction(event -> togglePane(
                leftStackPane, searchInFilesPane, showSearchButton, () -> {}, () -> {}
        ));

        setSearchShortCut(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                () -> showPane(leftStackPane, searchInFilesPane, () -> {})
        );

        titleBarController.bindSettingsButton(e -> settingsController.toggle());

        setOpenShortcut(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                this::showSearchOverlay
        );

        setOpenShortcut(
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                this::showSearchInFileTreeOverlay
        );

        setEscShortcut(this::hideEverythingSearchRelated);
    }

    private void hideEverythingSearchRelated() {
        searchInTextController.hideSearchBar();
        if (fileTreePane.isVisible()) {
            searchInFileTreeController.hideSearchBar();
        }

        if (searchInTextPane.isVisible())
            searchInTextPane.setVisible(false);
        if (searchInFileTreePane.isVisible())
            searchInFileTreePane.setVisible(false);

        if (fileTreePane.isVisible()) {
            leftStackPane.setManaged(false);
            fileTreePane.setVisible(false);
            showFilesButton.getStyleClass().remove("toggled-icon-button");
        }
        if (searchInFilesPane.isVisible()) searchInFilesPane.setVisible(false);
        if (bookmarksPane.isVisible()) bookmarksPane.setVisible(false);
        settingsController.hide();
    }

    private void setOpenShortcut(KeyCodeCombination openKeyComb, Runnable show) {
        if (tabPane.getScene() != null) {
            attachAccelerator(tabPane.getScene(), openKeyComb, show);
            tabPane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) attachAccelerator(newS, openKeyComb, show);
            });
        }
    }

    private void setEscShortcut(Runnable hideAll) {
        KeyCodeCombination esc = new KeyCodeCombination(KeyCode.ESCAPE);
        if (tabPane.getScene() != null) {
            attachAccelerator(tabPane.getScene(), esc, hideAll);
            tabPane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) attachAccelerator(newS, esc, hideAll);
            });
        }
    }

    private void attachAccelerator(Scene scene, KeyCodeCombination keyComb, Runnable callback) {
        scene.getAccelerators().put(keyComb, () -> Platform.runLater(callback));
    }

    private void showSearchOverlay() {
        if (!searchInTextPane.isVisible()) {
            searchInTextPane.setVisible(true);
        }
        searchInTextController.showSearchBar();
    }

    private void hideSearchOverlay() {
        if (searchInTextPane.isVisible()) {
            searchInTextController.hideSearchBar();
            searchInTextPane.setVisible(false);
        }
    }

    private void showSearchInFileTreeOverlay() {
        if (!fileTreePane.isVisible()) {
            togglePane(
                    leftStackPane,
                    fileTreePane,
                    showFilesButton,
                    () -> {},
                    () -> {}
            );
        }

        if (!searchInFileTreePane.isVisible()) {
            searchInFileTreePane.setVisible(true);
        }

        searchInFileTreeController.showSearchBar();
    }

    private void hideSearchInFileTreeOverlay() {
        if (searchInFileTreePane.isVisible()) {
            searchInFileTreeController.hideSearchBar();
            searchInFileTreePane.setVisible(false);
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
            resetLeftToolButtons();
            button.getStyleClass().add("toggled-icon-button");
            stackPane.setManaged(true);
            anchorPane.setVisible(true);
            show.run();
        }
    }

    private void setSearchShortCut(KeyCodeCombination openKeyComb, Runnable show) {
        if (tabPane.getScene() != null) {
            attachAccelerator(tabPane.getScene(), openKeyComb, show);
            tabPane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    attachAccelerator(newS, openKeyComb, show);
                }
            });
        }
    }

    private void showPane(StackPane stackPane, AnchorPane anchorPane, Runnable show) {
        if (!anchorPane.isVisible()) {
            resetLeftToolButtons();

            for (Node child : stackPane.getChildren()) {
                child.setVisible(false);
            }
            resetLeftToolButtons();

            stackPane.setManaged(true);
            anchorPane.setVisible(true);
            show.run();
        }
    }

    private void openNoteInEditor(Path path, boolean openInCurrent) {
        tabController.openFileInTab(path, openInCurrent);
    }

    private void renameNoteInEditor(Path oldPath, Path newPath) {
        tabController.renameTab(oldPath, newPath);
    }

    private void closeNoteInEditor(Path path) {
        tabController.clearTab(path);
    }

    private void setupGlobalClickHandler() {
        mainVBox.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (searchInTextPane.isVisible() &&
                    !searchInTextPane.contains(event.getX(), event.getY())) {
                hideSearchOverlay();
            }

            if (fileTreePane.isVisible() && searchInFileTreePane.isVisible() &&
                    !fileTreePane.getBoundsInParent().contains(event.getX(), event.getY()) &&
                    !searchInFileTreePane.contains(event.getX(), event.getY())) {
                hideSearchInFileTreeOverlay();
            }
        });
    }

    private void resetLeftToolButtons() {
        showFilesButton.getStyleClass().remove("toggled-icon-button");
        showSearchButton.getStyleClass().remove("toggled-icon-button");
        showBookmarksButton.getStyleClass().remove("toggled-icon-button");
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/tonpad-ui.fxml";
    }
}
