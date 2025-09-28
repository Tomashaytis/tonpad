package org.example.tonpad.core.exceptions;

public class CustomIOException extends TonpadBaseException {

    public CustomIOException(String message) {
        super(message);
    }

    public CustomIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
