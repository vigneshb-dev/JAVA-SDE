# The Master Java Backend Learning Roadmap

> **"Follow this structured progression. Do not skip phases. Each level builds the mechanical foundation for the next."**

---

```
                                  THE MASTER ROADMAP
                                          │
    LEVEL 1: CORE JAVA                   ▼
    ├── Memory Model, OOP, Immutability, Records
    ├── Collections Internals (ArrayList vs LinkedList, HashMap Treeification)
    ├── Generics & PECS (Producer-Extends, Consumer-Super)
    ├── Modern Java (Streams API, Lambdas, java.time)
    └── Exception Hierarchy & AutoCloseable
                                          │
    LEVEL 2: CONCURRENCY                 ▼
    ├── Thread Lifecycle & Intrinsic Monitors (synchronized)
    ├── JMM Happens-Before & volatile Memory Barriers
    ├── Thread Coordination (wait/notify, Spurious Wakeups)
    ├── ThreadPoolExecutor Sizing & WorkQueues
    ├── Asynchronous Non-Blocking Pipelines (CompletableFuture)
    └── Deadlock Analysis (Coffman's 4 conditions)
                                          │
    LEVEL 3: RELATIONAL DATA & JDBC       ▼
    ├── 3NF Schema Design, Foreign Keys & Constraints
    ├── B-Tree Index Mechanics & Write Penalties
    ├── Low-Level TCP Connections & HikariCP Pool Sizing
    ├── SQL Injection Defense (PreparedStatement AST Compilation)
    └── Manual ACID Transactions (setAutoCommit, commit, rollback)
                                          │
    LEVEL 4: SPRING BOOT & MVC            ▼
    ├── Inversion of Control (IoC) & ApplicationContext
    ├── Constructor Injection vs Field Injection
    ├── DispatcherServlet Request Lifecycle & HttpMessageConverters
    ├── DTOs & Jakarta Bean Validation (@Valid)
    └── Global Exception Handling (@RestControllerAdvice)
                                          │
    LEVEL 5: JPA & HIBERNATE 6            ▼
    ├── Entity Lifecycle (Transient, Managed, Detached, Removed)
    ├── Persistence Context, First-Level Cache & Dirty Checking
    ├── Association Mapping (OneToMany, ManyToOne, Cascade, orphanRemoval)
    ├── Declarative Transactions (@Transactional Dynamic Proxies)
    └── The N+1 Problem (JPQL JOIN FETCH & @EntityGraph)
                                          │
    LEVEL 6: SPRING SECURITY 6            ▼
    ├── Servlet Filter Pipeline & DelegatingFilterProxy
    ├── Cryptographic Password Hashing (BCrypt Work Factor & Salt)
    ├── Stateless JWT (Claims, HMAC-SHA256 Signatures, Expiration)
    ├── Role-Based Access Control (RBAC) & Custom 401/403 Handlers
    └── ThreadLocal SecurityContextHolder Lifecycle
                                          │
    LEVEL 7: REDIS DISTRIBUTED CACHING    ▼
    ├── In-Memory Single-Threaded Reactor (epoll)
    ├── Data Structures (Strings, Hashes, Sets, ZSETs) & LRU Eviction
    ├── Spring Cache Abstraction (@Cacheable, @CachePut, @CacheEvict)
    ├── Distributed Atomic Rate Limiting (INCR + EXPIRE)
    ├── JWT Token Revocation via Redis TTL Blacklisting
    └── Resilient Cache Degradation (ResilientCacheErrorHandler)
                                          │
    LEVEL 8: APACHE KAFKA & EDA          ▼
    ├── Distributed Append-Only Commit Log Architecture
    ├── Topics, Partitions, Offsets & Consumer Groups
    ├── At-Least-Once Delivery & Idempotent Consumer Pattern
    ├── Resilient Deserialization (ErrorHandlingDeserializer)
    ├── Retries & Dead Letter Topics (DLT / Poison Pill Defense)
    └── The Dual-Write Problem & The Transactional Outbox Pattern
```

---

## Study Progression Strategy

For each level in the roadmap:
1. **Read the corresponding documentation** in `docs/`.
2. **Execute the standalone educational demos** in `examples/`.
3. **Inspect the actual project implementation** in `src/main/java/com/backendlearninglab/`.
4. **Run the automated test suite** using `mvn test`.
5. **Break the code intentionally** (modify configurations, pass invalid payloads, disable network connections) and observe how the system fails.
6. **Attempt the corresponding engineering challenge** in `docs/challenges.md`.
