# Redis Deep Dive: In-Memory Architecture, Data Structures & Distributed Patterns

> **"A database is optimized for durable consistency; Redis is optimized for nanosecond memory access. Misunderstanding the trade-off leads to cache stampedes, stale data, and silent outages."**

---

## 1. Why Redis? Fundamentals & Single-Threaded Event Loop

### The Physics of Latency

| Storage Medium | Access Latency | Operations / Sec |
| :--- | :--- | :--- |
| **L1 CPU Cache** | ~0.5 – 1 ns | Billions |
| **RAM (Redis)** | **~50 – 100 ns** | **100,000+** |
| **NVMe SSD (PostgreSQL hot page)** | ~100 µs (100,000 ns) | ~10,000 – 50,000 |
| **Rotational HDD** | ~5 – 10 ms (10,000,000 ns) | ~100 – 200 |

Redis stores all operational data directly in **DRAM**. A lookup that requires reading an index page from disk in PostgreSQL takes orders of magnitude longer than reading from memory in Redis.

### How Does Redis Achieve 100,000+ Ops/Sec on a Single Thread?

A common misconception is that Redis must be multithreaded to handle thousands of concurrent clients. In fact, Redis's core execution engine is **single-threaded**, which eliminates:
1. Context switching overhead.
2. Thread synchronization locking (`synchronized`, mutexes, semaphores).
3. Race conditions and deadlocks.

Redis uses non-blocking I/O multiplexing (via the OS kernel's `epoll` on Linux or `kqueue` on macOS/BSD). A single reactor loop handles thousands of active client sockets simultaneously:

```
[ Client 1 ] ──┐
[ Client 2 ] ──┼──► [ OS Kernel epoll ] ──► [ Redis Single-Threaded Event Loop ] ──► [ In-Memory Data Store ]
[ Client 3 ] ──┘
```

---

## 2. Core Redis Data Structures & Enterprise Use Cases

| Data Structure | Internal Representation | Big-O Complexity | Ideal Enterprise Use Case |
| :--- | :--- | :--- | :--- |
| **String** | SDS (Simple Dynamic String) | $O(1)$ get/set | Object Caching (JSON), Atomic Counters (`INCR`), Fixed-Window Rate Limiting. |
| **Hash** | Hash Table / Listpack | $O(1)$ field access | User session profiles, Shopping cart items (avoiding full-object serialization). |
| **List** | Quicklist (Linked list of zip lists) | $O(1)$ push/pop | Job queues (`LPUSH` / `BRPOP`), recent activity streams. |
| **Set** | Intset / Hash Table | $O(1)$ add/check | Unique tags, friend lists, Token Blacklists (revocation sets). |
| **Sorted Set (ZSET)** | SkipList + Hash Table | $O(\log N)$ add/range | Gaming leaderboards, Sliding-window rate limiters (scores = millisecond timestamps). |

---

## 3. Key Eviction Policies (When Memory Fills Up)

When Redis hits `maxmemory`, it must decide which keys to discard:

1. **`noeviction` (Default):** Returns an out-of-memory error on write operations when memory limit is reached. Safe for primary datastores, fatal for volatile caches.
2. **`allkeys-lru`:** Evicts the **Least Recently Used** key across all keys. (The gold standard for caching).
3. **`volatile-lru`:** Evicts least recently used keys among those with an explicit **TTL** (`EXPIRE`).
4. **`allkeys-lfu`:** Evicts the **Least Frequently Used** key (keys with fewest total hits). Useful when certain old items remain consistently popular.
5. **`volatile-ttl`:** Evicts keys with the shortest remaining Time-To-Live.

---

## 4. Cache Serialization: The Hidden Production Trap

When storing Java objects in Redis, they must be converted into byte arrays (`byte[]`).

### 1. Java Native Serialization (`JdkSerializationRedisSerializer`)
- ❌ **DANGEROUS:** Extremely vulnerable to Remote Code Execution (RCE) deserialization attacks.
- ❌ **Brittle:** If a Java class changes a single field or `serialVersionUID`, existing cached entries cannot be read and throw `InvalidClassException`.
- ❌ **Opaque:** Values stored in Redis are unreadable binary garbage; you cannot inspect them in `redis-cli`.

### 2. JSON Serialization (`GenericJackson2JsonRedisSerializer`)
- ✅ **Human-Readable:** Data in `redis-cli` is plain JSON: `{"id":1,"name":"Desk","price":350.00}`.
- ✅ **Cross-Language:** Node.js, Python, or Go microservices can consume the exact same cache entries.
- ✅ **Resilient:** Tolerates adding new optional fields to Java DTOs.

Our `RedisConfiguration.java` configures `GenericJackson2JsonRedisSerializer`:
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
    return template;
}
```

---

## 5. Distributed Backend Patterns Implemented in OrderFlow

### Pattern 1: Atomic Rate Limiting (Fixed Window)
Preventing abusive API clients or DDoS attacks:
1. Compute Redis key: `rate_limit:{ip}:{window_timestamp}`.
2. Call atomic `redisTemplate.opsForValue().increment(key)`.
3. If value equals `1` (new window), set TTL to 60 seconds (`EXPIRE`).
4. If count exceeds `limit`, reject with `HTTP 429 Too Many Requests`.
- **Why Redis?** Because multiple horizontal instances of the Spring Boot application share the identical counter state in Redis atomically.

### Pattern 2: Token Blacklisting (Stateless JWT Revocation)
Since JWTs are stateless and cannot be cancelled before expiration:
1. When user calls `POST /api/v1/auth/logout`:
2. Extract the JWT token and calculate its remaining lifespan: $\text{TTL} = \text{Token Expiration} - \text{Current Time}$.
3. Store token in Redis: `SET blacklist:{jwt} "revoked" EX {TTL}`.
4. In `JwtAuthenticationFilter`: check `if (tokenBlacklistService.isTokenBlacklisted(token))` -> return 401 Unauthorized!
5. After the token naturally expires, Redis automatically evicts the blacklist key. Zero perpetual memory leakage!

### Pattern 3: Resilient Fallback (Graceful Cache Degradation)
If Redis crashes or the network partition severs the connection, your database should NOT crash with it!
Using `ResilientCacheErrorHandler`:
```java
public class ResilientCacheErrorHandler implements CacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis GET failed for key {}. Falling back to PostgreSQL DB directly.", key);
        // Do NOT rethrow! By swallowing the exception, Spring Cache treats it as a cache MISS
        // and seamlessly delegates to the database query!
    }
}
```
