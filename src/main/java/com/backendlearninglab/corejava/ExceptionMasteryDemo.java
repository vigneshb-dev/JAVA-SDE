package com.backendlearninglab.corejava;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * CORE JAVA: Exception Hierarchy, Checked vs. Unchecked, Try-With-Resources, and Assertions.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class ExceptionMasteryDemo {

    public static void main(String[] args) {
        System.out.println("=== EXCEPTION HIERARCHY & CONTROL FLOW ===");

        demonstrateExceptionHierarchy();
        demonstrateTryCatchFinallyExecutionOrder();
        demonstrateTryWithResourcesAutoClose();
        demonstrateJavaAssertions();
    }

    /**
     * EXCEPTION HIERARCHY:
     * <pre>
     *                    java.lang.Throwable
     *                       /           \
     *           java.lang.Error      java.lang.Exception
     *          (Unrecoverable JVM)    /               \
     *        e.g. OutOfMemoryError   /                 \
     *            StackOverflowError  Checked         java.lang.RuntimeException
     *                              (Must declare    (Unchecked: programming bugs)
     *                               or catch)       e.g. NullPointerException
     *                         e.g. IOException          IllegalArgumentException
     *                              SQLException         IndexOutOfBoundsException
     * </pre>
     */
    private static void demonstrateExceptionHierarchy() {
        System.out.println("\n[1] Checked vs. Unchecked Exceptions:");

        // Unchecked: Compiler does NOT force try/catch
        try {
            int result = 100 / 0; // ArithmeticException extends RuntimeException
        } catch (ArithmeticException e) {
            System.out.println("  Caught unchecked ArithmeticException: " + e.getMessage());
        }

        // Checked: Compiler forces method to either handle with try/catch or declare with 'throws'
        try {
            simulateNetworkIo();
        } catch (java.io.IOException e) {
            System.out.println("  Caught checked IOException: " + e.getMessage());
        }
    }

    /**
     * TRY - CATCH - FINALLY CONTROL FLOW:
     * The `finally` block ALWAYS executes, whether an exception occurred or not,
     * even if the try or catch block contains a `return` statement!
     * (The only exceptions are System.exit(0) or JVM crash).
     */
    private static void demonstrateTryCatchFinallyExecutionOrder() {
        System.out.println("\n[2] Try-Catch-Finally Execution Order:");
        System.out.println("  Method returned value: " + testFinallyFlow());
    }

    private static String testFinallyFlow() {
        try {
            System.out.println("    1. Inside try block");
            throw new RuntimeException("Simulated error in try");
        } catch (RuntimeException e) {
            System.out.println("    2. Inside catch block: handling " + e.getMessage());
            return "ValueFromCatch";
        } finally {
            // FINALLY ALWAYS RUNS BEFORE THE METHOD RETURNS
            System.out.println("    3. Inside finally block: guarantees cleanup executed!");
        }
    }

    /**
     * TRY-WITH-RESOURCES (Java 7+):
     * Eliminates manual finally blocks for anything implementing java.lang.AutoCloseable.
     */
    private static void demonstrateTryWithResourcesAutoClose() {
        System.out.println("\n[3] Try-With-Resources:");

        String sampleData = "Line 1: Item #1\nLine 2: Item #2";
        try (BufferedReader reader = new BufferedReader(new StringReader(sampleData))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  Read: " + line);
            }
        } catch (java.io.IOException e) {
            System.out.println("  I/O Error: " + e.getMessage());
        }
        System.out.println("  Reader closed automatically when leaving block!");
    }

    /**
     * JAVA ASSERTIONS:
     * Used for internal sanity checks during development/testing.
     * Enabled via VM flag: `java -ea ...` (enable assertions).
     * Disabled by default in production for zero performance overhead.
     */
    private static void demonstrateJavaAssertions() {
        System.out.println("\n[4] Assertions (Enabled with VM option -ea):");
        int orderQuantity = 5;
        // Syntax: assert <condition> : <failure message>;
        assert orderQuantity > 0 : "Order quantity must be positive";
        System.out.println("  Assertion passed: orderQuantity is " + orderQuantity);
    }

    private static void simulateNetworkIo() throws java.io.IOException {
        throw new java.io.IOException("Socket timeout waiting for remote payment gateway");
    }
}
