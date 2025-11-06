package org.example.tonpad.core.editor;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для работы с Markdown редактором в JavaFX
 * Инкапсулирует все вызовы JavaScript API
 */
public interface Editor {
    void setNoteContent(String noteContent);

    CompletableFuture<String> getNoteContent();

    CompletableFuture<String> getFrontMatter(boolean jsonFormat);

    CompletableFuture<String> getDoc();

    CompletableFuture<String> getHtml();

    CompletableFuture<String> getMarkdown();

    void focus();

    void destroy();

    URL getEditorHtmlSource();

    URL getEditorCssSource();

    URL getEditorJsSource();

    interface JsCallback {

        void onResult(String result);
    }
}