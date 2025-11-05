package org.example.tonpad.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.example.tonpad.core.files.RecentVaultService;
import org.example.tonpad.core.service.VaultService;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class QuickStartDialogController extends AbstractController {

    @FXML
    private HBox quickStartDialogMainHBox;

    @FXML
    private HBox headerHBox;

    @FXML
    private Button closeDialogButton;

    @FXML
    private Button collapseDialogButton;

    @FXML
    private Button createNewVaultButton;

    @FXML
    private Button openFolderAsVaultButton;

    @FXML
    private Button settingsButton;

    @FXML
    private ListView<String> recentVaultsListView;

    private final RecentVaultService recentVaultService;

    private final VaultService vaultService;

    private final ObservableList<String> recentVaults = FXCollections.observableArrayList();

    private final VaultPath vaultPath;

    private Stage stage;

    private double xOffset = 0;

    private double yOffset = 0;

    @Setter
    private Consumer<String> createVaultHandler;

    public void init() {
        this.stage = new Stage();
        setupEventHandlers();
        setStage(stage, quickStartDialogMainHBox, StageStyle.TRANSPARENT);
        setupDragHandlers();
        recentVaults.setAll(recentVaultService.load());
        recentVaultService.bindAutoSave(recentVaults);
        setupRecentVaultsList();
    }

    public void hide() {
        stage.hide();
    }

    public void show() {
        stage.show();
    }

    private void setupRecentVaultsList() {
        recentVaultsListView.setPlaceholder(new Label("no recent vaults"));
        recentVaultsListView.setItems(recentVaults);
        recentVaultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if(empty || path == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(path);
            }
        });

        recentVaultsListView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) openRecentSelected();
        });
        recentVaultsListView.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                openRecentSelected();
            }
        });
    }

    private void openRecentSelected() {
        String path = recentVaultsListView.getSelectionModel().getSelectedItem();
        if(path == null || path.isBlank()) return; 
        File dir = new File(path);
        if(!dir.exists() || !dir.isDirectory()) {
            if(confirmToRemoveBrokenRecent(path)) recentVaults.remove(path);
            return;
        }

        vaultService.checkVaultInitialization(Path.of(path));
        vaultPath.setVaultPath(path);

        if(createVaultHandler != null) {
            recentVaultService.setFirstRecent(recentVaults, path);
            createVaultHandler.accept(path);
            hide();
        }
    }

    private void setupEventHandlers() {
        closeDialogButton.setOnAction(e -> stage.close());
        collapseDialogButton.setOnAction(e -> stage.setIconified(true));
        createNewVaultButton.setOnAction(e -> selectEmptyFolder());
        openFolderAsVaultButton.setOnAction(e -> selectFolder());
    }

    private void setupDragHandlers() {
        headerHBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        headerHBox.setOnMouseDragged(event -> {
            if (stage != null) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    private void selectEmptyFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Empty Directory for New Vault");

        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            if (!isEmptyDirectory(selectedDirectory)) {
                boolean proceed = showNonEmptyDirectoryWarning(selectedDirectory);
                if (!proceed) {
                    return;
                }
            }

            vaultPath.setVaultPath(selectedDirectory.getAbsolutePath());
            vaultService.initVault(selectedDirectory.toPath());
            createVaultHandler.accept(vaultPath.getVaultPath());
            recentVaultService.setFirstRecent(recentVaults, vaultPath.getVaultPath());
            hide();
        }
    }

    private boolean isEmptyDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return true;
        }

        String[] contents = directory.list();
        return contents == null || contents.length == 0;
    }

    private boolean showNonEmptyDirectoryWarning(File directory) {
        // Кастомный диалог или использование Alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Directory Not Empty");
        alert.setHeaderText("The selected directory is not empty");
        alert.setContentText("Directory '" + directory.getName() + "' contains files. " +
                "Using a non-empty directory as a vault might mix your notes with existing files.\n\n" +
                "Do you want to proceed anyway?");

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void selectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Vault Directory");

        File selectedDirectory = directoryChooser.showDialog(stage);
        if(selectedDirectory == null) {
            return;
        }

        String path = selectedDirectory.getAbsolutePath();
        vaultService.checkVaultInitialization(Path.of(path));

        vaultPath.setVaultPath(path);
        recentVaultService.setFirstRecent(recentVaults, path);
        if(createVaultHandler != null) {
            System.out.println(vaultPath.getVaultPath());
            createVaultHandler.accept(vaultPath.getVaultPath());
            hide();
        }
//        String vaultPath = selectedDirectory.getAbsolutePath();
    }

    private void ensureEmptyDirectory(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
    
    private boolean confirmToRemoveBrokenRecent(String path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("path not found");
        alert.setHeaderText("Vault path is not accessible");
        alert.setContentText("Remove from Recent?\n\n" + path);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/quick-start-dialog.fxml";
    }
}
