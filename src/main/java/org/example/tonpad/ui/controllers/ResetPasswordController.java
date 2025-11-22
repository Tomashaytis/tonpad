package org.example.tonpad.ui.controllers;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ResetPasswordController extends AbstractController {

    @FXML
    private VBox resetPasswordRoot;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button setPasswordButton;

    @FXML
    private Button resetPasswordButton;

    private Stage stage;

    private final String EMPTY_PASSWORD_MESSAGE = "If you want to change password, input something.\nyour input is empty. passwort must not be empty. input something that is not empty.\nOr switch to the so called guest mode";
    private final String EMPTY_PASSWORD_HEADER = "password may not be empty";
    private final String INFO_TITLE = "Info";
    private final String GUEST_MODE_MESSAGE = "you switched to the so called guest mode. \nAll the notes you work with are not encrypted now and they're stored in open state.\nYou can not access encrypted notes.";
    private final String GUEST_MODE_HEADER = "Guest mode is active NOW";
    private final String PASSWORD_MODE_MESSAGE = "You just changed the password.\nAll the notes you could access before are encrypted now. You can access all your notes now as well.\nBe careful and don't forget your new password.";
    private final String PASSWORD_MODE_HEADER = "You work in encrypted vault.\nYour data is... probably safe";

    public void showModal(Stage ownerStage, Consumer<char[]> onSet, Runnable onReset) {
        stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(ownerStage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        stage.setTitle("reset pwd");

        Scene scene = new Scene(resetPasswordRoot);
        stage.setScene(scene);

        stage.showingProperty().addListener((obs, was, is) -> {
            if (is) passwordField.requestFocus();
        });

        setPasswordButton.setOnAction(e -> {
            String text = passwordField.getText();
            if(text == null || text.isEmpty()) {
                showAlert(EMPTY_PASSWORD_MESSAGE, Alert.AlertType.INFORMATION, EMPTY_PASSWORD_HEADER, INFO_TITLE);
                return;
            }
            char[] pwd = text.toCharArray();
            try {
                if(onSet != null) onSet.accept(pwd);
                showAlert(PASSWORD_MODE_MESSAGE, Alert.AlertType.INFORMATION, PASSWORD_MODE_HEADER, INFO_TITLE);
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });

        resetPasswordButton.setOnAction(e -> {
            showAlert(GUEST_MODE_MESSAGE, Alert.AlertType.INFORMATION, GUEST_MODE_HEADER, INFO_TITLE);
            if(onReset != null) onReset.run();
            stage.close();
        });

        // scene.setOnKeyPressed(k -> {
        //     switch (k.getCode()) {
        //         case ENTER -> setPasswordButton.fire();
        //         case ESCAPE -> stage.close();
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
        return "/ui/fxml/reset-password.fxml";
    }
}
