package com.backendlearninglab.exception;

/**
 * Thrown when low-level JDBC / SQL infrastructure encounters an unrecoverable failure.
 * <p>
 * EXCEPTION TRANSLATION PATTERN:
 * Repositories catch low-level, checked java.sql.SQLException, log technical details,
 * and wrap it into DatabaseOperationException before rethrowing. This isolates callers
 * from JDBC specifics.
 */
public class DatabaseOperationException extends OrderFlowException {

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
