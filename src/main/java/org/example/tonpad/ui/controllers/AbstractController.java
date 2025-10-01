package org.example.tonpad.ui.controllers;

import jakarta.annotation.PostConstruct;
import javafx.fxml.FXMLLoader;

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
}
