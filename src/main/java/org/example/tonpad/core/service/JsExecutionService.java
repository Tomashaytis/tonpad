package org.example.tonpad.core.service;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ExtractJsFunctionNameException;
import org.example.tonpad.core.exceptions.IllegalJsArgumentException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JsExecutionService {

    private final JsFunctionProviderService jsFunctionProviderService;

    public CompletableFuture<Integer> getCaretPosition(WebView webView) {
        return executeJs(webView, jsFunctionProviderService.getGetCaretPosition());
    }

    public void insertTextAtPosition(WebView webView, String text, int position) {
        executeJs(webView, jsFunctionProviderService.getInsertTextAtPosition(), text, position);
    }

    private <T> CompletableFuture<T> executeJs(WebView webView, String jsFunctionBody, Object... params) {
        String wrapped = """
            (function(){
            try {
                %s
            } catch (e) {
                console.error("JS ERR:", e);
                return "JS_ERROR:" + (e && e.message ? e.message : e);
            }
            })();
            """.formatted(generateFullJsCode(jsFunctionBody, List.of(params)));

        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                @SuppressWarnings("unchecked")
                T result = (T) webView.getEngine().executeScript(wrapped);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private String generateFullJsCode(String jsFunctionBody, List<Object> params) {
        String functionName = extractFunctionName(jsFunctionBody);

        String paramsString = params.stream()
                .map(param -> {
                    if (param instanceof String) {
                        return "'" + escapeForJsString((String) param) + "'";
                    } else if (param instanceof Number || param instanceof Boolean) {
                        return param.toString();
                    } else {
                        throw new IllegalJsArgumentException("неподдерживаемый тип параметра: " + param.getClass());
                    }
                })
                .collect(Collectors.joining(", "));

        return jsFunctionBody +
                "\n\n" +
                "return " + functionName + "(" + paramsString + ");";
    }

    private String extractFunctionName(String jsFunctionBody) {
        Pattern pattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(jsFunctionBody);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new ExtractJsFunctionNameException("не удалось получить название функции: " + jsFunctionBody);
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
