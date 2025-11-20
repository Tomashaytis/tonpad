package org.example.tonpad.ui.controllers;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
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
            if(text == null || text.isEmpty()) return;
            char[] pwd = text.toCharArray();
            try {
                if(onSet != null) onSet.accept(pwd);
            }
            finally {
                Arrays.fill(pwd, '\0');
                passwordField.clear();
            }
            stage.close();
        });

        resetPasswordButton.setOnAction(e -> {
            if(onReset != null) onReset.run();
            stage.close();
        });

        scene.setOnKeyPressed(k -> {
            switch (k.getCode()) {
                case ENTER -> setPasswordButton.fire();
                case ESCAPE -> stage.close();
                default -> {}
            }
        });

        stage.showAndWait();
    }
    
    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/reset-password.fxml";
    }
}
