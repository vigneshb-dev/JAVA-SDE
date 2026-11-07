package com.backendlearninglab.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CONCURRENCY: Thread Pools (ExecutorService), Callable, and Future.
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand why manually creating new Thread() per task destroys servers (Thread Leaks, OOM).
 * 2. Understand Callable<V> (returns a value & throws checked exceptions) vs. Runnable (void, no checked exceptions).
 * 3. Master Future<V>: Handling timeouts, cancellation, and exceptions.
 * 4. Thread Pool Sizing: CPU-bound vs. I/O-bound formulas.
 */
public class ExecutorAndFutureDemo {

    public static void main(String[] args) {
        System.out.println("=== EXECUTORSERVICE, CALLABLE & FUTURE ===");

        demonstrateCallableAndFuture();
        explainThreadPoolSizing();
    }

    /**
     * WHY USE EXECUTORSERVICE INSTEAD OF new Thread()?
     * 1. Creating an OS thread is EXPENSIVE: Allocates ~1MB of stack memory, requires kernel context switch.
     * 2. Unbounded Thread Creation Attack: 10,000 incoming web requests = 10,000 threads -> OutOfMemoryError.
     * 3. ExecutorService maintains a controlled pool of reusable worker threads.
     */
    private static void demonstrateCallableAndFuture() {
        System.out.println("\n[1] Submitting Callable to ExecutorService:");

        // Create a fixed pool with 3 worker threads
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // Callable returns a calculated result (Double) and can throw checked exceptions
            Callable<Double> calculateTaxTask = () -> {
                System.out.println("  Calculating tax on thread: " + Thread.currentThread().getName());
                Thread.sleep(400); // Simulate external tax calculation service
                return 18.50;
            };

            // Submit returns a Future (a promise of a future result)
            Future<Double> taxFuture = executor.submit(calculateTaxTask);

            System.out.println("  Task submitted. Main thread can continue doing other work...");

            // Blocking get with strict TIMEOUT (Crucial backend defensive practice!)
            // NEVER use future.get() without a timeout in production!
            try {
                Double tax = taxFuture.get(2, TimeUnit.SECONDS);
                System.out.println("  Received calculated tax from Future: $" + tax);
            } catch (TimeoutException e) {
                System.err.println("  Tax service timed out! Cancelling task...");
                taxFuture.cancel(true);
            } catch (ExecutionException e) {
                // If the Callable threw an exception, it is wrapped in an ExecutionException
                System.err.println("  Tax task failed with root cause: " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } finally {
            // ALWAYS shut down thread pools, otherwise the JVM will NOT terminate!
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force interrupt pending tasks
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            System.out.println("  ExecutorService cleanly shut down.");
        }
    }

    /**
     * THREAD POOL SIZING FORMULA (PRODUCTION ENGINEERING):
     * <p>
     * 1. CPU-Bound Tasks (Cryptography, Image processing, Complex math):
     *    Pool Size = Number of CPU Cores (or Cores + 1)
     *    Why: More threads than cores causes CPU thrashing (wasting cycles on context switches).
     * <p>
     * 2. I/O-Bound Tasks (Database queries, REST API calls, File read/write):
     *    Pool Size = Cores * (1 + Wait_Time / CPU_Service_Time)
     *    Example: If a DB call spends 90ms waiting on the network and 10ms on CPU,
     *    Wait / Service = 9. On an 8-core machine: 8 * (1 + 9) = 80 threads.
     */
    private static void explainThreadPoolSizing() {
        System.out.println("\n[2] Hardware Sizing Guidelines:");
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("  Available CPU cores on this machine: " + availableCores);
        System.out.println("  Recommended pool for CPU-bound tasks: " + availableCores);
        System.out.println("  Recommended pool for typical I/O-bound tasks: " + (availableCores * 4) + " - " + (availableCores * 10));
    }
}
