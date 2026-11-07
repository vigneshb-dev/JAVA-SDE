# Concurrency & Multithreading Learning Demos

The runnable Concurrency laboratories are located in `src/main/java/com/backendlearninglab/concurrency/`:

| Module | Location | Concepts Covered |
| :--- | :--- | :--- |
| **Threads & Monitors** | `concurrency/ThreadBasicsAndSyncDemo.java` | Thread lifecycle, `synchronized` intrinsic locks, monitor acquisition, and race condition reproduction. |
| **Volatile & Visibility** | `concurrency/VolatileAndVisibilityDemo.java` | CPU L1/L2 hardware caches, JMM happens-before relationship, instruction reordering, memory barriers, and when `volatile` is insufficient (non-atomic compound operations like `count++`). |
| **Coordination** | `concurrency/WaitNotifyProducerConsumerDemo.java` | `wait()` vs `sleep()`, `notify()` vs `notifyAll()`, spurious wakeups (why `while (!condition)` is mandatory instead of `if`), and thread signalling. |
| **Thread Pools** | `concurrency/ExecutorAndFutureDemo.java` | `ThreadPoolExecutor` internal architecture (`corePoolSize`, `maximumPoolSize`, `BlockingQueue`, `RejectedExecutionHandler`), `Callable<T>`, and `Future<T>`. |
| **Asynchronous Pipelines** | `concurrency/CompletableFutureAsyncDemo.java` | Non-blocking async composition (`thenApplyAsync`, `thenCompose`, `thenCombine`), exception handling (`exceptionally`, `handle`), and thread decoupling. |
| **Concurrent Collections** | `concurrency/ConcurrentCollectionsDemo.java` | `ConcurrentHashMap` bucket-level striping / CAS vs synchronized map, `CopyOnWriteArrayList`, and thread-safe lock-free data structures. |
| **Deadlock & Prevention** | `concurrency/DeadlockAndPreventionDemo.java` | Reproducing Coffman's 4 deadlock conditions (Mutual exclusion, Hold and wait, No preemption, Circular wait) and preventing them via Global Resource Ordering. |

### How to Run Any Concurrency Demo:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.ThreadBasicsAndSyncDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.VolatileAndVisibilityDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.WaitNotifyProducerConsumerDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.ExecutorAndFutureDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.CompletableFutureAsyncDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.ConcurrentCollectionsDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.concurrency.DeadlockAndPreventionDemo"
```
