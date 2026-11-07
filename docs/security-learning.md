# Spring Security 6 & JWT Mastery Guide

> **"Never roll your own crypto, never store plaintext passwords, and never trust user input beyond your filter boundary."**

---

## 1. Authentication vs Authorization: The Separation of Concerns

In enterprise backend systems, conflating authentication and authorization is one of the most common architectural flaws.

```
+-----------------------------------------------------------------------------------+
|                                  INCOMING REQUEST                                 |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
                      ┌───────────────────────────────────────┐
                      │        Authentication (AuthN)         │
                      │        "Who are you?"                 │
                      └───────────────────────────────────────┘
                                          │
                        ┌─────────────────┴─────────────────┐
                        │ Valid Credentials                 │ Invalid / Missing
                        ▼                                   ▼
          ┌───────────────────────────┐         ┌───────────────────────────┐
          │ Populates SecurityContext │         │ HTTP 401 Unauthorized     │
          │ with authenticated        │         │ (AuthenticationEntryPoint)│
          │ UserPrincipal & Roles     │         └───────────────────────────┘
          └───────────────────────────┘
                        │
                        ▼
          ┌───────────────────────────┐
          │   Authorization (AuthZ)   │
          │   "Are you allowed here?" │
          └───────────────────────────┘
                        │
          ┌─────────────┴─────────────┐
          │ Permitted                 │ Insufficient Privileges
          ▼                           ▼
┌───────────────────┐       ┌───────────────────────────┐
│ DispatcherServlet │       │ HTTP 403 Forbidden        │
│ ──► Controller    │       │ (AccessDeniedHandler)     │
└───────────────────┘       └───────────────────────────┘
```

### Key Differences: 401 vs 403

| HTTP Status | Meaning | When Thrown | Spring Security Handler |
| :--- | :--- | :--- | :--- |
| **`401 Unauthorized`** | Unauthenticated (Identity Unknown) | Missing Bearer token, expired JWT, invalid password, corrupt signature. | `AuthenticationEntryPoint` |
| **`403 Forbidden`** | Authenticated, but Unauthorized (Lacks Permissions) | Valid `USER` role attempts to hit `/api/v1/users` (requires `ROLE_ADMIN`). | `AccessDeniedHandler` |

---

## 2. Password Hashing: Why Plaintext & Plain SHA-256 Are Disasters

### The Mathematics of BCrypt

Plain hashing algorithms like MD5 or SHA-256 were designed for **file integrity verification**, meaning they execute in **nanoseconds**. 
- Modern GPUs (RTX 4090) can compute over **100 billion SHA-256 hashes per second**.
- If an attacker dumps a database containing SHA-256 hashes, precomputed **rainbow tables** or brute-force dictionaries recover 8-character passwords within seconds.

**BCrypt** solves this through three deliberate design principles:
1. **Adaptive Work Factor (Cost parameter, default 10–12):** BCrypt uses key stretching ($2^{\text{cost}}$ iterations). An increase of 1 in cost doubles the computation time. At cost 12, each hash takes ~250–300 ms on modern hardware. This is unnoticeable to a logging-in user, but renders GPU brute-force cracking mathematically infeasible ($2^{12} = 4096$ rounds).
2. **Cryptographic Salt Injection:** A cryptographically random 16-byte (128-bit) salt is automatically generated and embedded into the 60-character result string. Even if two users choose the identical password `"Password123!"`, their BCrypt hashes are completely distinct, defeating rainbow tables entirely.
3. **Timing-Attack Resistance:** Password verification uses constant-time string comparison (`MessageDigest.isEqual`) to prevent attackers from inferring character matches via execution latency.

### Anatomy of a BCrypt Hash String

```
$2a$12$e8n...QdO9RkS...
├───┼──┼────────────────────────┼───────────────────────────────┤
│ $2a  │ Algorithm identifier   │ BCrypt revision (OpenBSD)     │
│ 12   │ Cost parameter (log2)  │ 2^12 = 4,096 computation rounds│
│ 22 ch│ Random Salt (128 bits) │ Base64-encoded unique salt    │
│ 31 ch│ Hash Output (184 bits) │ Base64-encoded cipher text    │
```

---

## 3. Stateless JWT (JSON Web Token) Architecture

### Why Stateless for Microservices and Scalable Backends?

- **Stateful (Session / Cookie-based):**
  - Session data (`JSESSIONID`) is stored on server memory or sticky Redis sessions.
  - Requires sticky load balancing or centralized session databases.
  - Vulnerable to Cross-Site Request Forgery (CSRF).
- **Stateless (JWT Bearer Token):**
  - The token is a self-contained cryptographic passport.
  - The server holds **zero state** regarding who is logged in.
  - Every application instance with the shared secret key can independently verify the token's validity in under 1 millisecond without hitting any database.

### Anatomy of a JWT

A JWT consists of three Base64Url-encoded segments joined by dots (`.`):

$$\text{JWT} = \text{Base64Url}(\text{Header}) \ . \ \text{Base64Url}(\text{Payload}) \ . \ \text{Base64Url}(\text{Signature})$$

1. **Header:** Algorithm and token type: `{"alg": "HS256", "typ": "JWT"}`.
2. **Payload (Claims):** Subject, expiration, issued-at, custom roles:
   ```json
   {
     "sub": "admin@orderflow.com",
     "roles": ["ROLE_ADMIN"],
     "iat": 1731000000,
     "exp": 1731086400
   }
   ```
   > [!WARNING]
   > **A JWT is signed, NOT encrypted!** Anyone can decode Base64Url and read the payload. **NEVER** place credit cards, social security numbers, or passwords inside a JWT payload.
3. **Signature:** Cryptographic seal preventing tampering:
   $$\text{Signature} = \text{HMAC-SHA256}(\text{Header} \ . \ \text{Payload}, \ \text{SecretKey})$$
   If an attacker modifies `"roles": ["ROLE_USER"]` to `"ROLE_ADMIN"`, the signature verification fails immediately because the attacker lacks the server's private secret key.

---

## 4. Spring Security 6 Filter Chain Implementation

### Filter Execution Sequence

Spring Security executes a configured chain of filters on every request. Our custom `JwtAuthenticationFilter` is positioned before `UsernamePasswordAuthenticationFilter`:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
            .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/orders/**").authenticated()
            .anyRequest().authenticated()
        )
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### Why SecurityContextHolder Uses `ThreadLocal`

By default, `SecurityContextHolder.getContext()` uses a `ThreadLocal` storage strategy:
- Each HTTP request handled by Tomcat is assigned a worker thread from the thread pool.
- The `JwtAuthenticationFilter` sets the `Authentication` on the thread's `ThreadLocal`.
- Any downstream component (Services, `@PreAuthorize` evaluators, Repositories) can access `SecurityContextHolder.getContext().getAuthentication()` without passing credentials through method signatures.
- **Critical Requirement:** At the end of the request, `SecurityContextHolderFilter` cleans up the `ThreadLocal` to prevent thread leakage when threads are returned to the Tomcat thread pool.

---

## 5. Roles vs Authorities

Spring Security distinguishes between **Roles** and **Authorities**:

- **Authority:** An individual permission string (e.g. `"product:delete"`, `"order:create"`). Evaluated with `.hasAuthority("product:delete")`.
- **Role:** A coarse-grained persona grouping multiple permissions. By convention in Spring Security, a role automatically prepends the prefix **`ROLE_`**.
  - If you configure `.hasRole("ADMIN")`, Spring checks if the user possesses the authority `"ROLE_ADMIN"`.
  - In our `CustomUserPrincipal`:
    ```java
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    ```

---

## 6. How to Debug Spring Security

When an endpoint returns an unexpected 401 or 403, do NOT guess. Enable Spring Security trace logging in `application.properties`:

```properties
logging.level.org.springframework.security=TRACE
logging.level.org.springframework.security.web.FilterChainProxy=DEBUG
```

### What You Will See in the Logs:
1. **The Exact Filter Chain:** Lists all 15+ filters in the order they will execute.
2. **Authentication Attempts:** Shows whether `JwtAuthenticationFilter` extracted a token, threw an `ExpiredJwtException`, or left the context anonymous.
3. **Authorization Decisions:** Shows:
   ```
   Voting to grant access: GrantedAuthority: [ROLE_USER] does not match required: [ROLE_ADMIN] -> AccessDeniedException
   ```
