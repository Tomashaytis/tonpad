package org.example.tonpad.core.js.service.impl;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ExtractJsFunctionNameException;
import org.example.tonpad.core.exceptions.IllegalJsArgumentException;
import org.example.tonpad.core.js.funtcion.JsFunctionFile;
import org.example.tonpad.core.js.service.JsExecutionService;
import org.example.tonpad.core.js.service.JsFilesService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JsExecutionServiceImpl implements JsExecutionService {

    private static final String JS_FUNCTION_EXTENDED_FORMAT_STRING = """
            (function(){
            try {
                %s
                return %s(%s);
            } catch (e) {
                console.error("JS ERR:", e);
                return "JS_ERROR:" + (e && e.message ? e.message : e);
            }
            })();
            """;

    private final JsFilesService jsFilesService;

    @Override
    public <T> CompletableFuture<T> executeJs(WebView webView, JsFunctionFile<T> jsFunctionFile) {
        String jsFunctionBody = jsFilesService.readFile(jsFunctionFile.getFileName());
        String jsFunctionExtended = JS_FUNCTION_EXTENDED_FORMAT_STRING.formatted(
                jsFunctionBody,
                extractFunctionName(jsFunctionBody),
                generateParamsString(jsFunctionFile.getParams())
        );

        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                @SuppressWarnings("unchecked")
                T result = (T) webView.getEngine().executeScript(jsFunctionExtended);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private String extractFunctionName(String jsFunctionBody) {
        Pattern pattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(jsFunctionBody);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new ExtractJsFunctionNameException("не удалось получить название функции: " + jsFunctionBody);
    }

    private String generateParamsString(List<Object> params) {
        return params.stream()
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
