# Senior Java Backend Interview Preparation Guide

> **"Top engineering interviewers do not test syntax memorization. They test whether you understand the mechanical trade-offs and failure modes of your architecture."**

---

## 1. Core Java & JVM Internals

### Beginner: "Why is `String` immutable in Java, and what would happen if it were mutable?"
- **Concept Path:** Memory management (String Pool) $\rightarrow$ Thread safety $\rightarrow$ Security (classloading, DB credentials).
- **The Senior Answer:**
  1. *String Pool Efficiency:* Java stores identical string literals once in the String Intern Pool. If strings were mutable, modifying `"admin"` on one thread would silently corrupt every other reference to `"admin"` across the entire JVM.
  2. *Security & Networking:* File paths, database connection URLs, and socket addresses are passed as strings. If strings were mutable, a malicious thread could alter a file path between verification and execution (Time-of-Check to Time-of-Use / TOCTOU vulnerability).
  3. *HashCode Caching:* Because `String` is immutable, its `hashCode()` is calculated once lazily and cached, providing fast $O(1)$ lookups in `HashMap`.

---

### Intermediate: "How does `HashMap` work internally, and what changed in Java 8?"
- **Concept Path:** Array of buckets $\rightarrow$ Hash function & bitwise masking $\rightarrow$ Hash collision resolution $\rightarrow$ Treeification threshold.
- **The Senior Answer:**
  - Internally, `HashMap` uses an array of `Node<K,V>` buckets (`table[]`).
  - The index is calculated as `index = (n - 1) & hash(key)`.
  - When two keys produce the same index (**Hash Collision**), Java historically stored them in a linked list. In the worst case (e.g. an adversarial hash-collision attack), lookups degraded from $O(1)$ to $O(N)$.
  - **Java 8 Enhancement:** When a bucket's linked list length exceeds `TREEIFY_THRESHOLD = 8` and table capacity is $\ge 64$, the linked list dynamically transforms into a **Red-Black Balanced Binary Search Tree**. Lookups improve from $O(N)$ to $O(\log N)$. When elements drop below 6, it untreeifies back to a linked list.

---

### Advanced: "What is the difference between `volatile` and `synchronized` in the Java Memory Model (JMM)?"
- **Concept Path:** CPU L1/L2 hardware caches $\rightarrow$ Instruction reordering $\rightarrow$ Memory barriers $\rightarrow$ Mutual exclusion.
- **The Senior Answer:**
  - `volatile` guarantees **Visibility** and **Ordering** (prevents instruction reordering by inserting memory barriers/fences). Every read reads directly from main memory (RAM); every write flushes to main memory immediately. However, `volatile` provides **NO Mutual Exclusion**. A compound operation like `count++` (which is read, modify, write) is NOT atomic and will suffer race conditions!
  - `synchronized` guarantees **Visibility**, **Ordering**, AND **Atomicity / Mutual Exclusion**. It acquires an intrinsic monitor lock on the object header (Mark Word), ensuring that only one thread can execute the critical section at a time.

---

## 2. Relational Databases & Pure JDBC

### Beginner: "Why is `PreparedStatement` safer than `Statement` against SQL Injection?"
- **Concept Path:** SQL parsing phase $\rightarrow$ Abstract Syntax Tree (AST) compilation $\rightarrow$ Parameter binding.
- **The Senior Answer:**
  - When you use `Statement` with string concatenation (`"WHERE user = '" + input + "'"`), the database treats the entire string as raw SQL instructions to parse. An attacker injecting `' OR '1'='1` alters the SQL syntax tree.
  - With `PreparedStatement`, the SQL syntax is compiled into an **Abstract Syntax Tree (AST)** on the database engine *first* with placeholders (`?`). The parameter values are transferred separately over the wire protocol as literal data values. The database **never evaluates the parameters as executable SQL code**, rendering injection mathematically impossible.

---

### Intermediate: "Why would you choose Pure JDBC over an ORM like Hibernate/JPA?"
- **Concept Path:** Overhead of ORM abstractions $\rightarrow$ Batch processing throughput $\rightarrow$ Complex analytical SQL $\rightarrow$ Memory footprint.
- **The Senior Answer:**
  - *When JPA is inferior:* High-volume batch ingestion (inserting 500,000 records/sec), complex reporting with window functions / CTEs, or ultra-low-latency microservices with minimal RAM budgets.
  - *Why Pure JDBC wins:* Zero reflection overhead, zero entity lifecycle tracking, zero First-Level Cache memory overhead, and full explicit control over the raw SQL query and batch sizes.

---

### Advanced: "What happens mechanically when a database index is used, and why can an index hurt performance?"
- **Concept Path:** Disk block reads $\rightarrow$ B-Tree node navigation $\rightarrow$ Heap tuple pointer $\rightarrow$ Page split on write.
- **The Senior Answer:**
  - A B-Tree index keeps keys sorted in 8KB disk blocks. A query executes a binary search through the tree nodes ($O(\log N)$ page reads) to locate the row's physical tuple ID (`ctid`), avoiding reading thousands of table pages.
  - *Why it hurts:* Indexes are not free. Every `INSERT`, `UPDATE`, or `DELETE` must synchronously write to the table heap AND update every B-Tree index on that table. If an index block is full, the engine must perform an expensive **B-Tree Page Split**. Over-indexing destroys write throughput.

---

## 3. Spring Boot, Spring MVC & IoC

### Beginner: "Why is Constructor Injection preferred over Field Injection (`@Autowired`)?"
- **Concept Path:** Immutability $\rightarrow$ Fail-fast compilation $\rightarrow$ Unit testing without reflection.
- **The Senior Answer:**
  1. *Immutability:* Dependencies can be declared `final`, guaranteeing they are never modified after bean construction.
  2. *Unit Testability:* You can instantiate the class in a JUnit test using `new MyService(mockRepo)` without starting a heavyweight Spring ApplicationContext or using reflection hacks.
  3. *Circular Dependency Detection:* Constructor injection fails fast at startup if two beans depend on each other, whereas field injection hides circular dependencies until runtime.

---

### Intermediate: "How does an HTTP request travel from the network socket to your `@RestController` method?"
- **Concept Path:** OS TCP socket $\rightarrow$ Tomcat Connector $\rightarrow$ Servlet Filter Chain $\rightarrow$ `DispatcherServlet` $\rightarrow$ `HandlerMapping` $\rightarrow$ `HandlerAdapter` $\rightarrow$ Controller.
- **The Senior Answer:**
  1. Tomcat accepts the TCP connection and wraps the HTTP payload into an `HttpServletRequest`.
  2. Request traverses the Servlet Filter pipeline (CorsFilter, Spring Security's `FilterChainProxy`).
  3. Request enters Spring MVC's front controller: **`DispatcherServlet`**.
  4. `DispatcherServlet` calls `HandlerMapping` to find which `@RestController` matches the URL.
  5. `DispatcherServlet` delegates to `HandlerAdapter` (`RequestMappingHandlerAdapter`), which uses `HttpMessageConverter` (Jackson) to deserialize the JSON request body into a Java DTO.
  6. The controller method executes and returns a response DTO, which Jackson serializes back to JSON.

---

## 4. JPA & Hibernate Internals

### Beginner: "What is the N+1 Query Problem and how do you eliminate it?"
- **Concept Path:** `FetchType.LAZY` proxies $\rightarrow$ Loop execution $\rightarrow$ Network round-trip penalty.
- **The Senior Answer:**
  - Occurs when loading a parent entity with a lazy association (e.g., `Order` $\rightarrow$ `OrderItem`). Loading 100 orders fires **1 SQL query**. Iterating through each order to inspect its items causes Hibernate proxies to fire **100 additional queries** ($1 + N$).
  - *Fix:* Use JPQL `JOIN FETCH` or Spring Data `@EntityGraph` to instruct Hibernate to perform an SQL `INNER/LEFT JOIN` in a single query.

---

### Intermediate: "What does it mean when a JPA entity becomes 'Managed', and how does Dirty Checking work?"
- **Concept Path:** `EntityManager` $\rightarrow$ Persistence Context $\rightarrow$ Snapshot array $\rightarrow$ Flush phase.
- **The Senior Answer:**
  - When an entity is fetched within a `@Transactional` boundary, it enters the **Persistence Context** and becomes **Managed**.
  - During loading, Hibernate stores a hidden duplicate copy of the entity's property values in a **Snapshot Array**.
  - When the transaction commits, Hibernate's **Dirty Checking mechanism** compares the current entity values against the initial snapshot. If any property changed, Hibernate generates and executes the appropriate SQL `UPDATE` statement automatically. You do NOT need to call `repository.save()`.

---

## 5. Spring Security & Architecture

### Intermediate: "What happens inside the Spring Security filter chain during a request?"
- **Concept Path:** `DelegatingFilterProxy` $\rightarrow$ `FilterChainProxy` $\rightarrow$ `SecurityContextHolder` $\rightarrow$ `ThreadLocal` $\rightarrow$ `AuthorizationFilter`.
- **The Senior Answer:**
  - The servlet container forwards requests to `DelegatingFilterProxy`, which routes execution into Spring's `FilterChainProxy`.
  - The request passes through 15+ ordered security filters.
  - Our custom `JwtAuthenticationFilter` extracts the `Bearer <token>`, validates the cryptographic HMAC signature, and populates `SecurityContextHolder.getContext().setAuthentication(token)`.
  - Downstream, `AuthorizationFilter` checks URI rules (`hasRole('ADMIN')`).
  - If unauthenticated, `AuthenticationEntryPoint` returns HTTP 401; if unauthorized, `AccessDeniedHandler` returns HTTP 403.
  - At the end of the request, `SecurityContextHolderFilter` cleans up the `ThreadLocal` to prevent memory leaks in pooled worker threads.

---

## 6. Apache Kafka & Distributed Systems

### Intermediate: "Why can Kafka consumers process the same event more than once?"
- **Concept Path:** At-least-once delivery $\rightarrow$ Offset commit timing $\rightarrow$ Consumer rebalances.
- **The Senior Answer:**
  - Kafka guarantees **At-Least-Once Delivery** by default.
  - If a consumer finishes processing an event (e.g. reserves inventory) but crashes, experiences a network timeout, or triggers a rebalance before successfully sending its offset commit to the broker, Kafka assumes the record was never processed.
  - The partition is reassigned, and the new consumer re-reads the event from the last committed offset, receiving a duplicate!
  - *Architectural Requirement:* Consumers must be implemented **idempotently** using a deduplication table or unique constraint.

---

### Advanced: "What is the Dual-Write Problem, and how does the Transactional Outbox Pattern solve it?"
- **Concept Path:** Two independent distributed systems $\rightarrow$ Lack of distributed 2PC transactions $\rightarrow$ Partial failure.
- **The Senior Answer:**
  - Occurs when an application must update a relational database (PostgreSQL) AND publish a message to an event bus (Kafka). There is no native distributed ACID transaction between PostgreSQL and Kafka.
  - If the database commits but Kafka is down, the event is permanently lost. If Kafka is published first but the database rolls back, phantom events trigger invalid actions in downstream services.
  - *The Outbox Solution:* Write the domain mutation AND an `OutboxEventEntity` into the **same local PostgreSQL transaction**. Since both writes share the same local ACID boundary, either both commit or both roll back. A separate background worker reads pending outbox events and reliably publishes them to Kafka with retry safety!
