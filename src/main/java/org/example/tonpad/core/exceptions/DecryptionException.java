package org.example.tonpad.core.exceptions;

public class DecryptionException extends TonpadBaseException {

    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Exception cause) {
        super(message, cause);
    }
}
