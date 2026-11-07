package com.backendlearninglab.exception;

/**
 * Thrown when an order fails domain validation (e.g. empty items, invalid customer, negative total).
 */
public class InvalidOrderException extends OrderFlowException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
