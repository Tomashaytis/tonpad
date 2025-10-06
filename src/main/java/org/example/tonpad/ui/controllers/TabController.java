package org.example.tonpad.ui.controllers;

import com.vladsch.flexmark.util.ast.Document;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


@Component
@RequiredArgsConstructor
public class TabController {

    @Setter
    private TabPane tabPane;

    private final MarkdownService markdownService;

    public void init(String path) {
        addNewTabButton();
        createInitialTab(path);
    }

    public void openFileInCurrentTab(String path) {
        try {
            Path filePath = Path.of(path);

            String fileContent = Files.readString(filePath);
            Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            String html = markdownService.renderMarkdownFileToHtml(markdownFile);

            Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
            replaceTabContent(currentTab, getTabName(filePath), html);
        } catch (Exception e) {
            createTabWithContent("New Tab", "<h1>Error loading content</h1>" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void createInitialTab(String path) {
        try {
            Path filePath = Path.of(path);
            String fileContent = Files.readString(filePath);
            Document markdownFile = markdownService.parseMarkdownFile(fileContent);
            String html = markdownService.renderMarkdownFileToHtml(markdownFile);
            createTabWithContent(getTabName(filePath), html);
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
            tabClose(newTab);
        });

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);
        tabPane.getSelectionModel().select(newTab);
    }

    private void replaceTabContent(Tab tab, String title, String htmlContent) {
        tab.setText(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        webView.getEngine().loadContent(htmlContent);

        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        tab.setContent(content);
        tab.setUserData(webView);
        tab.setOnCloseRequest(event -> {
            tabClose(tab);
        });

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
        tabPane.getSelectionModel().select(tab);
    }

    private String getTabName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private void tabClose(Tab tab) {
    }
}
