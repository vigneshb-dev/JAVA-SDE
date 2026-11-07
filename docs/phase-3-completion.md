# Phase 3 Completion: Spring Security, Stateless JWT & Redis Caching

> **OrderFlow Backend Learning Lab — Phase 3 Review & Verification Guide**

---

## 1. Executive Summary

Phase 3 transitions our monolithic backend into a hardened, production-grade enterprise system featuring:
- **Stateless Authentication:** JSON Web Tokens (JJWT 0.12.5) with HMAC-SHA256 digital signatures.
- **Adaptive Password Security:** BCrypt with 12 calculation rounds ($2^{12} = 4,096$ key stretching iterations) and per-user salt injection.
- **Enterprise Filter Pipeline:** Custom `JwtAuthenticationFilter` positioned in Spring's `SecurityFilterChain`.
- **Role-Based Authorization:** Strict URI matching for `USER`, `ADMIN`, and `INVENTORY_MANAGER` roles.
- **Custom Security Exception Handling:** Differentiating `401 Unauthorized` (`DelegatedAuthenticationEntryPoint`) from `403 Forbidden` (`DelegatedAccessDeniedHandler`).
- **Distributed Caching & In-Memory Storage:** Redis integration via `spring-boot-starter-data-redis` and Spring Cache abstraction (`@Cacheable`, `@CachePut`, `@CacheEvict`).
- **Resilient Fallback Design:** `ResilientCacheErrorHandler` ensuring database queries succeed even if Redis is unreachable.
- **Distributed Use Cases:** Atomic fixed-window rate limiting (`RateLimiterService`) and immediate JWT revocation on logout (`TokenBlacklistService`).

---

## 2. Directory Manifest for Phase 3

### New Security & Auth Architecture:
- `src/main/java/com/backendlearninglab/spring/security/`:
  - `SecurityConfiguration.java`: Main Spring Security 6 config with stateless session and URL rules.
  - `JwtService.java`: Cryptographic token builder, parser, and signature verifier.
  - `JwtAuthenticationFilter.java`: `OncePerRequestFilter` inspecting `Authorization: Bearer <token>`.
  - `CustomUserPrincipal.java`: Adapter binding JPA `UserEntity` to Spring's `UserDetails`.
  - `CustomUserDetailsService.java`: Loads user principal from PostgreSQL repository.
  - `DelegatedAuthenticationEntryPoint.java`: Intercepts 401s and renders standardized JSON.
  - `DelegatedAccessDeniedHandler.java`: Intercepts 403s and renders standardized JSON.

### New Redis & Caching Architecture:
- `src/main/java/com/backendlearninglab/spring/redis/`:
  - `RedisConfiguration.java`: Configures `RedisTemplate`, `GenericJackson2JsonRedisSerializer`, and conditional cache manager.
  - `ResilientCacheErrorHandler.java`: Gracefully degrades cache misses to database lookups during Redis timeouts or outages.
  - `RateLimiterService.java`: Atomic counter throttling requests across distributed replicas.
  - `TokenBlacklistService.java`: Revokes JWTs on user logout using Redis automatic TTL expiration.

### Interactive Runnable Standalone Demos:
- `examples/security/PlaintextVsBCryptDemo.java`: Compares plaintext, SHA-256 rainbow tables, and BCrypt work factor.
- `examples/security/ManualJwtDemo.java`: Pure Java HMAC-SHA256 JWT construction, Base64Url parsing, and tamper detection without external libraries.
- `examples/redis/StandaloneRedisDemo.java`: In-memory key-value simulation demonstrating SET, GET, DEL, EXPIRE, and LRU eviction.

### Comprehensive Documentation:
- `docs/security-fundamentals.md`: Architectural request pipeline and core security concepts.
- `docs/security-learning.md`: Deep dive into JWT lifecycle, BCrypt mathematics, roles vs authorities, and debugging techniques.
- `docs/redis-learning.md`: In-memory physics, single-threaded reactor pattern, eviction policies, and serialization dangers.
- `docs/caching-learning.md`: Caching patterns (Cache-Aside, Write-Through, Write-Behind) and defenses against the 4 classic caching disasters (Penetration, Breakdown, Avalanche, Stale Cache).

### Automated Test Suites:
- `src/test/java/com/backendlearninglab/spring/AuthenticationAndSecurityTest.java`: End-to-end integration tests for register, login, 401 rejection, and 403 authorization.
- `src/test/java/com/backendlearninglab/spring/RedisCachingAndRateLimiterTest.java`: Lifecycle tests for `@Cacheable`, `@CachePut`, `@CacheEvict`, rate limiting, and token blacklisting.

---

## 3. How to Run and Verify Phase 3

### A. Run All Automated Test Suites
```bash
mvn test
```
Verifies all 5 test suites across Core Java, Pure JDBC, JPA, Spring MVC, Spring Security, and Redis.

### B. Run the Standalone Interactive Demos
```bash
# 1. Plaintext vs BCrypt Comparison Demo
mvn exec:java -Dexec.mainClass="com.backendlearninglab.examples.security.PlaintextVsBCryptDemo"

# 2. Pure Java Manual JWT Construction Demo
mvn exec:java -Dexec.mainClass="com.backendlearninglab.examples.security.ManualJwtDemo"

# 3. Standalone Redis & LRU Eviction Simulation Demo
mvn exec:java -Dexec.mainClass="com.backendlearninglab.examples.redis.StandaloneRedisDemo"
```

### C. Live REST API Verification (via cURL)
Once the server is running (`mvn spring-boot:run`):

```bash
# 1. Register a new user (Customer role)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer@example.com","password":"Password123!","fullName":"Buyer Bob","role":"USER"}'

# 2. Login to obtain JWT
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer@example.com","password":"Password123!"}'

# 3. Query current profile using JWT
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"

# 4. Attempt administrative action with Customer JWT (Returns 403 Forbidden!)
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```
