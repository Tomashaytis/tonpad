package org.example.tonpad.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.example.tonpad.core.editor.impl.EditorImpl;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.example.tonpad.core.service.crypto.EncryptionService;
import org.example.tonpad.core.service.crypto.Impl.EncryptionServiceImpl;
import org.example.tonpad.core.service.crypto.exception.DecryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.editor.Editor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TabController {

    @Setter
    private TabPane tabPane;

    private final Map<Tab, Editor> editorMap = new ConcurrentHashMap<Tab, Editor>();

    private final Map<Tab, Path> pathMap = new ConcurrentHashMap<Tab, Path>();

    private final RegularFileService fileService;

    private final VaultSession vaultSession;

    public void init(URI fileUri) {
        addNewTabButton();
        createInitialTab(fileUri);
    }

    public void openFileInCurrentTab(String path) {
        try {
            Path filePath = Path.of(path);

            String noteContent = Files.readString(filePath);

            Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
            pathMap.put(currentTab, filePath);
            replaceTabContent(currentTab, getTabName(filePath), noteContent);
        } catch (Exception e) {
            createTemporaryTab("<h1>Error loading content</h1>" + Arrays.toString(e.getStackTrace()));
        }
    }

    public void clearAllTabs() {
        tabPane.getTabs().forEach(this::tabClose);
    }

    private void createInitialTab(URI fileUri) {
        try {
            Path filePath = Path.of(fileUri);
            String noteContent = Files.readString(filePath);
            createTabWithContent(getTabName(filePath), noteContent, filePath);
        } catch (Exception e) {
            createTemporaryTab("<h1>Error loading content</h1>");
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

//        plusLabel.setOnMouseClicked(event -> createNewTab());

        addTab.setOnSelectionChanged(event -> {
            if (addTab.isSelected()) {
//                createNewTab();
                tabPane.getSelectionModel().selectPrevious();
            }
        });
    }

//    private void createNewTab() {
//        createTabWithContent("New Tab " + (tabPane.getTabs().size()), "<h1>New Tab Content</h1>");
//    }

    private void createTemporaryTab(String noteContent) {
        Tab newTab = new Tab("New Tab");

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();

        initTab(newTab, noteContent, content, webView);

        newTab.setOnCloseRequest(event -> tabClose(newTab));
    }

    private void createTabWithContent(String title, String noteContent, Path path) {
        Tab newTab = new Tab(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        pathMap.put(newTab, path);
        initTab(newTab, noteContent, content, webView);

        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile());

        newTab.setOnCloseRequest(event -> {
            saveToFile();
            tabClose(newTab);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private void initTab(Tab tab, String noteContent, AnchorPane content, WebView webView) {
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        byte[] key = vaultSession.getMasterKeyIfPresent()
                .map(k -> k.getEncoded())
                .orElse(null);
        EncryptionService encoder = new EncryptionServiceImpl(key);

        editorMap.put(tab, new EditorImpl(webView.getEngine(), false));
        try
        {
            String resNoteContent = encoder.decrypt(noteContent, null);
            editorMap.get(tab).setNoteContent(resNoteContent);
            
            tab.setContent(content);
            tab.setUserData(webView);

            tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
            tabPane.getSelectionModel().select(tab);
        }
        catch(DecryptionException e)
        {
            javafx.stage.Window owner = (tabPane != null && tabPane.getScene() != null) ? tabPane.getScene().getWindow() : null;

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Заметка зашифрована другим паролем",
                    javafx.scene.control.ButtonType.OK
            );
            if (owner != null) alert.initOwner(owner);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.showAndWait();
            e.printStackTrace();
        }

    }

    private void replaceTabContent(Tab tab, String title, String noteContent) {
        tab.setText(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        
        initTab(tab, noteContent, content, webView);
        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile());

        tab.setOnCloseRequest(event -> {
            saveToFile();
            tabClose(tab);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private String getTabName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private void tabClose(Tab tab) {
    }

    private void saveToFile() {        
        byte[] key = vaultSession.getMasterKeyIfPresent()
                        .map(k -> k.getEncoded())
                        .orElse(null);
        EncryptionService encoder = new EncryptionServiceImpl(key);
        new Thread(() -> {
            try {
                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                Path path = pathMap.get(currentTab);
                String noteContent = editorMap.get(currentTab).getNoteContent().get(3, TimeUnit.SECONDS);
                if (vaultSession.isOpendWithNoPassword())
                    fileService.writeFile(path, noteContent);
                else
                    fileService.writeFile(path, encoder.encrypt(noteContent, null));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }
}
