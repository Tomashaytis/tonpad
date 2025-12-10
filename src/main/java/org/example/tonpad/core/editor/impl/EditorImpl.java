package org.example.tonpad.core.editor.impl;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebEngine;
import lombok.Getter;
import netscape.javascript.JSObject;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.editor.dto.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.editor.enums.FormatType;
import org.example.tonpad.core.editor.enums.LinkType;
import org.example.tonpad.core.editor.enums.ParagraphType;
import org.example.tonpad.core.editor.event.FrontMatterChangeEvent;
import org.example.tonpad.core.editor.listener.FrontMatterChangeListener;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Реализация сервиса EditorApi
 */
public class EditorImpl implements Editor {

    private final WebEngine webEngine;

    private final List<FrontMatterChangeListener> frontMatterListeners = new ArrayList<>();

    private volatile boolean isLoaded = false;

    private final Yaml yaml = new Yaml();

    public EditorImpl(WebEngine webEngine, EditorMode mode, boolean enableDebugAlerts) {
        this.webEngine = webEngine;

        if (mode == EditorMode.NOTE) {
            executeJs("createEditor('note');");
        } else if (mode == EditorMode.SNIPPET) {
            executeJs("createEditor('snippet');");
        } else {
            executeJs("createEditor('template');");
        }

        this.webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                isLoaded = true;

                JSObject window = (JSObject) this.webEngine.executeScript("window");
                window.setMember("editorBridge", this);
            }
        });

        if (enableDebugAlerts) {
            this.webEngine.setOnAlert(e -> {
                String data = e.getData();
                if (data.startsWith("ERROR:")) {
                    System.err.println("\u001B[31mJS " + data + "\u001B[0m");
                } else if (data.startsWith("WARN:")) {
                    System.err.println("\u001B[33mJS " + data + "\u001B[0m");
                } else if (data.startsWith("INFO:")) {
                    System.out.println("\u001B[34mJS " + data + "\u001B[0m");
                } else {
                    System.out.println("JS " + data);
                }
            });
            this.webEngine.setOnError(e -> System.err.println("\u001B[31mJS ERROR: " + e.getMessage() + "\u001B[0m"));
            executeJs("debugAlerts.enable();");
        }

        this.webEngine.load(getEditorHtmlSource().toExternalForm());
    }

    public void setNoteContent(String noteContent) {
        String jsCode = String.format("editor.setNoteContent(%s);",
                toJsString(noteContent));

        executeJs(jsCode);
    }

    public void setFrontMatter(Map<String, String> frontMatter) {
        String yamlContent = yaml.dump(frontMatter);

        String jsCode = String.format("editor.setFrontMatter(%s);",
                toJsString(yamlContent));

        executeJs(jsCode);
    }

    public void insertSnippet(String snippetContent) {
        String jsCode = String.format("editor.insertSnippet(%s);",
                toJsString(snippetContent));

        executeJs(jsCode);
    }

    public CompletableFuture<SearchResult> find(String query) {
        String jsCode = String.format("editor.find(%s);",
                toJsString(query));

        return executeJs(jsCode).thenApply(this::parseSearchResult);
    }

    public CompletableFuture<SearchResult> findNext() {
        return executeJs("editor.findNext();").thenApply(this::parseSearchResult);
    }

    public CompletableFuture<SearchResult> findPrevious() {
        return executeJs("editor.findPrevious();").thenApply(this::parseSearchResult);
    }

    public CompletableFuture<SearchResult> clearSearch() {
        return executeJs("editor.clearSearch();").thenApply(this::parseSearchResult);
    }

    public CompletableFuture<String> getNoteContent() {
        return executeJs("editor.getNoteContent();");
    }

    public CompletableFuture<String> getFrontMatter(boolean jsonFormat) {
        String jsCode = jsonFormat ? "editor.getFrontMatterJSON();" : "editor.getFrontMatterYAML();";
        return executeJs(jsCode);
    }

    public CompletableFuture<String> getDoc() {
        return executeJs("editor.getDoc();");
    }

    public CompletableFuture<String> getHtml() {
        return executeJs("editor.getHTML();");
    }

    public CompletableFuture<String> getMarkdown() {
        return executeJs("editor.getMarkdown();");
    }

    public void format(FormatType format) {
        switch (format) {
            case BOLD -> executeJs("editor.format('bold');");
            case ITALIC -> executeJs("editor.format('italic');");
            case STRIKETHROUGH -> executeJs("editor.format('strikethrough');");
            case HIGHLIGHT -> executeJs("editor.format('highlight');");
            case UNDERLINE -> executeJs("editor.format('underline');");
            case COMMENT -> executeJs("editor.format('comment');");
            case CODE -> executeJs("editor.format('code');");
            case MATH -> executeJs("editor.format('math');");
            case CLEAR -> executeJs("editor.format('clear');");
        }
    }

    public void paragraph(ParagraphType paragraph) {
        switch (paragraph) {
            case BULLET_LIST -> executeJs("editor.paragraph('bullet-list');");
            case ORDERED_LIST -> executeJs("editor.paragraph('ordered-list');");
            case HEADING_1 -> executeJs("editor.paragraph('heading1');");
            case HEADING_2 -> executeJs("editor.paragraph('heading2');");
            case HEADING_3 -> executeJs("editor.paragraph('heading3');");
            case HEADING_4 -> executeJs("editor.paragraph('heading4');");
            case HEADING_5 -> executeJs("editor.paragraph('heading5');");
            case HEADING_6 -> executeJs("editor.paragraph('heading6');");
            case QUOTE -> executeJs("editor.paragraph('quote');");
            case BODY -> executeJs("editor.paragraph('body');");
        }
    }

    public void link(LinkType link) {
        switch (link) {
            case NOTE_LINK -> executeJs("editor.link('note');");
            case EXTERNAL_LINK -> executeJs("editor.link('external');");
        }
    }

    public void insert(String content) {
        String jsCode = String.format("editor.insert(%s);",
                toJsString(content));

        executeJs(jsCode);
    }

    public CompletableFuture<Boolean> canCreateLinks() {
        return executeJs("editor.canCreateLinks()")
                .thenApply("true"::equals);
    }

    public void copy() {
        executeJs("editor.copy();");
    }

    public void cut() {
        executeJs("editor.cut();");
    }

    public void paste() {
        executeJs("editor.paste();");
    }

    public void selectAll() {
        executeJs("editor.selectAll();");
    }

    public void focus() {
        executeJs("editor.focus();");
    }

    public void destroy() {
        executeJs("editor.destroy();");
    }

    public URL getEditorHtmlSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.html"));
    }

    public URL getEditorCssSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.css"));
    }

    public URL getEditorJsSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.js"));
    }

    public void addFrontMatterChangeListener(FrontMatterChangeListener listener) {
        frontMatterListeners.add(listener);
    }

    public void removeFrontMatterChangeListener(FrontMatterChangeListener listener) {
        frontMatterListeners.remove(listener);
    }

    public void onFrontMatterChanged(String action, String oldKey, String oldValue, String newKey, String newValue) {
        Platform.runLater(() -> {
            FrontMatterChangeEvent event = new FrontMatterChangeEvent(action, oldKey, oldValue, newKey, newValue);
            notifyFrontMatterListeners(event);
        });
    }

    private void notifyFrontMatterListeners(FrontMatterChangeEvent event) {
        for (FrontMatterChangeListener listener : frontMatterListeners) {
            listener.onFrontMatterChanged(event);
        }
    }

    private String toJsString(String input) {
        if (input == null) return "null";
        return "'" + escapeForJsString(input) + "'";
    }

    private CompletableFuture<String> executeJs(String jsCode) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                if (!isLoaded) {
                    webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            executeJavaScriptSafely(jsCode, future);
                        } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                            future.completeExceptionally(new RuntimeException("Page failed to load"));
                        }
                    });
                } else {
                    executeJavaScriptSafely(jsCode, future);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void executeJavaScriptSafely(String jsCode, CompletableFuture<String> future) {
        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript(jsCode);
                future.complete(result != null ? result.toString() : null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
    }

    private SearchResult parseSearchResult(String json) {
        if (json == null || json.equals("null")) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, SearchResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search result", e);
        }
    }

    public void setClipboardText(String text) {
        Platform.runLater(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        });
    }

    public String getClipboardText() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.hasString() ? clipboard.getString() : "";
    }

    private String escapeForJsString(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}