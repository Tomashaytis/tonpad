package org.example.tonpad.controllers;

import com.vladsch.flexmark.util.ast.Document;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;
import org.example.tonpad.Starter;
import org.example.tonpad.service.MarkdownService;
import org.example.tonpad.service.impl.MarkdownServiceImpl;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainController {
    @FXML
    private AnchorPane viewingPane;
    @FXML
    private AnchorPane notePane;
    @FXML
    private WebView noteWebView;
    @FXML
    private Button showFilesButton;
    @FXML
    private TabPane tabPane;

    private ApplicationContext springContext;

    private MarkdownService markdownService;
    private boolean isLeftPaneVisible = false;

    @FXML
    @SneakyThrows
    public void initialize() {
        updatePanelVisibility();
        addNewTabButton();
        showFilesButton.setOnAction(event -> toggleLeftPanel());

        springContext = new SpringApplicationBuilder(Starter.class).run();
        markdownService = springContext.getBean(MarkdownServiceImpl.class);
        String fileContent = Files.readString(Path.of("src/main/resources/test.md"));
        Document markdownFile = markdownService.parseMarkdownFile(fileContent);
        String html = markdownService.renderMarkdownFileToHtml(markdownFile);
        noteWebView.getEngine().loadContent(html);
    }

    @FXML
    private void toggleLeftPanel() {
        isLeftPaneVisible = !isLeftPaneVisible;
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        if (isLeftPaneVisible) {
            AnchorPane.setLeftAnchor(noteWebView, 5.0);
            notePane.requestLayout();
            viewingPane.setVisible(true);
            viewingPane.setManaged(true);
            viewingPane.setPrefWidth(viewingPane.getMaxWidth());
        } else {
            AnchorPane.setLeftAnchor(noteWebView, 120.0);
            notePane.requestLayout();
            viewingPane.setVisible(false);
            viewingPane.setManaged(false);
            viewingPane.setPrefWidth(0);
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

        plusLabel.setOnMouseClicked(event -> {
            createNewTab();
        });

        addTab.setOnSelectionChanged(event -> {
            if (addTab.isSelected()) {
                createNewTab();
                tabPane.getSelectionModel().selectPrevious();
            }
        });
    }

    private void createNewTab() {
        Tab newTab = new Tab("New tab" + (tabPane.getTabs().size()));

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        newTab.setContent(content);

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);
        tabPane.getSelectionModel().select(newTab);
    }
}
