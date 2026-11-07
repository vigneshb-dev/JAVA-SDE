# Enterprise Caching Architecture & Failure Modes Guide

> **"There are only two hard things in Computer Science: cache invalidation and naming things."** — Phil Karlton

---

## 1. Caching Strategies Compared

Caching is the practice of duplicating a subset of data in high-speed, volatile storage (RAM) to serve read requests with sub-millisecond latencies.

```
+───────────────────────────────────────────────────────────────────────────────+
|                             1. CACHE-ASIDE (LAZY)                             |
|                                                                               |
|   App ───(1) Check Cache───► [ Redis ]                                        |
|    │                                                                          |
|    ├───(2) Cache Miss! ────► [ PostgreSQL ]                                   |
|    │                                                                          |
|    └───(3) Populate Cache──► [ Redis ]                                        |
+───────────────────────────────────────────────────────────────────────────────+

+───────────────────────────────────────────────────────────────────────────────+
|                             2. WRITE-THROUGH                                  |
|                                                                               |
|   App ───(1) Write Data───► [ Redis Cache ] ───(2) Synchronous Write──► [ DB ]|
+───────────────────────────────────────────────────────────────────────────────+

+───────────────────────────────────────────────────────────────────────────────+
|                             3. WRITE-BEHIND (WRITE-BACK)                      |
|                                                                               |
|   App ───(1) Write Data───► [ Redis Cache ]                                   |
|                                   │                                           |
|                                   └───(2) Async Batch Write (Delayed)──► [ DB ]
+───────────────────────────────────────────────────────────────────────────────+
```

### Strategy Trade-off Matrix

| Strategy | Read Latency | Write Latency | Risk of Data Loss | Stale Data Risk | Primary Use Case |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Cache-Aside** | Ultra low (hit)<br>High (miss) | Low | **Zero** (DB is primary) | Medium (if invalidation fails) | General read-heavy product catalogs, user profiles. |
| **Read-Through** | Ultra low | Low | **Zero** | Low | Abstracted multi-tier caching frameworks. |
| **Write-Through** | Ultra low | High (writes to 2 systems) | **Zero** | **Lowest** | Financial ledgers, high-consistency inventory. |
| **Write-Behind** | Ultra low | **Ultra low** (in-memory write) | **High** (Redis crash before DB flush) | High | Audit logging, clickstream analytics, metric counters. |

---

## 2. Spring Cache Abstraction in OrderFlow

Spring provides declarative caching via AOP (Aspect-Oriented Programming) proxies.

### `@Cacheable`
```java
@Cacheable(value = "products", key = "#id")
public ProductResponse getProductById(Long id) { ... }
```
- **How it works internally:**
  1. The AOP proxy intercepts the method invocation.
  2. Evaluates the SpEL expression `#id` to construct the Redis key (`products::101`).
  3. Checks Redis. If present (**Cache Hit**), the method body is **bypassed completely**.
  4. If absent (**Cache Miss**), executes the method (PostgreSQL query), writes the returned `ProductResponse` into Redis, and returns.

### `@CachePut`
```java
@CachePut(value = "products", key = "#id")
public ProductResponse updatePrice(Long id, BigDecimal newPrice) { ... }
```
- **Crucial Difference:** Unlike `@Cacheable`, `@CachePut` **ALWAYS executes the method body**.
- It captures the newly returned entity and immediately updates the Redis cache entry.
- **Why use it?** Eliminates cache staleness immediately upon updates without waiting for TTL expiration.

### `@CacheEvict`
```java
@CacheEvict(value = {"products", "productListings"}, allEntries = true)
public void deactivateProduct(Long id) { ... }
```
- Purges entries from the cache when an entity is deleted or deactivated.
- `allEntries = true` clears the entire cache bucket.

---

## 3. The 4 Classic Caching Disasters & Production Defenses

### 1. Cache Penetration
- **The Problem:** An attacker sends millions of requests for non-existent IDs (e.g. `GET /api/v1/products/-99999`).
  - Cache checks Redis $\rightarrow$ Miss.
  - Queries PostgreSQL $\rightarrow$ Returns null.
  - Cache does not store nulls $\rightarrow$ Subsequent requests repeat the exact same DB lookup!
  - **Result:** PostgreSQL connection pool exhaustion and crash.
- **The Solution:**
  1. **Cache Null Values:** Store a sentinel object or `null` in Redis with a short TTL (e.g. 60 seconds).
  2. **Bloom Filter:** A probabilistic data structure in front of the cache that determines with 100% certainty if an ID **does not exist**, rejecting requests before hitting Redis or DB.

### 2. Cache Breakdown (Dogpiling / Cache Stampede)
- **The Problem:** An extremely popular product (e.g., iPhone during Black Friday) has its cache key expire.
  - In a single second, 10,000 concurrent requests arrive.
  - All 10,000 find the cache empty.
  - All 10,000 execute the complex DB query simultaneously.
  - **Result:** Database CPU spikes to 100%, cascading failure.
- **The Solution:**
  - **Single-Flight Mutex / Distributed Lock:** When a cache miss occurs, only the first thread acquires a lock to recompute the value from the DB. All other threads wait for the lock or yield.
  - **Background Refresh (Ahead of Time):** A background worker detects keys close to expiration ($\text{TTL} < 30\text{s}$) and pre-warms the cache.

### 3. Cache Avalanche
- **The Problem:** A developer configures all product keys to expire with a fixed TTL: `EXPIRE key 3600`.
  - Exactly 1 hour after system startup or batch load, **every single cache key expires at the exact same second**.
  - All incoming traffic immediately slams PostgreSQL.
- **The Solution:**
  - **Jittered TTL:** Introduce uniform random jitter to the TTL:
    $$\text{TTL} = \text{BaseTTL} + \text{random}(0, \text{JitterWindow})$$
    e.g., `Duration.ofSeconds(3600 + ThreadLocalRandom.current().nextInt(300))`.
    Cache expirations are now smoothly distributed across a 5-minute window.

### 4. Stale Cache (Inconsistency)
- **The Problem:** An operator updates product prices directly in PostgreSQL via a SQL script (`UPDATE products SET price = 299 WHERE id = 1`).
  - Redis still holds the old price (`350.00`).
  - Customers purchase the product at the wrong price until the TTL expires hours later.
- **The Solution:**
  - Never bypass the application service layer for transactional mutations.
  - In distributed systems, implement **Change Data Capture (CDC)** using Debezium and Kafka to stream database WAL (Write-Ahead Log) mutations directly into a cache invalidator service.
