package com.backendlearninglab.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CONCURRENCY: Asynchronous, Non-Blocking Pipelines with CompletableFuture.
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand why Future.get() blocks threads, while CompletableFuture is event-driven (callback-based).
 * 2. Master pipeline operators: supplyAsync, thenApply, thenCompose, thenCombine, exceptionally.
 * 3. Demonstrate realistic parallel backend service aggregation (Payment + Shipping + Inventory).
 */
public class CompletableFutureAsyncDemo {

    public static void main(String[] args) {
        System.out.println("=== ASYNCHRONOUS PIPELINES WITH COMPLETABLEFUTURE ===");

        ExecutorService customExecutor = Executors.newFixedThreadPool(4);

        try {
            demonstrateAsyncOrderEnrichment(customExecutor);
            demonstrateErrorHandlingWithExceptionally(customExecutor);
        } finally {
            customExecutor.shutdown();
        }
    }

    /**
     * REAL-WORLD BACKEND SCENARIO:
     * Fetching Customer, Credit Score, and Shipping Estimate in PARALLEL,
     * then combining them into a final checkout summary without blocking worker threads.
     */
    private static void demonstrateAsyncOrderEnrichment(ExecutorService executor) {
        System.out.println("\n[1] Parallel Service Aggregation (thenCombine):");

        long start = System.currentTimeMillis();

        // Task A: Fetch payment authorization token (Takes 200ms)
        CompletableFuture<String> paymentFuture = CompletableFuture.supplyAsync(() -> {
            simulateDelay(200);
            return "AUTH_TOKEN_XYZ_99";
        }, executor);

        // Task B: Fetch shipping fee from logistics carrier (Takes 250ms)
        CompletableFuture<Double> shippingFuture = CompletableFuture.supplyAsync(() -> {
            simulateDelay(250);
            return 12.50;
        }, executor);

        // COMBINE: As soon as BOTH futures finish, combine their results into a single object!
        // Notice both ran in PARALLEL. Total time = ~250ms (not 200 + 250 = 450ms).
        CompletableFuture<String> orderSummaryFuture = paymentFuture.thenCombine(shippingFuture, (token, shippingFee) -> {
            return String.format("Order Authorized! [Token: %s | Shipping: $%.2f]", token, shippingFee);
        });

        // Pipeline chaining: thenApply transforms the result non-blockingly
        CompletableFuture<Void> finalPipeline = orderSummaryFuture.thenApply(summary -> summary.toUpperCase())
                .thenAccept(finalOutput -> System.out.println("  Final Pipeline Output: " + finalOutput));

        // Wait for pipeline completion (in a web backend, the framework handles this reactively)
        finalPipeline.join();
        System.out.println("  Total elapsed time: " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * ERROR HANDLING: exceptionally()
     * Allows resilient fallback logic if an asynchronous stage fails.
     */
    private static void demonstrateErrorHandlingWithExceptionally(ExecutorService executor) {
        System.out.println("\n[2] Resilient Error Handling (exceptionally):");

        CompletableFuture<String> riskyRecommendationService = CompletableFuture.supplyAsync(() -> {
            if (true) {
                throw new IllegalStateException("Recommendation AI service down");
            }
            return "Personalized Products: [Laptop, Headset]";
        }, executor).exceptionally(throwable -> {
            System.out.println("  Caught async failure: " + throwable.getMessage());
            // Fallback to default recommendations
            return "Fallback Default Products: [Top 10 Bestsellers]";
        });

        System.out.println("  Result with fallback: " + riskyRecommendationService.join());
    }

    private static void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
