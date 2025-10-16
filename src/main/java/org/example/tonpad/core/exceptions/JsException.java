package org.example.tonpad.core.exceptions;

public class JsException extends TonpadBaseException {

    public JsException(String message) {
        super(message);
    }

    public JsException(String message, Throwable cause) {
        super(message, cause);
    }

}
