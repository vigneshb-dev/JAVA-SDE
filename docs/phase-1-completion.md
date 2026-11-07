# Phase 1 Completion Checklist: Manual Mastery Verification

Before moving to **Phase 2 (Spring Boot, Spring MVC, Spring Security, JPA/Hibernate)**, you MUST be able to complete every item on this checklist manually without relying on AI code generation or copying and pasting.

---

## 1. Core Java & Object-Oriented Fundamentals

- [ ] **Encapsulation:** Can you explain why mutable fields should never be `public` and write defensive getters/setters?
- [ ] **Constructors:** Can you explain the difference between a default no-args constructor and an all-args canonical constructor?
- [ ] **Polymorphism:** Can you explain how dynamic method dispatch works at the JVM level using `invokevirtual` and the vtable?
- [ ] **Interfaces vs. Abstract Classes:** Can you describe when an interface is preferable to an abstract class (and vice versa)?
- [ ] **`equals()` and `hashCode()` Contract:** Can you explain why breaking this contract causes items to disappear inside a `HashSet` or `HashMap`?
- [ ] **Immutability:** Can you list the 4 rules required to make a Java class truly immutable?
- [ ] **Strings:** Can you explain why string concatenation (`+=`) in a loop creates $O(N^2)$ memory garbage and why `StringBuilder` is $O(N)$?
- [ ] **Autoboxing Traps:** Can you explain why `Integer.valueOf(500) == Integer.valueOf(500)` evaluates to `false` while `100 == 100` evaluates to `true`?
- [ ] **Generics & PECS:** Can you explain what "Producer Extends, Consumer Super" means and when to use `? extends T` vs. `? super T`?
- [ ] **Modern Streams & Optional:** Can you write a Stream pipeline with `filter`, `map`, and `collect`, and explain why calling `optional.get()` without `isPresent()` is an anti-pattern?
- [ ] **Records & Enums:** Can you write a Java `record` with a compact constructor for validation and an `enum` containing an abstract method?

---

## 2. Java Concurrency & Multithreading

- [ ] **Thread vs. Runnable:** Can you explain why calling `thread.run()` does NOT create a new thread while `thread.start()` does?
- [ ] **Race Conditions:** Can you explain what instructions the CPU executes during `count++` and why lost updates happen?
- [ ] **`synchronized` vs. `volatile`:** Can you explain why `volatile` guarantees visibility but NOT atomicity?
- [ ] **`wait()` and `notifyAll()`:** Can you explain what a "spurious wakeup" is and why `wait()` must ALWAYS be inside a `while` loop?
- [ ] **Thread Pools (`ExecutorService`):** Can you explain the danger of unbound thread creation (`OutOfMemoryError: unable to create native thread`)?
- [ ] **`Callable` & `Future`:** Can you explain why `future.get()` should always have a timeout in production backends?
- [ ] **`CompletableFuture`:** Can you chain two parallel asynchronous tasks using `thenCombine()`?
- [ ] **Deadlocks:** Can you name the 4 Coffman conditions and explain how Strict Global Lock Ordering prevents circular wait?

---

## 3. Maven Build System

- [ ] **Coordinates:** Can you explain the purpose of `groupId`, `artifactId`, and `version` (GAV)?
- [ ] **Dependency Scopes:** Can you explain the difference between `compile`, `test`, `provided`, and `runtime` scopes?
- [ ] **Build Lifecycles:** Can you list the default phases (`validate` -> `compile` -> `test` -> `package` -> `verify` -> `install` -> `deploy`) in order?
- [ ] **Transitive Dependencies:** Can you use `mvn dependency:tree` to detect version conflicts and explain the "Nearest-Wins" rule?

---

## 4. PostgreSQL & Relational Modeling

- [ ] **Constraints:** Can you write a SQL schema utilizing `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL`, `UNIQUE`, and `CHECK` constraints?
- [ ] **Third Normal Form (3NF):** Can you explain why `order_items` stores `unit_price` explicitly rather than looking it up dynamically from `products`?
- [ ] **B-Tree Indexes:** Can you explain how a B-Tree operates in $O(\log N)$ and why indexes accelerate `SELECT` but degrade `INSERT`/`UPDATE` throughput?
- [ ] **`EXPLAIN ANALYZE`:** Can you distinguish between a `Seq Scan` (Sequential Scan), an `Index Scan`, and an `Index Only Scan`?
- [ ] **ACID Guarantees:** Can you define Atomicity, Consistency, Isolation, and Durability in the context of an e-commerce checkout?

---

## 5. Pure JDBC (Without Spring)

- [ ] **Architecture:** Can you explain the relationship between `DriverManager`, `Connection`, `Statement`, and `ResultSet`?
- [ ] **SQL Injection:** Can you demonstrate how `' OR '1'='1` bypasses login using string concatenation and explain why `PreparedStatement` pre-compilation makes injection impossible?
- [ ] **Resource Management:** Can you explain what a "connection leak" is and use `try-with-resources` to ensure deterministic socket closure?
- [ ] **Manual Transactions:** Can you manage a multi-table transaction using `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()` inside a `try-catch-finally` block?
- [ ] **Batch Processing:** Can you explain why `addBatch()` and `executeBatch()` are superior to running 1,000 separate `executeUpdate()` statements?
- [ ] **Connection Pooling (HikariCP):** Can you explain why creating a physical database connection per HTTP request is a performance anti-pattern?

---

## Verification Task (The "No Code" Exam)

To prove you have completed Phase 1:
1. Open a blank text file or paper.
2. Without looking at this project's code, sketch out the JDBC code required to transfer \$100 between two bank accounts atomically with rollback on failure.
3. If you can write the connection setup, transaction disabling, two prepared statements, commit, catch block rollback, and resource closure cleanly, **you are officially ready for Phase 2 (Spring Boot & JPA)**.
