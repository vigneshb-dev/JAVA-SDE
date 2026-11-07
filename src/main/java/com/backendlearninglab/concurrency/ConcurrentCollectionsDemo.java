package com.backendlearninglab.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * CONCURRENCY: Concurrent Collections (BlockingQueue & ConcurrentHashMap).
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand BlockingQueue: The high-level thread-safe Producer-Consumer structure.
 * 2. Understand the "Poison Pill" shutdown pattern in queue-based systems.
 * 3. Master ConcurrentHashMap: Lock-striping and Compare-And-Swap (CAS) bucket operations.
 * 4. Atomic compound operations: computeIfAbsent, merge, LongAdder.
 */
public class ConcurrentCollectionsDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CONCURRENT COLLECTIONS (BlockingQueue & ConcurrentHashMap) ===");

        demonstrateBlockingQueueProducerConsumer();
        demonstrateConcurrentHashMapAtomicOperations();
    }

    /**
     * BLOCKINGQUEUE PRODUCER-CONSUMER:
     * - put(e): Blocks if queue is full.
     * - take(): Blocks if queue is empty.
     * - Zero manual wait() or notify() calls needed! Handled safely by ReentrantLock internally.
     */
    private static void demonstrateBlockingQueueProducerConsumer() throws InterruptedException {
        System.out.println("\n[1] BlockingQueue with Poison Pill Shutdown:");

        // Bounded capacity of 2 items
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
        String POISON_PILL = "SHUTDOWN_SIGNAL";

        // Producer task
        Runnable producer = () -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String event = "OrderEvent #" + i;
                    queue.put(event); // Blocks automatically if full
                    System.out.println("  [PRODUCER] Enqueued: " + event);
                    Thread.sleep(100);
                }
                // Send Poison Pill to tell consumer to terminate cleanly
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // Consumer task
        Runnable consumer = () -> {
            try {
                while (true) {
                    String event = queue.take(); // Blocks automatically if empty
                    if (POISON_PILL.equals(event)) {
                        System.out.println("  [CONSUMER] Received Poison Pill. Shutting down worker.");
                        break;
                    }
                    System.out.println("  [CONSUMER] Processed: " + event);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(producer, "Producer");
        Thread t2 = new Thread(consumer, "Consumer");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    /**
     * CONCURRENTHASHMAP:
     * <p>
     * WHY NOT Collections.synchronizedMap(new HashMap())?
     * SynchronizedMap places a single global mutex lock over the ENTIRE map!
     * If 50 threads read/write, only 1 thread executes at a time. Throughput collapses.
     * <p>
     * HOW CONCURRENTHASHMAP WORKS:
     * - Read operations (get) are completely lock-free via volatile reads.
     * - Write operations (put) lock ONLY the specific bucket head node (TreeBin / Node) being updated!
     * - Different threads writing to different hash buckets NEVER block each other.
     */
    private static void demonstrateConcurrentHashMapAtomicOperations() throws InterruptedException {
        System.out.println("\n[2] ConcurrentHashMap Atomic Frequency Counter:");

        ConcurrentHashMap<String, LongAdder> viewCounter = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);

        // 100 concurrent requests tracking views for 3 products
        for (int i = 0; i < 100; i++) {
            final String productSku = "SKU-PROD-" + (i % 3);
            pool.submit(() -> {
                // computeIfAbsent is atomic!
                viewCounter.computeIfAbsent(productSku, k -> new LongAdder()).increment();
            });
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        viewCounter.forEach((sku, counter) -> {
            System.out.printf("  Product: %-12s | Total Views: %d%n", sku, counter.sum());
        });
    }
}
