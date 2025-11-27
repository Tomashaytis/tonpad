package org.example.tonpad.ui.controllers;

import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

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
public class VaultAuthController extends AbstractController {

    @FXML
    private HBox vaultAuthRoot;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button enterWithoutButton;

    private final String EMPTY_PASSWORD_MESSAGE = "If you want to work with password, input something.\nyour input is empty. passwort must not be empty. input something that is not empty.\nOr enter in so called guest mode";
    private final String INFO_TITLE = "Info";
    private final String EMPTY_PASSWORD_HEADER = "password may not be empty";
    private final String GUEST_MODE_MESSAGE = "you are going to work in so called guest mode. \nAll the notes you work with won't be encrypted\n and u can not access encrypted notes as well. good luck.";
    private final String GUEST_MODE_HEADER = "Guest mode is active NOW";
    private final String PASSWORD_MODE_MESSAGE = "you are going to work with encrypted vault. \nAll the notes you work with will be encrypted immediately. \nYou can access not encrypted files, \nbut if you work with them they will be encrypted. \nyou can not access notes encrypted with password different from yours.";
    private final String PASSWORD_MODE_HEADER = "You work in encrypted vault.\nYour data is... probably safe";

    private Stage stage;

    public void showModal(Stage owner, Consumer<char[]> onPassword, Runnable onWithoutPwd, Runnable onCancel) {
        stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        var scene = new Scene(vaultAuthRoot);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("input vault password");
        stage.showingProperty().addListener((obs, was, is) -> {
            if(is) passwordField.requestFocus();
        });
        
        okButton.setOnAction(e -> {
            final String text = passwordField.getText();
            if(text == null || text.isEmpty()) 
            {
                // if(onCancel != null) //onCancel.run();
                showAlert(EMPTY_PASSWORD_MESSAGE, Alert.AlertType.INFORMATION, EMPTY_PASSWORD_HEADER, INFO_TITLE);
                //stage.close();
                return;
            }
            char[] pwd = text.toCharArray();
            try {
                if(onPassword != null) {
                    showAlert(PASSWORD_MODE_MESSAGE, Alert.AlertType.INFORMATION, PASSWORD_MODE_HEADER, INFO_TITLE);
                    onPassword.accept(pwd);
                }
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });
        enterWithoutButton.setOnAction(e -> {
            showAlert(GUEST_MODE_MESSAGE, Alert.AlertType.INFORMATION, GUEST_MODE_HEADER, INFO_TITLE);
            if(onWithoutPwd != null) onWithoutPwd.run();
            stage.close();
        });
        // cancelButton.setOnAction(e -> {
        //     if(onCancel != null) onCancel.run();
        //     stage.close();
        // });

        // scene.setOnKeyPressed(k -> {
        //     switch ((k.getCode())) {
        //         case ENTER -> okButton.fire();
        //         case ESCAPE -> cancelButton.fire();
        //         default -> {}
        //     }
        // });

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
        return "/ui/fxml/vault-auth.fxml";
    }
}
