package org.example.tonpad;

import com.vladsch.flexmark.util.ast.Document;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.example.tonpad.service.MarkdownService;
import org.example.tonpad.service.impl.MarkdownServiceImpl;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

public class TonpadApplication extends Application {

    private ApplicationContext springContext;

    private MarkdownService markdownService;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Starter.class).run();
        markdownService = springContext.getBean(MarkdownServiceImpl.class);
    }

    @Override
    @SneakyThrows
    public void start(Stage stage) {
        String fileContent = Files.readString(Path.of("src/main/resources/test.md"));
        Document markdownFile = markdownService.parseMarkdownFile(fileContent);
        String html = markdownService.renderMarkdownFileToHtml(markdownFile);

        WebView webView = new WebView();
        webView.getEngine().loadContent(html);



        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(event -> {
            try {
                String currentHtml = (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
                String newMarkdown = markdownService.convertHtmlToMarkdown(currentHtml);
                Files.writeString(Path.of("src/main/resources/test-edited.md"), newMarkdown);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(10, webView, saveButton);

        Scene scene = new Scene(layout, 800, 600);
        stage.setTitle("Editable Markdown to HTML Viewer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
