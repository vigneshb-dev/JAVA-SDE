# The Anti-Vibe-Coding Manifesto & Protocol

> **"If you cannot explain why a line of code exists, you do not own that code — the code owns you. AI must be your tutor, never your ghostwriter."**

---

## 1. What is "Vibe Coding" and Why Does It Ruin Engineers?

**Vibe Coding** is the habit of generating code through AI prompts without understanding the mechanical internals:
- Prompting: *"Build JWT authentication with Redis and Kafka for me."*
- Pasting 500 lines of AI-generated boilerplate into a project.
- Celebrating when it runs once on `localhost` without compiler errors.
- **The Inevitable Catastrophe:** In production, when a `LazyInitializationException`, connection pool exhaustion, memory leak, or consumer rebalance storm occurs at 3:00 AM, the vibe coder has no mental model to diagnose the failure. They become helpless because they never understood the underlying mechanics.

---

## 2. The Strict Rules of Engagement with AI

### Rule 1: The Inversion of Inquiry
❌ **DO NOT ASK AI:**
> *"Write a Spring Security JWT filter and controller for me."*

✅ **INSTEAD ASK AI:**
> *"Explain how Spring Security's `OncePerRequestFilter` intercepts requests before `DispatcherServlet`. What thread-local storage does `SecurityContextHolder` use, and why is `ThreadLocal.remove()` necessary at the end of the request?"*

Then **write the code yourself**, line by line.

### Rule 2: How to Legitimized Use AI as an Engineering Tool

| Role | What AI Should Do | What YOU Must Do |
| :--- | :--- | :--- |
| **As Teacher** | Explain theory, internal mechanics, and failure modes. | Synthesize the theory into executable code by hand. |
| **As Reviewer** | Critique your completed code for edge cases, resource leaks, or race conditions. | Defend or refactor your design choices based on principles. |
| **As Debugger** | Help interpret cryptic JVM stack traces or deadlocks. | Set breakpoints, inspect memory, and verify the root cause. |
| **As Doc Assistant**| Format architectural guides and Markdown tables. | Verify that every assertion in the documentation is mathematically and practically true. |

---

## 3. The "Before Asking AI for Code" Checklist

Before you ever ask an AI or search engine for an implementation pattern, you must write down answers to these 9 engineering questions in your notebook or PR description:

```
+───────────────────────────────────────────────────────────────────────────────+
|                  THE 9-POINT PRE-IMPLEMENTATION CHECKLIST                     |
+───────────────────────────────────────────────────────────────────────────────+
| 1. WHAT PROBLEM AM I SOLVING?                                                 |
|    Define the exact business invariant without mentioning any framework.     |
|                                                                               |
| 2. WHAT CLASSES ARE NEEDED?                                                   |
|    List the entity, DTO, repository, service, and controller explicitly.      |
|                                                                               |
| 3. WHAT DATA FLOWS THROUGH THEM?                                              |
|    Trace the request payload as it transforms from JSON -> DTO -> Entity.     |
|                                                                               |
| 4. WHAT EXCEPTIONS CAN OCCUR?                                                 |
|    Anticipate 404, 400 validation, 409 duplicate SKU, and 422 stock errors.  |
|                                                                               |
| 5. WHAT DATABASE OPERATION OCCURS?                                            |
|    Write out the exact SQL SELECT/INSERT/UPDATE before letting JPA do it.     |
|                                                                               |
| 6. WHAT HAPPENS IF THE DATABASE FAILS?                                        |
|    Does @Transactional roll back all mutations cleanly?                       |
|                                                                               |
| 7. WHAT HAPPENS CONCURRENTLY?                                                 |
|    What if 2 threads buy the final stock item at the exact same millisecond?  |
|                                                                               |
| 8. WHAT HAPPENS IF THE REQUEST IS REPEATED?                                   |
|    Is the endpoint or consumer idempotent? Will the customer be billed twice? |
|                                                                               |
| 9. WHAT HAPPENS IF REDIS OR KAFKA IS UNAVAILABLE?                             |
|    Does the application crash completely or degrade gracefully to the DB?     |
+───────────────────────────────────────────────────────────────────────────────+
```

---

## 4. The 10-Step Mastery Loop for Every Technology

For every new library, framework, or architectural pattern:
1. **Learn the concept:** Read the core philosophy and mechanical constraints.
2. **Run smallest example:** Execute the bare minimum runnable `main()` method.
3. **Read implementation:** Inspect the bytecode or framework source code (`F12` / `Ctrl+Click` into Spring/Hibernate classes).
4. **Break implementation intentionally:** Modify properties, drop network connections, pass nulls, or trigger concurrency race conditions.
5. **Debug:** Use debugger breakpoints, stack traces, and SQL logs.
6. **Fix:** Remedy the issue using architectural patterns (e.g. `JOIN FETCH`, DLT, In-memory fallback).
7. **Rewrite without looking:** Delete the file and reconstruct it from memory and first principles.
8. **Implement project feature:** Integrate the concept cleanly into the main OrderFlow backend.
9. **Explain it verbally:** Explain the concept out loud as if teaching a junior developer or interviewer.
10. **Build a small variation without AI:** Create a novel twist on the problem without opening a chat prompt.
