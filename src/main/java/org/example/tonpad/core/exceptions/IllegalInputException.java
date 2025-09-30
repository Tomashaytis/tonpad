package org.example.tonpad.core.exceptions;

public class IllegalInputException extends TonpadBaseException {
    public IllegalInputException(String message) {
        super(message);
    }

    public IllegalInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
