package org.example.tonpad.ui.controllers;

import com.vladsch.flexmark.util.ast.Document;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import netscape.javascript.JSException;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;


@Component
@RequiredArgsConstructor
public class TestFieldController extends AbstractController {

    @FXML
    private VBox mainVBox;

    private WebView webView;

    private WebEngine webEngine;

    private final MarkdownService markdownService;

    public void init(Stage stage) {
        setStage(stage, mainVBox, StageStyle.DECORATED);
        loadFile();
    }

    public void loadFile() {
        try {
            Path filePath = Path.of("/Welcome.md");
            String fileContent = Files.readString(filePath);

            //Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            //String html = markdownService.renderMarkdownFileToHtml(markdownFile);
            //createWebViewOld(html);
            createWebView(fileContent);
        } catch (IOException e) {
            createWebView("Error loading content");
        }
    }

    private void createWebViewOld(String html) {
        AnchorPane content = new AnchorPane();
        webView = new WebView();
        webEngine = webView.getEngine();

        webEngine.loadContent(html);

        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);
        mainVBox.getChildren().add(content);
    }

    private void createWebView(String markdown) {
        AnchorPane content = new AnchorPane();
        webView = new WebView();
        webEngine = webView.getEngine();

        // Логи JS-консоли в Java
        webEngine.setOnAlert(e -> System.out.println("JS Log: " + e.getData()));
        webEngine.setOnError(e -> System.err.println("JS Error: " + e.getMessage()));

        webEngine.load(Objects.requireNonNull(getClass().getResource("/ui/html/editor.html")).toString());

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

    private String escapeJavaScript(String str) {
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/test-field.fxml";
    }
}
