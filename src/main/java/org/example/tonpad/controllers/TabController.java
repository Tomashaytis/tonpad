package org.example.tonpad.controllers;

import com.vladsch.flexmark.util.ast.Document;
import jakarta.annotation.PostConstruct;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.SneakyThrows;
import org.example.tonpad.service.MarkdownService;
import org.example.tonpad.service.impl.MarkdownServiceImpl;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TabController {
    @Getter
    private final TabPane tabPane;

    private ApplicationContext springContext;

    private MarkdownService markdownService;

    public TabController(TabPane tabPane, ApplicationContext springContext) {
        this.tabPane = tabPane;
        this.springContext = springContext;
        initialize();
    }

    @PostConstruct
    public void init() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(i -> this);
        loader.load();
    }

    @SneakyThrows
    private void initialize() {
        addNewTabButton();
        if (springContext != null) {
            markdownService = springContext.getBean(MarkdownServiceImpl.class);
            createInitialTab();
        }
    }

    private void createInitialTab() {
        try {
            String fileContent = Files.readString(Path.of("src/main/resources/test.md"));
            Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            String html = markdownService.renderMarkdownFileToHtml(markdownFile);
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

    public void createNewTab() {
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

    public void setSpringContext(ApplicationContext springContext) {
        this.springContext = springContext;
        if (springContext != null && markdownService == null) {
            markdownService = springContext.getBean(MarkdownServiceImpl.class);
        }
    }

}
