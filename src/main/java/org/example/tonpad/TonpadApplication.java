package org.example.tonpad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tonpad.controllers.MainController;

import java.io.IOException;
import java.util.Objects;

public class TonpadApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/tonpad/fxml/tonpad-ui.fxml"));

        fxmlLoader.setController(new MainController());

        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
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
