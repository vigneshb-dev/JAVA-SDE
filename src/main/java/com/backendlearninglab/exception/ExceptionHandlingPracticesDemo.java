package com.backendlearninglab.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

/**
 * EDUCATIONAL DEMONSTRATION: Exception Handling Anti-Patterns vs. Professional Engineering.
 * <p>
 * RUNNABLE: You can run this class directly with `java` or via Maven.
 */
public class ExceptionHandlingPracticesDemo {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingPracticesDemo.class);

    public static void main(String[] args) {
        System.out.println("=== EXCEPTION HANDLING MASTERCLASS ===");

        demonstrateAntiPattern1_SwallowingException();
        demonstrateAntiPattern2_PrintStackTraceAndContinue();
        demonstrateAntiPattern3_CatchingThrowableOrGenericException();
        demonstrateProfessionalHandling();
    }

    // -------------------------------------------------------------------------
    // ANTI-PATTERN 1: Swallowing Exceptions (The Black Hole)
    // -------------------------------------------------------------------------
    // WHAT HAPPENS: The error occurs, is completely ignored, and execution continues.
    // WHY IT'S DEADLY: The program state is now silently corrupt. The bug will surface
    // 10,000 lines later as a baffling NullPointerException with no stack trace of the root cause.
    private static void demonstrateAntiPattern1_SwallowingException() {
        System.out.println("\n[ANTI-PATTERN 1] Swallowing Exceptions:");
        try {
            Integer.parseInt("NotANumber");
        } catch (NumberFormatException e) {
            // EMPTY CATCH BLOCK: NEVER DO THIS!
        }
        System.out.println("  -> System kept going blindly despite corrupted input!");
    }

    // -------------------------------------------------------------------------
    // ANTI-PATTERN 2: e.printStackTrace() and Continue
    // -------------------------------------------------------------------------
    // WHAT HAPPENS: Prints text to System.err, but caller has no idea an error happened.
    // WHY IT'S BAD:
    // 1. System.err bypasses logging frameworks (no timestamps, cannot send to Datadog/Elasticsearch).
    // 2. Caller receives null or partial data, leading to cascading failures downstream.
    private static void demonstrateAntiPattern2_PrintStackTraceAndContinue() {
        System.out.println("\n[ANTI-PATTERN 2] e.printStackTrace() and Continue:");
        try {
            simulateFailingDatabaseQuery();
        } catch (SQLException e) {
            // BAD CODE:
            System.out.println("  -> Bad code caught it and just dumped to console: (suppressed for demo)");
            // e.printStackTrace(); 
            // Application continues in an invalid state!
        }
    }

    // -------------------------------------------------------------------------
    // ANTI-PATTERN 3: Catching java.lang.Throwable or Exception
    // -------------------------------------------------------------------------
    // WHY IT'S BAD: Catches VirtualMachineError, OutOfMemoryError, ThreadDeath.
    // When the JVM is running out of memory, catching Throwable prevents JVM recovery or clean shutdown!
    private static void demonstrateAntiPattern3_CatchingThrowableOrGenericException() {
        System.out.println("\n[ANTI-PATTERN 3] Catching java.lang.Throwable:");
        try {
            // Risky operation
            String text = null;
            text.length();
        } catch (Throwable t) { // BAD: Never catch Throwable
            System.out.println("  -> Caught Throwable indiscriminately! Masked a NullPointerException.");
        }
    }

    // -------------------------------------------------------------------------
    // PROFESSIONAL PATTERN: Explicit, Logged, Chained, and Actionable
    // -------------------------------------------------------------------------
    // 1. Catch specific exceptions.
    // 2. Log contextual information (what were we trying to do? which user/order?).
    // 3. Preserve the original root cause by passing 'e' as the cause in new CustomException(msg, e).
    // 4. Use try-with-resources to guarantee resource cleanup.
    private static void demonstrateProfessionalHandling() {
        System.out.println("\n[PROFESSIONAL PATTERN] Exception Translation & Context Preservation:");
        try {
            fetchOrderFromDatabaseSafely(9999L);
        } catch (DatabaseOperationException e) {
            log.error("Safe handler caught translated exception: {}", e.getMessage());
            log.info("Original root cause was preserved: {}", e.getCause().getClass().getName());
        }
    }

    private static void fetchOrderFromDatabaseSafely(Long orderId) {
        try {
            simulateFailingDatabaseQuery();
        } catch (SQLException e) {
            // PROFESSIONAL: Wrap low-level SQL technical failure into domain exception
            // Notice we pass 'e' as the second argument - this maintains the entire causal chain!
            throw new DatabaseOperationException("Failed to query order with id: " + orderId, e);
        }
    }

    private static void simulateFailingDatabaseQuery() throws SQLException {
        throw new SQLException("Connection refused: PostgreSQL socket /tmp/.s.PGSQL.5432 closed unexpectedly", "08006", 101);
    }
}
