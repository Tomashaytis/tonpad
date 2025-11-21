package org.example.tonpad.core.exceptions;

public class DerivationException extends TonpadBaseException {

    public DerivationException(String message) {
        super(message);
    }

    public DerivationException(String message, Throwable cause) {
        super(message, cause);
    }
}
