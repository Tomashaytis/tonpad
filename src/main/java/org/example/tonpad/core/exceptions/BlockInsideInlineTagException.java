package org.example.tonpad.core.exceptions;

public class BlockInsideInlineTagException extends MarkdownException {

    public BlockInsideInlineTagException(String message) {
        super(message);
    }

    public BlockInsideInlineTagException(String message, Throwable cause) {
        super(message, cause);
    }

}
