# Java Backend Engineering Skills Checklist

> **"Track your progress through the 11 technology tiers. Check off each skill only after you can explain its internal mechanism and implement it from memory without relying on AI."**

---

### Level 1: Core Java Mastery
- [ ] Understand Heap vs Stack memory allocation and JVM Garbage Collection basics
- [ ] Understand why `equals()` and `hashCode()` must be overridden together (Hash contract)
- [ ] Explain why `String` is immutable and how the String Constant Pool works
- [ ] Explain why `StringBuilder` is faster than `String` concatenation in loops
- [ ] Explain Autoboxing/Unboxing and the performance penalty of wrapper classes in loops
- [ ] Implement custom immutable classes using Java 16+ `record`
- [ ] Implement rich `enum` types with abstract methods and fields
- [ ] Understand Java Generics: Type Erasure and Bridge Methods
- [ ] Implement Bounded Generics using PECS (Producer `extends`, Consumer `super`)
- [ ] Write functional pipelines using Streams API (`map`, `filter`, `reduce`, `collect`)
- [ ] Recognize `Optional` anti-patterns (never call `.get()` without check; use `.map()`/`.orElse()`)
- [ ] Use `java.time` (`Instant`, `LocalDate`, `ZonedDateTime`) instead of legacy `java.util.Date`
- [ ] Distinguish Checked vs Unchecked exceptions and implement `AutoCloseable`

---

### Level 2: Concurrency & Multithreading
- [ ] Explain Thread states (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED)
- [ ] Explain `synchronized` intrinsic locks, monitor acquisition, and biased locking
- [ ] Explain `volatile` in the Java Memory Model: visibility, instruction reordering, and memory barriers
- [ ] Explain why `wait()` and `notify()` must be called inside a synchronized block
- [ ] Explain why `wait()` must always be wrapped in a `while` loop (Spurious Wakeups)
- [ ] Size a `ThreadPoolExecutor` properly (`corePoolSize`, `maximumPoolSize`, `BlockingQueue`)
- [ ] Build asynchronous non-blocking pipelines using `CompletableFuture`
- [ ] Explain `ConcurrentHashMap` bucket-level locking / CAS vs synchronized collections
- [ ] Reproduce Coffman's 4 deadlock conditions and eliminate circular wait via Global Resource Ordering

---

### Level 3: Relational Databases & Pure JDBC
- [ ] Design a 3NF normalized schema with Primary Keys, Foreign Keys, CHECK, and UNIQUE constraints
- [ ] Explain how B-Tree indexes work in PostgreSQL and why indexes penalize writes
- [ ] Explain the Left-Most Prefix Rule in composite indexes
- [ ] Explain ACID guarantees (Atomicity, Consistency, Isolation, Durability)
- [ ] Explain why `PreparedStatement` pre-compiles the AST to prevent SQL injection
- [ ] Prevent connection and statement leaks using nested `try-with-resources`
- [ ] Size a HikariCP connection pool using the formula: $(\text{CPU Cores} \times 2) + \text{Spindles}$
- [ ] Manage manual ACID transactions using `conn.setAutoCommit(false)`, `commit()`, and `rollback()`
- [ ] Optimize bulk writes using JDBC batch operations (`addBatch()`, `executeBatch()`)

---

### Level 4: Spring Boot & Spring MVC
- [ ] Explain Inversion of Control (IoC), Dependency Injection, and `ApplicationContext`
- [ ] Explain Bean lifecycles, `@Component`, `@Service`, `@Repository`, and `@Configuration`
- [ ] Explain why Constructor Injection is mandatory over Field Injection (`@Autowired`)
- [ ] Trace an HTTP request through `DispatcherServlet`, `HandlerMapping`, and `HandlerAdapter`
- [ ] Implement RESTful endpoints using `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- [ ] Use Request and Response DTOs to eliminate security leaks and circular JSON references
- [ ] Validate input payloads using Jakarta Bean Validation (`@NotNull`, `@Size`, `@Min`, `@Valid`)
- [ ] Centralize error handling using `@RestControllerAdvice` and `@ExceptionHandler`

---

### Level 5: JPA & Hibernate 6 Internals
- [ ] Map entity relationships (`@OneToOne`, `@ManyToOne`, `@OneToMany`) with Owning vs Inverse sides
- [ ] Explain the 4 Entity states: `Transient`, `Managed`, `Detached`, and `Removed`
- [ ] Explain the Persistence Context, First-Level Cache, and Hibernate's Dirty Checking mechanism
- [ ] Explain how `@Transactional` works via Spring CGLIB dynamic proxies
- [ ] Recognize and avoid the "Self-Invocation" pitfall where `@Transactional` is silently bypassed
- [ ] Reproduce the N+1 query problem and eliminate it using JPQL `JOIN FETCH` and `@EntityGraph`
- [ ] Implement Optimistic Locking using JPA `@Version` to prevent lost updates

---

### Level 6: Spring Security 6 & Stateless JWT
- [ ] Differentiate Authentication (Identity / 401) from Authorization (Permissions / 403)
- [ ] Explain the Servlet filter chain and how `DelegatingFilterProxy` bridges into Spring
- [ ] Explain why plain SHA-256 is vulnerable to GPU rainbow tables
- [ ] Explain the mathematics of BCrypt: adaptive work factor ($2^{\text{cost}}$) and 128-bit random salt
- [ ] Construct and verify a signed JSON Web Token (JWT) using HMAC-SHA256
- [ ] Explain why JWT is signed, NOT encrypted (never store secrets in the payload)
- [ ] Implement `JwtAuthenticationFilter` extending `OncePerRequestFilter`
- [ ] Explain how `SecurityContextHolder` uses `ThreadLocal` and why cleanup is mandatory
- [ ] Configure custom 401 (`AuthenticationEntryPoint`) and 403 (`AccessDeniedHandler`) responses

---

### Level 7: Redis Caching & In-Memory Patterns
- [ ] Explain why Redis is single-threaded and how `epoll` I/O multiplexing handles 100k+ ops/sec
- [ ] Choose the right data structure: Strings (cache/counters), Hashes (profiles), ZSETs (leaderboards)
- [ ] Explain key eviction policies: `allkeys-lru`, `volatile-ttl`, and `noeviction`
- [ ] Configure JSON serialization over dangerous native Java serialization
- [ ] Implement Cache-Aside using Spring Cache (`@Cacheable`, `@CachePut`, `@CacheEvict`)
- [ ] Defend against the 4 Caching Disasters: Penetration, Breakdown (Stampede), Avalanche, and Staleness
- [ ] Implement distributed rate limiting using atomic Redis `INCR` + `EXPIRE`
- [ ] Implement stateless JWT revocation on logout using Redis key TTLs
- [ ] Implement graceful cache degradation via `ResilientCacheErrorHandler` so DB lookups never crash

---

### Level 8: Apache Kafka & Event-Driven Architecture
- [ ] Explain why Kafka is an append-only distributed commit log rather than a traditional queue
- [ ] Explain Topics, Partitions, Offsets, and Log Segments
- [ ] Explain why partition keys guarantee strict FIFO ordering per entity (e.g. `hash(orderId)`)
- [ ] Explain Consumer Groups and how partitions are distributed across consumers
- [ ] Explain At-Least-Once Delivery and why network retries cause duplicate messages
- [ ] Implement the Idempotent Consumer Pattern using a database deduplication table
- [ ] Prevent "Poison Pill" consumer crash loops using `ErrorHandlingDeserializer`
- [ ] Configure retries with exponential backoff and route unrecoverable records to a Dead Letter Topic (DLT)
- [ ] Explain the Dual-Write Problem between PostgreSQL and Kafka
- [ ] Implement the Transactional Outbox Pattern to guarantee atomic database mutations and event publishing

---

### Level 9: Production Observability & Performance
- [ ] Implement Distributed Tracing via `MDC` Correlation IDs (`X-Correlation-ID`)
- [ ] Configure structured logging with zero sensitive credential leaks (no passwords, no tokens)
- [ ] Implement Health checks (Liveness & Readiness probes) inspecting DB connections and JVM Heap
- [ ] Optimize database pagination using Keyset (Cursor) pagination instead of `OFFSET`
- [ ] Profile slow SQL queries using PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)`
- [ ] Profile JVM memory and thread contention using Java Flight Recorder (JFR)
