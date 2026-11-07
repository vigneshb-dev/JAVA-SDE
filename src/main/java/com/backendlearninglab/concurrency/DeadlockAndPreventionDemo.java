package com.backendlearninglab.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CONCURRENCY: Deadlocks, Diagnosis, and Prevention Strategies.
 * <p>
 * RUNNABLE: Contains its own main() method.
 * <p>
 * LEARNING OBJECTIVES:
 * 1. Understand what causes a Deadlock (The 4 Coffman Conditions: Mutual Exclusion, Hold & Wait, No Preemption, Circular Wait).
 * 2. Observe how circular lock ordering causes threads to freeze indefinitely.
 * 3. Master Prevention Technique #1: Strict Global Lock Ordering.
 * 4. Master Prevention Technique #2: Lock Timeouts with ReentrantLock.tryLock().
 */
public class DeadlockAndPreventionDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DEADLOCK DEMONSTRATION & PREVENTION ===");

        // We run the prevention techniques directly so the process does not hang indefinitely.
        // (The deadlock code is provided with an explanation of how it creates Circular Wait).
        demonstrateDeadlockPreventionByGlobalLockOrdering();
        demonstrateDeadlockPreventionByTryLockTimeout();
    }

    /**
     * HOW A DEADLOCK HAPPENS (CIRCULAR WAIT):
     * <pre>
     * Thread-1 holds Lock A, wants Lock B.
     * Thread-2 holds Lock B, wants Lock A.
     * Both wait forever. Neither releases their lock.
     * </pre>
     * <p>
     * Educational Code Snippet (Do not run in production tests as it hangs the process):
     * <pre>
     * Object lockA = new Object();
     * Object lockB = new Object();
     *
     * Thread t1 = new Thread(() -> {
     *     synchronized(lockA) {
     *         Thread.sleep(50);
     *         synchronized(lockB) { ... }
     *     }
     * });
     * Thread t2 = new Thread(() -> {
     *     synchronized(lockB) {
     *         Thread.sleep(50);
     *         synchronized(lockA) { ... }
     *     }
     * });
     * </pre>
     */

    // -------------------------------------------------------------------------
    // PREVENTION TECHNIQUE 1: STRICT GLOBAL LOCK ORDERING
    // -------------------------------------------------------------------------
    // RULE: If every thread in the entire system acquires locks in the EXACT SAME ORDER
    // (e.g. always lock the lower ID account first, then the higher ID account),
    // a Circular Wait condition is MATHEMATICALLY IMPOSSIBLE to form!
    private static void demonstrateDeadlockPreventionByGlobalLockOrdering() throws InterruptedException {
        System.out.println("\n[1] Deadlock Prevention via Global Lock Ordering (Canonical IDs):");

        Account acc1 = new Account(101, 1000);
        Account acc2 = new Account(202, 1000);

        // Thread 1 transfers from 101 to 202
        Thread t1 = new Thread(() -> transferWithGlobalOrdering(acc1, acc2, 100), "Transfer-T1");
        // Thread 2 transfers in reverse from 202 to 101
        Thread t2 = new Thread(() -> transferWithGlobalOrdering(acc2, acc1, 200), "Transfer-T2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  Both transfers completed without deadlock!");
        System.out.println("  Account 101 balance: " + acc1.balance + ", Account 202 balance: " + acc2.balance);
    }

    private static void transferWithGlobalOrdering(Account from, Account to, int amount) {
        // Enforce canonical order based on unique Account ID
        Account firstLock = from.id < to.id ? from : to;
        Account secondLock = from.id < to.id ? to : from;

        synchronized (firstLock) {
            System.out.println("  " + Thread.currentThread().getName() + " locked first account: " + firstLock.id);
            try {
                Thread.sleep(50); // Simulate processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            synchronized (secondLock) {
                System.out.println("  " + Thread.currentThread().getName() + " locked second account: " + secondLock.id);
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    // -------------------------------------------------------------------------
    // PREVENTION TECHNIQUE 2: REENTRANTLOCK WITH TIMEOUT (tryLock)
    // -------------------------------------------------------------------------
    // RULE: Instead of blocking indefinitely waiting for a lock, use tryLock(timeout).
    // If the second lock cannot be acquired within the timeout, RELEASE THE FIRST LOCK
    // and back off! This breaks the "Hold and Wait" condition.
    private static void demonstrateDeadlockPreventionByTryLockTimeout() throws InterruptedException {
        System.out.println("\n[2] Deadlock Prevention via ReentrantLock.tryLock(timeout):");

        Lock lockA = new ReentrantLock();
        Lock lockB = new ReentrantLock();

        Thread worker = new Thread(() -> {
            try {
                if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("  Worker acquired lockA.");
                        if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("  Worker acquired lockB. Critical section safe.");
                            } finally {
                                lockB.unlock();
                            }
                        } else {
                            System.out.println("  Worker could not acquire lockB within timeout. Backing off safely!");
                        }
                    } finally {
                        lockA.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        worker.join();
    }

    static class Account {
        final int id;
        int balance;

        Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }
}
