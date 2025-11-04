package org.example.tonpad.core.editor;

import java.net.URL;

/**
 * Сервис для работы с Markdown редактором в JavaFX
 * Инкапсулирует все вызовы JavaScript API
 */
public interface Editor {
    void setContent(String noteContent);

    void getContent(JsCallback callback);

    void getFrontMatter(JsCallback callback, boolean jsonFormat);

    void getDoc(JsCallback callback);

    void getHtml(JsCallback callback);

    void getMarkdown(JsCallback callback);

    void focus();

    void destroy();

    URL getEditorHtmlSource();

    URL getEditorCssSource();

    URL getEditorJsSource();

    interface JsCallback {

        void onResult(String result);
    }
}