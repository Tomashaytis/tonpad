package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;
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
    }

    public void close() {
        stage.close();
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
            createVaultHandler.accept(vaultPath.getVaultPath());
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
        vaultPath.setVaultPath(selectedDirectory.getAbsolutePath());
//        String vaultPath = selectedDirectory.getAbsolutePath();
        System.out.println(vaultPath.getVaultPath());
        createVaultHandler.accept(vaultPath.getVaultPath());
    }

    private void ensureEmptyDirectory(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/quick-start-dialog.fxml";
    }
}
