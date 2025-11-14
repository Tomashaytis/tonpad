package org.example.tonpad.ui.controllers;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
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
    @FXML private Button cancelButton;

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
                return;
            }
            char[] pwd = text.toCharArray();
            try {
                if (onSetPassword != null) onSetPassword.accept(pwd);
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });

        continueWithoutButton.setOnAction(e -> {
            if(onContinueWithout != null) onContinueWithout.run();
            stage.close();
        });

        cancelButton.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/vault-set-password.fxml";
    }
}
