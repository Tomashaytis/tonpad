package org.example.tonpad.ui.controllers;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

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
                if(onCancel != null) onCancel.run();
                stage.close();
                return;
            }
            char[] pwd = text.toCharArray();
            try {
                if(onPassword != null) onPassword.accept(pwd);
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });
        enterWithoutButton.setOnAction(e -> {
            if(onWithoutPwd != null) onWithoutPwd.run();
            stage.close();
        });
        cancelButton.setOnAction(e -> {
            if(onCancel != null) onCancel.run();
            stage.close();
        });

        scene.setOnKeyPressed(k -> {
            switch ((k.getCode())) {
                case ENTER -> okButton.fire();
                case ESCAPE -> cancelButton.fire();
                default -> {}
            }
        });

        stage.showAndWait();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/vault-auth.fxml";
    }
}
