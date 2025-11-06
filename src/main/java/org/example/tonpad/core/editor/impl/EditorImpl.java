package org.example.tonpad.core.editor.impl;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import org.example.tonpad.core.editor.Editor;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Реализация сервиса EditorApi
 */
public class EditorImpl implements Editor {

    private final WebEngine webEngine;
    private volatile boolean isLoaded = false;

    public EditorImpl(WebEngine webEngine, boolean enableDebugAlerts) {
        this.webEngine = webEngine;

        this.webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                isLoaded = true;
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
        }

        this.webEngine.load(getEditorHtmlSource().toExternalForm());
    }

    @Override
    public void setNoteContent(String noteContent) {
        String jsCode = String.format("editor.setNoteContent(%s);",
                toJsString(noteContent));

        executeJs(jsCode);
    }

    @Override
    public CompletableFuture<String> getNoteContent() {
        return executeJs("editor.getNoteContent();");
    }

    @Override
    public CompletableFuture<String> getFrontMatter(boolean jsonFormat) {
        String jsCode = jsonFormat ? "editor.getFrontMatterJSON();" : "editor.getFrontMatterYAML();";
        return executeJs(jsCode);
    }

    @Override
    public CompletableFuture<String> getDoc() {
        return executeJs("editor.getDoc();");
    }

    @Override
    public CompletableFuture<String> getHtml() {
        return executeJs("editor.getHTML();");
    }

    @Override
    public CompletableFuture<String> getMarkdown() {
        return executeJs("editor.getMarkdown();");
    }

    @Override
    public void focus() {
        executeJs("editor.focus();");
    }

    @Override
    public void destroy() {
        executeJs("editor.destroy();");
    }

    @Override
    public URL getEditorHtmlSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.html"));
    }

    @Override
    public URL getEditorCssSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.css"));
    }

    @Override
    public URL getEditorJsSource() {
        return Objects.requireNonNull(getClass().getResource("/editor/editor.js"));
    }

    private String toJsString(String input) {
        if (input == null) return "null";
        return "`" + input + "`";
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
}