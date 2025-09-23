package org.example.tonpad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.tonpad.test.MarkdownHtmlTest;

import java.nio.file.Files;
import java.nio.file.Path;

public class TonpadApplication extends Application {

    @Override
    public void start(Stage stage) {
        /*Label helloLabel = new Label("Hello World");
        helloLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: blue;");

        StackPane root = new StackPane();
        root.getChildren().add(helloLabel);

        Scene scene = new Scene(root, 1500, 900);

        stage.setTitle("Tonpad - Hello World");
        stage.setScene(scene);
        stage.setResizable(true);

        stage.show();*/


        String html = MarkdownHtmlTest.toHtml();

        String styledHtml = "<html><head><style>" +
                "body { font-family: Arial, sans-serif; padding: 20px; }" +
                "h1, h2, h3 { color: #333; }" +
                "pre { background: #f4f4f4; padding: 10px; border-radius: 5px; }" +
                "code { font-family: 'Courier New', monospace; }" +
                "</style></head><body contenteditable='true'>" + html + "</body></html>";

        WebView webView = new WebView();
        webView.getEngine().loadContent(styledHtml);



        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(event -> {
            try {
                String currentHtml = (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
                String newMarkdown = MarkdownHtmlTest.toMarkdown(currentHtml);
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
