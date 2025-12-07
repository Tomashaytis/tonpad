package org.example.tonpad.ui.controllers.security;

import java.util.Arrays;
import java.util.function.Consumer;

import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VaultSetPasswordController extends AbstractController {
    @FXML private HBox vaultSetPasswordRoot;
    @FXML private PasswordField passwordField;
    @FXML private Button setPasswordButton;
    @FXML private Button continueWithoutButton;
    // @FXML private Button cancelButton;

    private final String CREATE_NOT_ENCTYPTED_MESSAGE = "You are gonna work with NO password.\nThe vault is not encrypted.\nAll your notes you work with will NOT be encrypted.\nIf you wanna protect yourself,\nyou can start working with password (look for it in settings)";
    private final String CREATE_NOT_ENCTYPTED_HEADER = "You created vault with NO password. Vault is not encrypted.";
    private final String INFO_TITLE = "Info";
    private final String CREATE_ENCTYPTED_MESSAGE = "You are gonna work with password.\nThe vault is ENCRYPTED.\nAll your notes you work with will be ENCRYPTED.\nYou can work with notes only working in this app,\nor you can decrypt all notes and work with not encrypted vault by\nswitching to the Guest mode in settings. \nThen all your notes will be immediately decrypted and will be stored in open state.";
    private final String CREATE_ENCTYPTED_HEADER = "You created vault with password. Vault is ENCRYPTED. All your notes are ENCRYPTED";
    private final String EMPTY_PASSWORD_MESSAGE = "If you want to work with password, input something.\nyour input is empty. passwort must not be empty. input something that is not empty\nOr cdreate not encrypted one";
    private final String EMPTY_PASSWORD_HEADER = "password may not be empty";

    private final String GUEST_MODE_MESSAGE = "you are going to work in so called guest mode. \nAll the notes you work with won't be encrypted\n and u can not access encrypted notes as well. good luck.";
    private final String GUEST_MODE_HEADER = "Guest mode is active NOW";
    private final String PASSWORD_MODE_MESSAGE = "you are going to work with encrypted vault. \nAll the notes you work with will be encrypted immediately. \nYou can access not encrypted files, \nbut if you work with them they will be encrypted. \nyou can not access notes encrypted with password different from yours.";
    private final String PASSWORD_MODE_HEADER = "You work in encrypted vault.\nYour data is... probably safe";

    private Stage stage;

    public void showModal(Stage stageOwner, Consumer<char[]> onSetPassword, Runnable onContinueWithout) {
        stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(stageOwner);
        stage.initModality(Modality.WINDOW_MODAL);


        var scene = new Scene(vaultSetPasswordRoot);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("set vault password");
        stage.showingProperty().addListener((obs, was, is) -> {
            if(is) passwordField.requestFocus();
        });

        setPasswordButton.setOnAction(e -> {
            final String text = passwordField.getText();
            if(text == null || text.isEmpty()) {
                showAlert(EMPTY_PASSWORD_MESSAGE, Alert.AlertType.INFORMATION, EMPTY_PASSWORD_HEADER, INFO_TITLE);
                return;
            }
            char[] pwd = text.toCharArray();
            try {
                if (onSetPassword != null) {
                    showAlert(CREATE_ENCTYPTED_MESSAGE, Alert.AlertType.INFORMATION, CREATE_ENCTYPTED_HEADER, INFO_TITLE);
                    onSetPassword.accept(pwd);
                }
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });

        continueWithoutButton.setOnAction(e -> {
            showAlert(CREATE_NOT_ENCTYPTED_MESSAGE, Alert.AlertType.INFORMATION, CREATE_NOT_ENCTYPTED_HEADER, INFO_TITLE);
            if(onContinueWithout != null) onContinueWithout.run();
            stage.close();
        });

        // cancelButton.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private void showAlert(String text, Alert.AlertType type, String header, String title) {
        Alert alert = new Alert(type);
        alert.setResizable(true);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(400);
        alert.getDialogPane().setContent(label);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        alert.showAndWait();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/security/vault-set-password.fxml";
    }
}
