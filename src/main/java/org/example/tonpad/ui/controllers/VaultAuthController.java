package org.example.tonpad.ui.controllers;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
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

    private Stage stage;

    public void showModal(Stage owner) {
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
            // сюда написать хрень

            // проверь на create vault (тут мб другую (set password), и open folder as vault что выдает плашку, 
            stage.close();
        });
        cancelButton.setOnAction(e -> {
            stage.close();
        });

        stage.showAndWait();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/vault-auth.fxml";
    }
}
