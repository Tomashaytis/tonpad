package org.example.tonpad.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.example.tonpad.core.editor.impl.EditorImpl;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.service.crypto.Impl.AesGcmEncryptor;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.editor.Editor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TabController {

    @Setter
    private TabPane tabPane;

    @Getter
    private final Map<Tab, Editor> editorMap = new ConcurrentHashMap<>();

    private final Map<Tab, Path> pathMap = new ConcurrentHashMap<>();

    private final RegularFileService fileService;

    private final VaultSession vaultSession;

    private final EncryptorFactory encryptorFactory;

    public void init(URI fileUri) {
        addNewTabButton();
        createInitialTab(fileUri);
    }

    public void openFileInCurrentTab(Path filePath) {
        // Path filePath = Path.of(path);

        if(vaultSession.isOpendWithNoPassword())
        {
            AesGcmEncryptor encoder = new AesGcmEncryptor();
            if (encoder.isOpeningWithNoPasswordAllowed(filePath)) {
                String noteContent = fileService.readFile(filePath);

                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                pathMap.put(currentTab, filePath);
                replaceTabContent(currentTab, getTabName(filePath), noteContent);
            }
            else {
                throw new DecryptionException("Invalid password");
            }
        }
        else
        {
            try
            {
                byte[] key = vaultSession.getKeyIfPresent().map(k -> k.getEncoded()).orElse(null);
                Encryptor encoder = new AesGcmEncryptor(key);
                String resNoteContent = encoder.decrypt(fileService.readFile(filePath), null);

                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                pathMap.put(currentTab, filePath);
                replaceTabContent(currentTab, getTabName(filePath), resNoteContent);
            }
            catch(DecryptionException e) {
                throw new DecryptionException("Invalid password", e);
            }
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
        debounce.setOnFinished(event -> saveToFile(true));

        newTab.setOnCloseRequest(event -> {
            saveToFile(true);
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
        editorMap.put(tab, new EditorImpl(webView.getEngine(), false));
        editorMap.get(tab).setNoteContent(noteContent);
        tab.setContent(content);
        tab.setUserData(webView);

        tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void replaceTabContent(Tab tab, String title, String noteContent) {
        tab.setText(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        initTab(tab, noteContent, content, webView);
        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile(false));

        tab.setOnCloseRequest(event -> {
            saveToFile(false);
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

    private void saveToFile(boolean isSpetialNote) {
        byte[] key = vaultSession.getKeyIfPresent()
                        .map(k -> k.getEncoded())
                        .orElse(null);
        
        new Thread(() -> {
            try {
                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                Path path = pathMap.get(currentTab);
                String noteContent = editorMap.get(currentTab).getNoteContent().get(3, TimeUnit.SECONDS);
                if (vaultSession.isOpendWithNoPassword() || isSpetialNote)
                    fileService.writeFile(path, noteContent);
                else
                {
                    Encryptor encoder = encryptorFactory.encryptorForKey(key);
                    fileService.writeFile(path, encoder.encrypt(noteContent, null));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }
}
