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

    public EditorImpl(WebEngine webEngine, boolean enableDebugAlerts) {
        this.webEngine = webEngine;

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

        if (enableDebugAlerts) {
            executeJs("debugAlerts.enable();");
        }

        this.webEngine.load(getEditorHtmlSource().toExternalForm());
    }

    @Override
    public void setContent(String noteContent) {
        String jsCode = String.format("editor.setNoteContent(%s);",
                toJsString(noteContent));

        executeJs(jsCode);
    }

    @Override
    public void getContent(JsCallback callback) {
        executeJs("editor.getNoteContent();")
                .thenAccept(callback::onResult)
                .exceptionally(throwable -> {
                    callback.onResult(null);
                    return null;
                });
    }

    @Override
    public void getFrontMatter(JsCallback callback, boolean jsonFormat) {
        String jsCode = jsonFormat ? "editor.getFrontMatterJSON();" : "editor.getFrontMatterYAML();";
        executeJs(jsCode)
                .thenAccept(callback::onResult)
                .exceptionally(throwable -> {
                    callback.onResult(null);
                    return null;
                });
    }

    @Override
    public void getDoc(JsCallback callback) {
        executeJs("editor.getDoc();")
                .thenAccept(callback::onResult)
                .exceptionally(throwable -> {
                    callback.onResult(null);
                    return null;
                });
    }

    @Override
    public void getHtml(JsCallback callback) {
        executeJs("editor.getHTML();")
                .thenAccept(callback::onResult)
                .exceptionally(throwable -> {
                    callback.onResult(null);
                    return null;
                });
    }

    @Override
    public void getMarkdown(JsCallback callback) {
        executeJs("editor.getMarkdown();")
                .thenAccept(callback::onResult)
                .exceptionally(throwable -> {
                    callback.onResult(null);
                    return null;
                });
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

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    try {
                        Object result = webEngine.executeScript(jsCode);
                        future.complete(result != null ? result.toString() : null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            }
        });

        return future;
    }
}