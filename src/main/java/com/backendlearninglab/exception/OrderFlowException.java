package com.backendlearninglab.exception;

/**
 * Base unchecked exception for all domain business errors in OrderFlow.
 * <p>
 * WHY UNCHECKED (RuntimeException)?
 * Modern Java backends favor unchecked exceptions for business rule violations.
 * Checked exceptions (extending Exception) force callers to declare 'throws' on every method
 * up the call stack, polluting API contracts and leaking implementation details.
 * Unchecked exceptions cleanly propagate up to transaction boundaries or web exception handlers.
 */
public class OrderFlowException extends RuntimeException {

    public OrderFlowException(String message) {
        super(message);
    }

    public OrderFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
