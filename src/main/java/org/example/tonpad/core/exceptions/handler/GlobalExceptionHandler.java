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
public class GlobalExceptionHandler {

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
            Class<?> c1 = h1.getExceptionType();
            Class<?> c2 = h2.getExceptionType();

            if (c1.isAssignableFrom(c2)) {
                return 1;
            }

            if (c2.isAssignableFrom(c1)) {
                return -1;
            }

            return 0;
        });
    }

    public void handle(Throwable throwable) {
        if (!(throwable instanceof TonpadBaseException exception)) {
            return;
        }

        for (HandlerMethod handler : handlers) {
            if (handler.getExceptionType().isInstance(exception)) {
                try {
                    handler.getMethod().invoke(this, exception);

                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @ExceptionHandler
    private void handleBaseException(TonpadBaseException exception) {
        showErrorAlert(exception);
    }

    private void showErrorAlert(TonpadBaseException exception) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(exception.getClass().getSimpleName());
            alert.setContentText(exception.getMessage());

            alert.showAndWait();
        });
    }

}
