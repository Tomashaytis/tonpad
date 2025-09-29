package org.example.tonpad.core.exceptions;

public class TonpadBaseException extends RuntimeException {

    public TonpadBaseException(String message) {
        super(message);
    }

    public TonpadBaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
