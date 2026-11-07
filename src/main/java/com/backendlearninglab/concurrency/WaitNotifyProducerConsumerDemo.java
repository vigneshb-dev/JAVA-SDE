package com.backendlearninglab.concurrency;

import java.util.LinkedList;
import java.util.Queue;

/**
 * CONCURRENCY: Low-level Thread Signaling with wait(), notify(), and notifyAll().
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand how threads communicate without busy-waiting (spinning CPU).
 * 2. Understand why wait() and notify() MUST be called inside a synchronized block on that exact monitor.
 * 3. Master the Spurious Wakeup rule: Why wait() MUST ALWAYS be enclosed in a `while` loop, NEVER `if`!
 * 4. Understand notify() (wakes one arbitrary thread) vs. notifyAll() (wakes all waiting threads).
 */
public class WaitNotifyProducerConsumerDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LOW-LEVEL PRODUCER-CONSUMER (wait & notifyAll) ===");

        BoundedBuffer<String> queue = new BoundedBuffer<>(3);

        // Producer Thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    String order = "ORDER-" + i;
                    queue.put(order);
                    System.out.println("  [PRODUCER] Created: " + order);
                    Thread.sleep(150); // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer-Thread");

        // Consumer Thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    String order = queue.take();
                    System.out.println("  [CONSUMER] Processed: " + order);
                    Thread.sleep(300); // Consumer is slower to demonstrate buffer filling up
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        System.out.println("  All orders processed successfully!");
    }

    /**
     * BOUNDED BUFFER IMPLEMENTATION
     */
    public static class BoundedBuffer<T> {
        private final Queue<T> buffer = new LinkedList<>();
        private final int capacity;

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        public synchronized void put(T item) throws InterruptedException {
            /*
             * CRITICAL LEARNING: WHY 'while' INSTEAD OF 'if'?
             * 
             * Bad Code:
             * if (buffer.size() == capacity) {
             *     wait();
             * }
             * 
             * Dangers of 'if':
             * 1. Spurious Wakeups: The OS / JVM can wake up a waiting thread even if NO notify() was called!
             * 2. Race on Wakeup: If 2 producers were waiting and 1 item is consumed, BOTH wake up.
             *    The first producer grabs the lock, adds an item, and fills the buffer.
             *    If the second producer used 'if', it blindly resumes without re-checking,
             *    adding an item and OVERFLOWING the buffer!
             * 
             * RULE: ALWAYS test the wait condition in a WHILE loop!
             */
            while (buffer.size() == capacity) {
                // Buffer is full. Release lock and sleep until consumer takes an item
                wait();
            }

            buffer.add(item);

            // Notify waiting consumers that data is now available
            notifyAll();
        }

        public synchronized T take() throws InterruptedException {
            while (buffer.isEmpty()) {
                // Buffer is empty. Release lock and sleep until producer adds an item
                wait();
            }

            T item = buffer.remove();

            // Notify waiting producers that space is now available
            notifyAll();
            return item;
        }
    }

    /*
     * WHEN TO USE wait() / notifyAll():
     * Rarely in modern business applications!
     * Modern Java provides high-level abstractions in java.util.concurrent (like BlockingQueue,
     * CountDownLatch, Semaphore) that solve these problems safely without risky monitor mechanics.
     * 
     * COMMON MISTAKE:
     * Calling wait() without holding the monitor lock throws:
     *   java.lang.IllegalMonitorStateException
     */
}
