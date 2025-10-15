package org.example.tonpad.core.js.service.impl;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.example.tonpad.core.exceptions.ExtractJsFunctionNameException;
import org.example.tonpad.core.exceptions.IllegalJsArgumentException;
import org.example.tonpad.core.js.funtcion.JsFunction;
import org.example.tonpad.core.js.service.JsExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
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

    private final Map<Class<?>, JsFunction<?>> functionClassToFunction = new HashMap<>();

    @Autowired
    public JsExecutionServiceImpl(List<JsFunction<?>> functions) {
        for (JsFunction<?> function : functions) {
            functionClassToFunction.put(function.getClass(), function);
        }
    }

    public <T> CompletableFuture<T> executeJs(WebView webView, Class<? extends JsFunction<T>> jsFunctionClass, Object... params) {
        @SuppressWarnings("unchecked")
        JsFunction<T> function = (JsFunction<T>) functionClassToFunction.get(jsFunctionClass);
        validateFunctionParams(function, params);

        String jsFunctionBody = function.getJsCode();
        String jsFunctionExtended = JS_FUNCTION_EXTENDED_FORMAT_STRING.formatted(
                jsFunctionBody,
                extractFunctionName(jsFunctionBody),
                generateParamsString(params)
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

    private void validateFunctionParams(JsFunction<?> function, Object... params) {
        List<Class<?>> expectedClasses = function.getParamsTypes();
        if (expectedClasses.size() != params.length) {
            throw new IllegalJsArgumentException("неверное число параметров");
        }

        for (int i = 0; i < params.length; ++i) {
            Class<?> provided = params[i].getClass();
            Class<?> expected = expectedClasses.get(i);
            if (provided != expected) {
                throw new IllegalJsArgumentException("неверный тип аргумента " + provided + ", ожидается " + expected);
            }
        }
    }

    private String extractFunctionName(String jsFunctionBody) {
        Pattern pattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(jsFunctionBody);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new ExtractJsFunctionNameException("не удалось получить название функции: " + jsFunctionBody);
    }

    private String generateParamsString(Object... params) {
        return Arrays.stream(params)
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
