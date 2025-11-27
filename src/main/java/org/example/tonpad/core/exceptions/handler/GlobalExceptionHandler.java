package org.example.tonpad.core.exceptions.handler;

import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final List<HandlerMethod> handlers = new ArrayList<>();

    @PostConstruct
    private void postConstruct() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
            if (annotation != null && method.getParameterCount() == 1) {
                @SuppressWarnings("unchecked")
                Class<? extends Exception> paramType = (Class<? extends Exception>) method.getParameterTypes()[0];
                handlers.add(new HandlerMethod(paramType, method));
            }
        }

        handlers.sort((h1, h2) -> {
            Class<?> c1 = h1.exceptionType();
            Class<?> c2 = h2.exceptionType();

            if (c1.isAssignableFrom(c2)) {
                return 1;
            }

            if (c2.isAssignableFrom(c1)) {
                return -1;
            }

            return 0;
        });
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        for (HandlerMethod handler : handlers) {
            if (handler.exceptionType().isInstance(throwable)) {
                try {
                    handler.method().invoke(this, throwable);
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @ExceptionHandler
    private void handleAnyException(Exception exception) {
        showErrorAlert(exception);
    }

    @ExceptionHandler
    private void handleBaseException(TonpadBaseException exception) {
        showErrorAlert(exception);
    }

    private void showErrorAlert(Exception exception) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(exception.getMessage());

            alert.showAndWait();
        });
    }

}
