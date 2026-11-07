# Spring Security 6 Architecture & Fundamentals Guide

> **"Security is not a feature you add at the end of a project. It is an architectural filter pipeline that intercepts every byte entering your application."**

---

## 1. Core Terminology Demystified

| Term | What It Means | Real-World Analogy |
| :--- | :--- | :--- |
| **Authentication (AuthN)** | Verifying **WHO** you are (Identity). | Showing your passport at airport customs. |
| **Authorization (AuthZ)** | Verifying **WHAT** you are permitted to do (Access Rights). | Showing a boarding pass granting access to Gate 14. |
| **Principal** | The currently authenticated user object (e.g. `UserDetails`). | The traveler holding the passport. |
| **Credentials** | Proof of identity (e.g. Password, JWT, Private Key, Biometric). | The secret passphrase or stamp proving the passport is genuine. |
| **Roles vs. Authorities** | **Role:** High-level persona (`ROLE_ADMIN`, `ROLE_CUSTOMER`).<br>**Authority:** Fine-grained permission (`product:delete`, `order:read`). | **Role:** Pilot.<br>**Authority:** License to fly a Boeing 777. |
| **SecurityContext** | Container holding the current `Authentication` token for the active thread. | The security badge worn by an employee while in a secure room. |
| **SecurityContextHolder** | Storage strategy (default: `ThreadLocal`) binding the `SecurityContext` to the executing thread. | The clip holding the security badge to your shirt. |

---

## 2. The Spring Security Request Pipeline

In a Servlet container (like Apache Tomcat), incoming HTTP requests traverse a chain of Servlet Filters before ever reaching Spring's `DispatcherServlet`.

Spring Security hooks into this mechanism using a bridge called **`DelegatingFilterProxy`**:

```
Client (HTTP Request)
       │
       ▼
[ Tomcat Servlet Container ]
       │
       ▼
[ DelegatingFilterProxy ] (Standard Servlet Filter registered in web container)
       │ (Delegates execution into Spring ApplicationContext)
       ▼
[ FilterChainProxy ] (Spring bean managing SecurityFilterChains)
       │
       ▼
[ SecurityFilterChain ] (Ordered pipeline of 15+ specialized filters)
       │
       ├─► 1. SecurityContextHolderFilter: Restores existing SecurityContext if session exists.
       ├─► 2. CorsFilter: Validates Origin, Methods, and Preflight OPTIONS requests.
       ├─► 3. CsrfFilter: Checks CSRF tokens (disabled for stateless REST APIs).
       ├─► 4. LogoutFilter: Clears SecurityContext upon logout request.
       ├─► 5. JwtAuthenticationFilter (OUR CUSTOM FILTER):
       │        - Reads "Authorization: Bearer <jwt>" header.
       │        - Validates HMAC signature & expiration.
       │        - Populates SecurityContextHolder with UsernamePasswordAuthenticationToken.
       │
       ├─► 6. UsernamePasswordAuthenticationFilter: Handles traditional form logins.
       ├─► 7. AnonymousAuthenticationFilter: Assigns 'ROLE_ANONYMOUS' if unauthenticated.
       ├─► 8. AuthorizationFilter (formerly FilterSecurityInterceptor):
       │        - Checks requestMatchers() rules (e.g. hasRole('ADMIN')).
       │        - Throws 401 Unauthorized or 403 Forbidden if access is denied!
       ▼
[ DispatcherServlet ] (Spring MVC Front Controller)
       │
       ▼
[ Controller ] (e.g. OrderController#placeOrder)
```

---

## 3. Session-Based vs. Stateless JWT Authentication

### Session-Based (Stateful):
- **How it works:** After login, server stores user session in RAM (or Redis) and issues a `JSESSIONID` cookie.
- **Drawbacks in Distributed Systems:** If you run 10 server replicas, either every server must replicate session state across the network, or you must configure sticky sessions on load balancers, complicating zero-downtime rolling deployments.

### JWT-Based (Stateless):
- **How it works:** After login, server signs an RFC 7519 JSON Web Token containing claims (`userId`, `email`, `roles`) and returns it.
- **Advantages:** The server stores **ZERO** session state in memory. Any server replica can verify the token's cryptographic signature in isolation without querying a central database on every HTTP request!

---

## 4. CSRF (Cross-Site Request Forgery) vs. CORS (Cross-Origin Resource Sharing)

### CSRF:
- **The Attack:** You are logged into `mybank.com` via a browser cookie. You visit a malicious site `evil.com`. That site has `<img src="http://mybank.com/transfer?to=attacker&amount=1000">`. Your browser automatically attaches the cookie!
- **Why we disable CSRF in Stateless APIs:** REST clients (mobile apps, React frontends storing JWT in memory or Authorization headers) do NOT rely on ambient browser cookies. Browsers never automatically attach custom `Authorization: Bearer` headers cross-origin!

### CORS:
- **The Security Guard:** A browser-enforced security policy preventing a script running on `http://localhost:3000` (frontend) from reading HTTP responses from `http://localhost:8080` (backend) unless the backend explicitly emits `Access-Control-Allow-Origin`.

---

## 5. Password Hashing: Why Plaintext & MD5/SHA-256 are Fatal

- **Plaintext:** Any database dump or SQL injection leak exposes all customer credentials immediately.
- **Fast Hashing (MD5, SHA-1, SHA-256):** Standard cryptographic hashes were designed to be **fast** (verifying gigabytes of files per second). Modern consumer GPUs can calculate **100,000,000,000 SHA-256 hashes per second**, cracking 8-character passwords via brute-force or precomputed Rainbow Tables in seconds!
- **BCrypt (The Industry Standard):**
  1. **Salt:** Generates a cryptographically random 16-byte salt per password, making rainbow tables impossible.
  2. **Work Factor (Cost):** Configurable slowness (e.g. 10 to 12 rounds = $2^{12}$ iterations). Takes ~100ms per attempt. To a legit user logging in, 100ms is unnoticeable. To an attacker trying 1 billion passwords, 100ms per attempt requires **3,170 years** of compute time!
