package org.example.tonpad.ui.controllers;

import com.vladsch.flexmark.util.ast.Document;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import lombok.Setter;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TabController {

    @Setter
    private TabPane tabPane;

    private final MarkdownService markdownService;

    public TabController(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }

    public void init() {
        addNewTabButton();
        createInitialTab();
    }

    private void createInitialTab() {
        try {
            String fileContent = Files.readString(Path.of("src/main/resources/test.md"));
            Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            String html = markdownService.renderMarkdownFileToHtml(markdownFile);
            String markdown = markdownService.convertHtmlToMarkdown(html);
            createTabWithContent("Initial Tab", html);
        } catch (Exception e) {
            createTabWithContent("New Tab", "<h1>Error loading content</h1>");
        }
    }

    private void addNewTabButton() {
        Tab addTab = new Tab();
        addTab.setClosable(false);
        addTab.setDisable(true);

        Label plusLabel = new Label("+");
        plusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        addTab.setGraphic(plusLabel);

        tabPane.getTabs().add(addTab);

        plusLabel.setOnMouseClicked(event -> createNewTab());

        addTab.setOnSelectionChanged(event -> {
            if (addTab.isSelected()) {
                createNewTab();
                tabPane.getSelectionModel().selectPrevious();
            }
        });
    }

    private void createNewTab() {
        createTabWithContent("New Tab " + (tabPane.getTabs().size()), "<h1>New Tab Content</h1>");
    }

    private void createTabWithContent(String title, String htmlContent) {
        Tab newTab = new Tab(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        webView.getEngine().loadContent(htmlContent);

        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        newTab.setContent(content);
        newTab.setUserData(webView);
        newTab.setOnCloseRequest(event -> {
            // Закрытие вкладки
        });

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);
        tabPane.getSelectionModel().select(newTab);
    }
}
