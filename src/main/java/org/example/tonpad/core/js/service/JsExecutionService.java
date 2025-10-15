package org.example.tonpad.core.js.service;

import javafx.scene.web.WebView;
import org.example.tonpad.core.js.funtcion.JsFunctionFile;

import java.util.concurrent.CompletableFuture;

public interface JsExecutionService {

    <T> CompletableFuture<T> executeJs(WebView webView, JsFunctionFile<T> jsFunctionFile);

}
