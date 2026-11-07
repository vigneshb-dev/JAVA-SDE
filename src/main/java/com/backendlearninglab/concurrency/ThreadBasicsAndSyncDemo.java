package com.backendlearninglab.concurrency;

/**
 * CONCURRENCY: Thread, Runnable, Synchronized, and Volatile.
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand Thread vs. Runnable (Separation of task from execution mechanism).
 * 2. Understand Race Conditions and Critical Sections.
 * 3. Understand synchronized intrinsic locks (monitors) and performance overhead.
 * 4. Understand volatile: Visibility guarantee vs. lack of atomicity.
 */
public class ThreadBasicsAndSyncDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== THREAD, RUNNABLE, SYNCHRONIZED & VOLATILE ===");

        demonstrateThreadAndRunnable();
        demonstrateRaceConditionAndSynchronized();
        demonstrateVolatileVisibility();
    }

    // -------------------------------------------------------------------------
    // 1. THREAD VS RUNNABLE
    // -------------------------------------------------------------------------
    // WHEN TO USE: Always prefer implementing Runnable over extending Thread.
    // WHY: Java only allows single inheritance. Extending Thread burns your one inheritance slot
    // and tightly couples the work (what to run) with the worker (how to run).
    // COMMON MISTAKE: Calling run() instead of start(). Calling run() executes synchronously
    // on the CALLER thread, creating NO new thread!
    private static void demonstrateThreadAndRunnable() throws InterruptedException {
        System.out.println("\n[1] Thread vs. Runnable:");

        // Runnable represents the TASK
        Runnable orderPackingTask = () -> {
            System.out.println("  Packing task running on worker thread: " + Thread.currentThread().getName());
        };

        // Thread represents the WORKER
        Thread workerThread = new Thread(orderPackingTask, "OrderPacker-1");
        workerThread.start(); // Spawns new OS thread
        workerThread.join();  // Main thread waits for workerThread to complete
    }

    // -------------------------------------------------------------------------
    // 2. RACE CONDITION & SYNCHRONIZED
    // -------------------------------------------------------------------------
    // SCENARIO: Two threads attempt to increment an inventory count simultaneously.
    // WHAT HAPPENS WITHOUT SYNCHRONIZATION:
    // count++ is NOT atomic! It comprises 3 distinct CPU instructions:
    //   1. READ current count from memory into CPU register.
    //   2. INCREMENT register value.
    //   3. WRITE new value back to memory.
    // When 2 threads interleave, both read 10, both increment to 11, and write 11.
    // One increment is silently LOST (Lost Update race condition)!
    //
    // WHEN TO USE synchronized: When protecting shared mutable state across threads.
    // WHEN NOT TO USE: Don't synchronize long-running I/O or network calls (blocks other threads).
    // PERFORMANCE IMPLICATION: Acquiring monitors causes thread parking, context switching,
    // and inhibits JVM compiler optimizations. Keep synchronized blocks as short as possible!
    private static void demonstrateRaceConditionAndSynchronized() throws InterruptedException {
        System.out.println("\n[2] Synchronized: Preventing Lost Updates:");

        InventoryCounter counter = new InventoryCounter();

        // Spawn 2 threads each incrementing 1,000 times
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter.incrementSynchronized();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter.incrementSynchronized();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  Final synchronized inventory count: " + counter.getCount() + " (Expected: 2000)");
    }

    // -------------------------------------------------------------------------
    // 3. VOLATILE: VISIBILITY GUARANTEE
    // -------------------------------------------------------------------------
    // WHAT IT DOES: Guarantees that writes to a variable are IMMEDIATELY visible to all other threads
    // by flushing CPU L1/L2 hardware caches and bypassing local register caching.
    // WHAT IT DOES NOT DO: volatile does NOT guarantee ATOMICITY!
    // (volatile int x; x++ is STILL vulnerable to race conditions!).
    // WHEN TO USE: For boolean status flags (e.g. shutdown requested).
    // WHEN NOT TO USE: When new value depends on old value (use AtomicInteger instead).
    private static void demonstrateVolatileVisibility() throws InterruptedException {
        System.out.println("\n[3] Volatile: Cross-Thread Visibility Flag:");

        WorkerLoop worker = new WorkerLoop();
        Thread thread = new Thread(worker, "LoopWorker");
        thread.start();

        Thread.sleep(100); // Let worker spin for 100ms
        System.out.println("  Main thread signaling stop...");
        worker.stopWorking(); // Writes to volatile boolean
        thread.join();
        System.out.println("  Worker thread stopped cleanly!");
    }

    static class InventoryCounter {
        private int count = 0;

        // Synchronized method locks on 'this' monitor
        public synchronized void incrementSynchronized() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    static class WorkerLoop implements Runnable {
        // 'volatile' ensures changes made by main thread are immediately observed here
        private volatile boolean running = true;

        @Override
        public void run() {
            long iterations = 0;
            while (running) {
                iterations++;
            }
            System.out.println("  Worker finished after " + iterations + " iterations.");
        }

        public void stopWorking() {
            this.running = false;
        }
    }
}
