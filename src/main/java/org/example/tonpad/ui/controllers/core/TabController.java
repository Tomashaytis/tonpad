package org.example.tonpad.ui.controllers.core;

import javafx.animation.PauseTransition;
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

import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.editor.impl.EditorImpl;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.editor.Editor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
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

    @Getter
    private final Map<Path, Tab> pathMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Tab, Path> inversePathMap = new ConcurrentHashMap<>();

    private final RegularFileService fileSystemService;

    private final VaultSession vaultSession;

    private final EncryptorFactory encryptorFactory;

    public void init(URI fileUri, EditorMode editorMode) {
        createInitialTab(fileUri, editorMode);
    }

    public void openFileInTab(Path filePath, boolean openInCurrent, EditorMode editorMode) {
        if (pathMap.containsKey(filePath)) {
            Tab existingTab = pathMap.get(filePath);
            tabPane.getSelectionModel().select(existingTab);
            return;
        }

        String noteContent;
        if(vaultSession.isOpendWithNoPassword())
        {
            Encryptor encoder = encryptorFactory.encryptorForKey();
            if (encoder.isOpeningWithNoPasswordAllowed(filePath)) {
                noteContent = fileSystemService.readFile(filePath);
            }
            else {
                throw new DecryptionException("Invalid password");
            }
        }
        else
        {
            try
            {
                byte[] key = vaultSession.getKeyIfPresent().map(Key::getEncoded).orElse(null);
                Encryptor encoder = encryptorFactory.encryptorForKey(key);
                noteContent = encoder.decrypt(fileSystemService.readFile(filePath), null);
            }
            catch(DecryptionException e) {
                throw new DecryptionException("Invalid password", e);
            }
        }

        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();

        if (openInCurrent) {
            replaceTabContent(currentTab, getTabName(filePath), noteContent, filePath, editorMode);
        } else {
            createTabWithContent(getTabName(filePath), noteContent, filePath, editorMode);
        }
    }

    public void clearAllTabs() {
    tabPane.getTabs().stream()
            .filter(t -> !t.isDisable())
            .toList()
            .forEach(this::closeTab);
    }

    public void renameTab(Path oldPath, Path newPath) {
        if (pathMap.containsKey(oldPath)) {
            Tab tab = pathMap.get(oldPath);

            pathMap.remove(oldPath);
            pathMap.put(newPath, tab);
            inversePathMap.remove(tab);
            inversePathMap.put(tab, newPath);

            String title = getTabName(newPath);
            tab.setText(title);
        }
    }

    public void closeTab(Path path) {
        if (pathMap.containsKey(path)) {
            Tab tab = pathMap.get(path);
            closeTab(tab);
        }
    }

    public void insertSnippet(Path path) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();

        String snippetContent = fileSystemService.readFile(path);

        editorMap.get(currentTab).insertSnippet(snippetContent);
    }

    private void createInitialTab(URI fileUri, EditorMode editorMode) {
        try {
            Path filePath = Path.of(fileUri);
            String noteContent = Files.readString(filePath);
            createTabWithContent(getTabName(filePath), noteContent, filePath, editorMode);
        } catch (Exception e) {
            createErrorTab();
        }
    }

    private void createErrorTab() {
        Tab newTab = new Tab("New Tab");

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();

        initTabContent(newTab, "<h1>Error loading content</h1>", content, webView, EditorMode.SNIPPET);
        addTabToPane(newTab);

        newTab.setOnCloseRequest(event -> closeTab(newTab));
    }

    private void createTabWithContent(String title, String noteContent, Path path, EditorMode editorMode) {
        Tab newTab = new Tab(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        pathMap.put(path, newTab);
        inversePathMap.put(newTab, path);
        initTabContent(newTab, noteContent, content, webView, editorMode);
        addTabToPane(newTab);

        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile(true));

        newTab.setOnCloseRequest(event -> {
            saveToFile(true);
            closeTab(newTab);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private void initTabContent(Tab tab, String noteContent, AnchorPane content, WebView webView, EditorMode editorMode) {
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        Editor editor = new EditorImpl(webView.getEngine(), editorMode, false);
        editor.setNoteContent(noteContent);
        editorMap.put(tab, editor);

        tab.setContent(content);
        tab.setUserData(webView);
    }

    private void addTabToPane(Tab tab) {
        int size = tabPane.getTabs().size();

        int index = size;

        if (size > 0) {
            Tab last = tabPane.getTabs().get(size - 1);
            if (last.isDisable()) {
                index = size - 1;
            }
        }

        tabPane.getTabs().add(index, tab);
        tabPane.getSelectionModel().select(tab);
    }


    private void replaceTabContent(Tab tab, String title, String noteContent, Path path, EditorMode editorMode) {
        saveToFile(false);
        tab.setText(title);
        pathMap.put(path, tab);
        inversePathMap.put(tab, path);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        initTabContent(tab, noteContent, content, webView, editorMode);
        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile(false));

        tab.setOnCloseRequest(event -> {
            saveToFile(false);
            closeTab(tab);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private String getTabName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private void closeTab(Tab tab) {
        editorMap.remove(tab);
        Path path = inversePathMap.get(tab);
        inversePathMap.remove(tab);
        pathMap.remove(path);

        if (tab.getTabPane() != null) {
            tab.getTabPane().getTabs().remove(tab);
        }
    }

    private void saveToFile(boolean isSpecialNote) {
        byte[] key = vaultSession.getKeyIfPresent()
                        .map(Key::getEncoded)
                        .orElse(null);
        
        new Thread(() -> {
            try {
                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                Path path = inversePathMap.get(currentTab);
                String noteContent = editorMap.get(currentTab).getNoteContent().get(3, TimeUnit.SECONDS);
                if (vaultSession.isOpendWithNoPassword() || isSpecialNote)
                    fileSystemService.writeFile(path, noteContent);
                else
                {
                    Encryptor encoder = encryptorFactory.encryptorForKey(key);
                    fileSystemService.writeFile(path, encoder.encrypt(noteContent, null));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }
}
