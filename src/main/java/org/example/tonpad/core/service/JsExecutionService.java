package org.example.tonpad.core.service;

import javafx.scene.web.WebView;

import java.util.concurrent.CompletableFuture;

public interface JsExecutionService {

    CompletableFuture<Integer> getCaretPosition(WebView webView);

    void insertTextAtPosition(WebView webView, String text, int position);

}
