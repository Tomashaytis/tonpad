package org.example.tonpad.core.js.service;

import javafx.scene.web.WebView;
import org.example.tonpad.core.js.funtcion.JsFunction;

import java.util.concurrent.CompletableFuture;

public interface JsExecutionService {

    <T> CompletableFuture<T> executeJs(WebView webView, Class<? extends JsFunction<T>> jsFunctionClass, Object... params);

}
