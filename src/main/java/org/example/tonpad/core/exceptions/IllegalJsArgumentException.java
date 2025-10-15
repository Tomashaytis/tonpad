package org.example.tonpad.core.exceptions;

public class IllegalJsArgumentException extends JsException {

    public IllegalJsArgumentException(String message) {
        super(message);
    }

    public IllegalJsArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

}
