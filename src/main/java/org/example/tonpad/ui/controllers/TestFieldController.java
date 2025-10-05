package org.example.tonpad.ui.controllers;

import com.vladsch.flexmark.util.ast.Document;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;


@Component
@RequiredArgsConstructor
public class TestFieldController extends AbstractController {

    @FXML
    private VBox mainVBox;

    private final MarkdownService markdownService;

    public void init(Stage stage) {
        setStage(stage, mainVBox, StageStyle.DECORATED);
        loadFile();
    }

    public void loadFile() {
        try {
            Path filePath = Path.of("src/main/resources/Welcome.md");

            String fileContent = Files.readString(filePath);
            Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            String html = markdownService.renderMarkdownFileToHtml(markdownFile);
            createWebView(html);
        } catch (Exception e) {
            createWebView("<h1>Error loading content</h1>");
        }
    }

    private void createWebView(String html) {
        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        webView.getEngine().loadContent(html);

        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);
        mainVBox.getChildren().add(content);
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/test-field.fxml";
    }
}
