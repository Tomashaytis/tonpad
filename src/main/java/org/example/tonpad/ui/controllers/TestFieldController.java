package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import netscape.javascript.JSException;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.editor.impl.EditorImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Component
@RequiredArgsConstructor
public class TestFieldController extends AbstractController {

    @FXML
    private VBox mainVBox;

    private WebView webView;

    private WebEngine webEngine;

    private Editor editor;

    public void init(Stage stage) {
        setStage(stage, mainVBox, StageStyle.DECORATED);
        loadFile();
    }

    public void loadFile() {
        try {
            URL noteUrl = Objects.requireNonNull(getClass().getResource("/note.md"));
            Path notePath = Paths.get(noteUrl.toURI());
            String note = Files.readString(notePath);

            createWebView(note);
        } catch (IOException e) {
            createWebView("Error loading content");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void createWebView(String markdown) {
        AnchorPane content = new AnchorPane();
        webView = new WebView();

        editor = new EditorImpl(webView.getEngine(), true);
        if (!markdown.isEmpty()) {
            editor.setNoteContent(markdown);
        }
        new Thread(() -> {
            try {
                String note = editor.getNoteContent().get(3, TimeUnit.SECONDS);
                System.out.println("Содержимое: " + note);
            } catch (TimeoutException e) {
                System.out.println("Таймаут получения содержимого");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }).start();


        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);
        mainVBox.getChildren().add(content);
    }
    public String getMarkdownFromEditor() {
        if (webEngine != null) {
            try {
                return (String) webEngine.executeScript("editor.getMarkdown()");
            } catch (JSException e) {
                System.err.println("Error getting Markdown: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    public String getHTMLFromEditor() {
        if (webEngine != null) {
            try {
                return (String) webEngine.executeScript("editor.getHTML()");
            } catch (JSException e) {
                System.err.println("Error getting HTML: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/test-field.fxml";
    }
}
