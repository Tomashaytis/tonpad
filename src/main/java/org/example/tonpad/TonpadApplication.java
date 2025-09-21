package org.example.tonpad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TonpadApplication extends Application {

    @Override
    public void start(Stage stage) {
        Label helloLabel = new Label("Hello World");
        helloLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: blue;");

        StackPane root = new StackPane();
        root.getChildren().add(helloLabel);

        Scene scene = new Scene(root, 300, 200);

        stage.setTitle("Tonpad - Hello World");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
