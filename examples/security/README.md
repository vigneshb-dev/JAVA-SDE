# Spring Security & Cryptography Demos

Runnable security laboratories comparing vulnerable beginner practices against production-grade implementations:

| Demo Class | Location | Concepts Covered |
| :--- | :--- | :--- |
| **Plaintext vs SHA-256 vs BCrypt** | `examples/security/PlaintextVsBCryptDemo.java` | Why plaintext leaks credentials, why fast SHA-256 is vulnerable to GPU brute-force and precomputed Rainbow Tables, and how BCrypt's adaptive work factor ($2^{12} = 4096$ rounds) and per-user salt provide resilience. |
| **Pure Java Manual JWT Demo** | `examples/security/ManualJwtDemo.java` | Building a signed JSON Web Token from first principles using `java.security.Mac` (HMAC-SHA256) and `Base64.getUrlEncoder()`. Proving tokens are signed (not encrypted) and verifying tamper rejection. |

### How to Run:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.security.PlaintextVsBCryptDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.security.ManualJwtDemo"
```
