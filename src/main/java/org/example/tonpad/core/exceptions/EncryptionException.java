package org.example.tonpad.core.exceptions;

public class EncryptionException extends TonpadBaseException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
