package org.example.tonpad.ui.controllers.core;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.editor.enums.FormatType;
import org.example.tonpad.core.editor.impl.EditorImpl;
import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.example.tonpad.core.service.RecentTabService;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.exceptions.ObjectNotFoundException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.ui.controllers.toolbar.EditorToolbarController;
import org.example.tonpad.ui.extentions.TabParams;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class TabController {

    private TabPane tabPane;

    private boolean vaultChanging = false;

    @Getter
    private final Map<Tab, TabParams> tabMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Path, Tab> pathMap = new ConcurrentHashMap<>();

    private final EditorToolbarController editorToolbarController;

    private final RegularFileService fileSystemService;

    private final RecentTabService recentTabService;

    private final VaultSession vaultSession;

    private final EncryptorFactory encryptorFactory;

    public void init(URI fileUri, EditorMode editorMode, boolean protectedMode) {
        createInitialTab(fileUri, editorMode, protectedMode);
    }

    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;

        this.tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (vaultChanging) {
                return;
            }

            if (newTab == null) {
                recentTabService.clearLastActive();
                return;
            }

            TabParams params = tabMap.get(newTab);
            if (params == null) {
                recentTabService.clearLastActive();
                return;
            }

            Path path = params.path();
            if (path == null) {
                recentTabService.clearLastActive();
                return;
            }
            recentTabService.updateLastActive(path);
        });
    }
    
    public void refreshRtConfig() {
        recentTabService.refreshRtConfig();
    }

    public void restoreRecentTabs() {
        Path lastActivePath = recentTabService.getLastActiveTab().orElse(null);

        vaultChanging = true;

        StringBuilder sb = new StringBuilder();
        List<Path> badPaths = new ArrayList<>();
        boolean[] lastActiveOpened = new boolean[1];
        lastActiveOpened[0] = false;
        try {
            recentTabService.getRecentTabs().forEach(tabOpt -> {
                                if (tabOpt.isEmpty()) return;
                                Path notePath = tabOpt.get();
                                try {
                                    openFileInTab(notePath, false, EditorMode.NOTE, true);
                                    if (lastActivePath != null && lastActivePath.equals(notePath)) {
                                        lastActiveOpened[0] = true;
                                    }
                                }
                                catch (Exception ex) {
                                    sb.append(notePath).append(";\n");
                                    badPaths.add(notePath);
                                }
                            });

        }
        finally {
            vaultChanging = false;
        }
        if (!badPaths.isEmpty()) {
            badPaths.forEach(recentTabService::deleteClosedTab);
        }

        if (lastActiveOpened[0]) {
            Tab lastTab = pathMap.get(lastActivePath);
            if (lastTab != null) {
                tabPane.getSelectionModel().select(lastTab);
                recentTabService.updateLastActive(lastActivePath);
            }
        }
        else {
            if (!pathMap.isEmpty()) {
                Path firstPath = pathMap.keySet().iterator().next();
                Tab firstTab = pathMap.get(firstPath);
                if (firstTab != null) {
                    tabPane.getSelectionModel().select(firstTab);
                    recentTabService.updateLastActive(firstPath);
                }
            }
            else {
                recentTabService.clearLastActive();
            }
        }

        if (!sb.isEmpty()) {
            throw new ObjectNotFoundException("Not all tabs restored: couldn't open files: " + sb);
        }
    }

    public void openFileInTab(Path filePath, boolean openInCurrent, EditorMode editorMode, boolean protectedMode) {
        if (pathMap.containsKey(filePath)) {
            Tab existingTab = pathMap.get(filePath);
            tabPane.getSelectionModel().select(existingTab);
            return;
        }

        String noteContent;

        if (protectedMode) {
            if(vaultSession.isOpendWithNoPassword()) {
                Encryptor encoder = encryptorFactory.encryptorForKey();
                if (encoder.isActionWithNoPasswordAllowed(filePath)) {
                    noteContent = fileSystemService.readFile(filePath);
                }
                else {
                    throw new DecryptionException("Invalid password");
                }
            } else {
                try {
                    byte[] key = vaultSession.getKeyIfPresent().map(Key::getEncoded).orElse(null);
                    Encryptor encoder = encryptorFactory.encryptorForKey(key);
                    noteContent = encoder.decrypt(fileSystemService.readFile(filePath), null);
                }
                catch(DecryptionException e) {
                    throw new DecryptionException("Invalid password", e);
                }
            }
        } else {
            noteContent = fileSystemService.readFile(filePath);
        }

        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();

        if (openInCurrent) {
            replaceTabContent(currentTab, getTabName(filePath), noteContent, filePath, editorMode, protectedMode);
        } else {
            createTabWithContent(getTabName(filePath), noteContent, filePath, editorMode, protectedMode);
        }
        recentTabService.addOpenedTab(filePath);
    }

    public void clearAllTabs() {
        vaultChanging = true;
        try {
            tabPane.getTabs().stream()
                    .filter(t -> !t.isDisable())
                    .toList()
                    .forEach(t -> closeTab(t, true));
        } finally {
            vaultChanging = false;
        }
    }

    public void renameTab(Path oldPath, Path newPath) {
        if (pathMap.containsKey(oldPath)) {
            Tab tab = pathMap.get(oldPath);

            pathMap.remove(oldPath);
            pathMap.put(newPath, tab);

            TabParams tabParams = tabMap.get(tab);
            tabMap.put(tab, new TabParams(tabParams.editor(), newPath));

            String title = getTabName(newPath);
            tab.setText(title);
        }
        boolean wasInRecent = recentTabService.isInRecent(oldPath);
        recentTabService.renamePath(oldPath, newPath);
        recentTabService.renameLastActive(oldPath, newPath);
        if (wasInRecent) {
            recentTabService.deleteClosedTab(oldPath);
            recentTabService.addOpenedTab(newPath);
        }
    }

    public void closeTab(Path path, boolean isVaultChanging) {
        if (pathMap.containsKey(path)) {
            Tab tab = pathMap.get(path);
            closeTab(tab, isVaultChanging);
        }
    }

    public void insertSnippet(Path path) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();

        String snippetContent = fileSystemService.readFile(path);

        tabMap.get(currentTab).editor().insertSnippet(snippetContent);
    }

    public Editor getActiveEditor() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return tabMap.get(tab).editor();
    }

    private void createInitialTab(URI fileUri, EditorMode editorMode, boolean protectedMode) {
        try {
            Path filePath = Path.of(fileUri);
            String noteContent = Files.readString(filePath);
            createTabWithContent(getTabName(filePath), noteContent, filePath, editorMode, protectedMode);
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

        newTab.setOnCloseRequest(event -> closeTab(newTab, false));
    }

    private void createTabWithContent(String title, String noteContent, Path path, EditorMode editorMode, boolean protectedMode) {
        Tab newTab = new Tab(title);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        pathMap.put(path, newTab);
        Editor editor = initTabContent(newTab, noteContent, content, webView, editorMode);
        tabMap.put(newTab, new TabParams(editor, path));

        addTabToPane(newTab);

        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile(newTab, protectedMode));

        newTab.setOnCloseRequest(event -> {
            saveToFile(newTab, protectedMode);
            closeTab(newTab, false);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private Editor initTabContent(Tab tab, String noteContent, AnchorPane content, WebView webView, EditorMode editorMode) {
        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);
        content.getChildren().add(webView);

        Editor editor = new EditorImpl(webView.getEngine(), editorMode, false);
        editor.setNoteContent(noteContent);

        setupKeyboardShortcutsForWebView(webView, editor);

        setupContextMenuForWebView(webView, editor);

        tab.setContent(content);
        tab.setUserData(webView);

        return editor;
    }

    private void setupKeyboardShortcutsForWebView(WebView webView, Editor editor) {
        webView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case B:
                        editor.format(FormatType.BOLD);
                        event.consume();
                        break;
                    case I:
                        editor.format(FormatType.ITALIC);
                        event.consume();
                        break;
                    case U:
                        editor.format(FormatType.UNDERLINE);
                        event.consume();
                        break;
                }
            }
        });
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


    private void replaceTabContent(Tab tab, String title, String noteContent, Path path, EditorMode editorMode, boolean protectedMode) {
        tab.setText(title);
        pathMap.put(path, tab);

        AnchorPane content = new AnchorPane();
        WebView webView = new WebView();
        Editor editor = initTabContent(tab, noteContent, content, webView, editorMode);
        tabMap.put(tab, new TabParams(editor, path));

        PauseTransition debounce = new PauseTransition(Duration.millis(1500));
        debounce.setOnFinished(event -> saveToFile(tab, protectedMode));

        tab.setOnCloseRequest(event -> {
            saveToFile(tab, protectedMode);
            closeTab(tab, false);
        });
        content.addEventFilter(KeyEvent.KEY_TYPED, event -> debounce.playFromStart());
    }

    private String getTabName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private void closeTab(Tab tab, boolean isVaultChanging) {
        Path path = tabMap.get(tab).path();
        tabMap.remove(tab);
        pathMap.remove(path);

        if (tab.getTabPane() != null) {
            tab.getTabPane().getTabs().remove(tab);
        }
        if (!isVaultChanging) recentTabService.deleteClosedTab(path);
    }

    private void setupContextMenuForWebView(WebView webView, Editor editor) {
        webView.setContextMenuEnabled(false);

        webView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                editorToolbarController.setEditor(editor);
                editorToolbarController.setWebView(webView);
                editorToolbarController.showAt(event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
    }

    private void saveToFile(Tab tab, boolean protectedMode) {
        byte[] key = vaultSession.getKeyIfPresent()
                        .map(Key::getEncoded)
                        .orElse(null);

        TabParams params = tabMap.get(tab);
        if (params == null) {
            return;
        }

        Path path = params.path();
        Editor editor = params.editor();
        
        new Thread(() -> {
            try {
                String noteContent = editor.getNoteContent().get(3, TimeUnit.SECONDS);
                if (vaultSession.isOpendWithNoPassword() || !protectedMode)
                    fileSystemService.writeFile(path, noteContent);
                else
                {
                    Encryptor encoder = encryptorFactory.encryptorForKey(key);
                    fileSystemService.writeFile(path, encoder.encrypt(noteContent, null));
                }
            } catch (Exception e) {
                throw new TonpadBaseException("Editor not responds");
            }
        }).start();
    }
}
