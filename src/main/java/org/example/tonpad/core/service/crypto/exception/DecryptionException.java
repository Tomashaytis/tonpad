package org.example.tonpad.core.service.crypto.exception;

public class DecryptionException extends Exception {
    public DecryptionException(Exception e) {super(e);}
    public DecryptionException(String text) {super(text);}
}
