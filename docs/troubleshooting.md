# Enterprise Java Backend Troubleshooting & Debugging Guide

> **"A senior engineer is not someone who never sees errors. A senior engineer is someone who systematically isolates the failure layer using diagnostic evidence rather than random guessing."**

---

## 1. The Troubleshooting Mindset: The 5-Layer Triage Model

When an error occurs, identify which layer is failing before touching a line of code:

```
[ Layer 5: Client / Gateway ] ──► (CORS, 401 Unauthorized, 403 Forbidden, 400 Validation)
[ Layer 4: Spring Framework ] ──► (NoSuchBeanDefinitionException, CircularDependencyException)
[ Layer 3: ORM / Data Layer ] ──► (LazyInitializationException, TransientPropertyValueException, N+1)
[ Layer 2: Network / Wire   ] ──► (Connection refused: 5432 / 6379 / 9092, SocketTimeoutException)
[ Layer 1: OS / JVM Runtime ] ──► (OutOfMemoryError, UnsupportedClassVersionError, Port already in use)
```

---

## 2. Common Failures & Diagnostic Playbooks

### 1. Java Version Mismatch (`UnsupportedClassVersionError: ... has been recognized by a more recent runtime`)
- **Root Cause:** Code was compiled with a higher Java version (e.g. Java 21) than the JRE executing it (e.g. Java 17).
- **How to Diagnose:**
  - Check bytecode class version: `javap -verbose target/classes/com/.../OrderFlowApplication.class | findstr major`
  - Java 21 bytecode is major version 65; Java 17 is major version 61.
- **The Fix:** Ensure `JAVA_HOME` points to JDK 21+ and verify `pom.xml` specifies `<java.version>21</java.version>`.

---

### 2. Maven Dependency & Plugin Conflicts
- **Symptom:** `ClassNotFoundException` or `NoSuchMethodError` despite the dependency existing in `pom.xml`.
- **Root Cause:** A transitive dependency conflict (e.g., library A pulls in Jackson 2.14, library B pulls in Jackson 2.16).
- **How to Diagnose:**
  - Run the Maven dependency tree: `mvn dependency:tree -Dincludes=com.fasterxml.jackson*`
  - Look for `omitted for conflict with ...` lines.
- **The Fix:** Exclude the conflicting transitive dependency or declare the preferred version explicitly in `<dependencyManagement>`.

---

### 3. Port Already in Use (`Web server failed to start. Port 8080 was already in use`)
- **How to Diagnose:**
  - In Windows PowerShell: `Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess`
  - Identify process ID: `Get-Process -Id <PID>`
- **The Fix:**
  - Kill orphaned process: `Stop-Process -Id <PID> -Force`
  - Or override port temporarily: `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"`

---

### 4. PostgreSQL Connection Refused (`Connection to localhost:5432 refused`)
- **Root Cause:** PostgreSQL daemon is not running, or listening only on a Unix socket instead of TCP.
- **How to Diagnose:**
  - Test TCP port: `Test-NetConnection -ComputerName localhost -Port 5432`
  - If using Docker: check container status: `docker ps -a | findstr postgres`
- **The Fix:**
  - Start container: `docker compose up -d postgres`
  - For tests/dev: Fall back to H2 in-memory mode via `spring.profiles.active=dev`.

---

### 5. `LazyInitializationException: could not initialize proxy - no Session`
- **Root Cause:** An entity was fetched with `FetchType.LAZY` (e.g. `order.getItems()`). The Hibernate `Session` closed when the `@Transactional` service method exited. Later, in the controller or JSON serializer, code attempts to read `order.getItems()`.
- **How to Diagnose:**
  - Check the stack trace: see which getter was invoked outside a `@Transactional` boundary.
- **The Fix:**
  - ❌ Anti-pattern: DO NOT enable `spring.jpa.open-in-view=true` (keeps DB connections open during rendering, exhausting connection pools).
  - ✅ Solution: Use JPQL `JOIN FETCH` or Spring Data `@EntityGraph` in the repository to load required relations within the service transaction!

---

### 6. Spring Bean Not Found (`NoSuchBeanDefinitionException: No qualifying bean of type ...`)
- **Root Cause:** The class is missing a stereotype annotation (`@Service`, `@Component`, `@Repository`), or is outside Spring Boot's component scan package hierarchy (`@SpringBootApplication` scans only its own package and sub-packages).
- **How to Diagnose:**
  - Check package declaration of the missing class. If `OrderFlowSpringBootApplication` is in `com.backendlearninglab`, the bean must be in `com.backendlearninglab.*`.
- **The Fix:** Move the class into the package hierarchy or add explicit `@ComponentScan`.

---

### 7. CORS Errors (`No 'Access-Control-Allow-Origin' header is present on the requested resource`)
- **Root Cause:** Browser security blocks cross-origin XMLHttpRequests/fetch calls from `http://localhost:3000` to `http://localhost:8080` unless the server sends explicit CORS headers during the HTTP preflight `OPTIONS` request.
- **How to Diagnose:**
  - Inspect Network tab in Chrome DevTools: look for failed `OPTIONS` preflight request.
- **The Fix:**
  - Add a `@CrossOrigin(origins = "http://localhost:3000")` or configure a global `CorsConfigurationSource` bean in Spring Security.

---

### 8. Spring Security 401 Unauthorized vs. 403 Forbidden
- **How to Diagnose:**
  - Enable TRACE logging in `application.properties`:
    ```properties
    logging.level.org.springframework.security=TRACE
    logging.level.org.springframework.security.web.FilterChainProxy=DEBUG
    ```
- **If 401 Unauthorized:**
  - Check whether `Authorization: Bearer <jwt>` header was sent.
  - Check whether `JwtAuthenticationFilter` threw `ExpiredJwtException` or `SignatureException`.
- **If 403 Forbidden:**
  - The JWT is valid, but the user's role does not match `hasRole(...)`.
  - Look for: `Voting to grant access: GrantedAuthority: [ROLE_USER] does not match required: [ROLE_ADMIN]`.

---

### 9. Redis Unavailable (`Unable to connect to Redis`)
- **How to Diagnose:**
  - Check if Redis container is active: `docker compose ps redis`
  - Test ping: `redis-cli ping` (should reply `PONG`).
- **The Fix:**
  - Start container: `docker compose up -d redis`
  - In our architecture: `ResilientCacheErrorHandler` automatically swallows the Redis exception, logs a warning, and falls back to PostgreSQL queries seamlessly.

---

### 10. Kafka Consumer Not Receiving Messages
- **How to Diagnose:**
  1. Check Consumer Lag:
     `kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group <group-id>`
  2. If Lag is 0 but messages were sent:
     - Check if consumer was configured with `auto-offset-reset=latest` (it skipped historical messages!). Change to `earliest`.
  3. If Lag is increasing:
     - The consumer thread is stuck or deadlocked. Check thread dump via `jstack <PID>` or look for unhandled exceptions in logs.
  4. If Deserialization Error:
     - Check for poison pill messages. Ensure `ErrorHandlingDeserializer` is active in `consumerFactory`.
