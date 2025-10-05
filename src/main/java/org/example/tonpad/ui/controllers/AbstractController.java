package org.example.tonpad.ui.controllers;

import jakarta.annotation.PostConstruct;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;


public abstract class AbstractController {

    protected abstract String getFxmlSource();

    @PostConstruct
    private void postConstruct() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource(getFxmlSource()))
        );
        loader.setControllerFactory(i -> this);
        loader.load();
    }

    protected void setStage(Stage stage, Parent root) {
        Scene scene = new Scene(root);

        Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/ui/icons/common/Tonpad256.png")
        ));
        stage.getIcons().add(icon);

        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/ui/css/base.css")).toExternalForm()
        );

        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();

        Rectangle rootClip = new Rectangle();
        rootClip.setArcWidth(10);
        rootClip.setArcHeight(10);

        rootClip.widthProperty().bind(scene.widthProperty());
        rootClip.heightProperty().bind(scene.heightProperty());
        root.setClip(rootClip);
    }
}
