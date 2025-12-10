package org.example.tonpad.core.exceptions;

public class FingerPrintException extends RuntimeException {
    public FingerPrintException(String message) {
        super(message);
    }
    
    public FingerPrintException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FingerPrintException(Throwable cause) {
        super(cause);
    }
}
