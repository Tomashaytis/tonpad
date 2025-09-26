package org.example.tonpad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.tonpad.controllers.FileTreeController;
import org.example.tonpad.controllers.MainController;

import java.io.IOException;
import java.util.Objects;

public class TonpadApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(
                getClass().getResource("/org/example/tonpad/fxml/tonpad-ui.fxml")
        );
        Parent root = mainLoader.load();
        MainController mainController = mainLoader.getController();

        FXMLLoader fileTreeLoader = new FXMLLoader(
                getClass().getResource("/org/example/tonpad/fxml/file-tree-panel.fxml")
        );
        VBox fileTreeVBox = fileTreeLoader.load();
        FileTreeController fileTreeController = fileTreeLoader.getController();

        mainController.initializeFileTreePanel(fileTreeVBox, fileTreeController);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/org/example/tonpad/css/styles.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
