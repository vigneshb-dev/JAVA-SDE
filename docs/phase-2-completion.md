# Phase 2 Completion Checklist: Manual Mastery Verification

Before moving to **Phase 3 (Spring Security, Redis Caching & Kafka Event Streaming)**, you MUST be able to complete every item on this checklist manually without relying on AI code generation or copying and pasting.

---

## 1. Spring Boot & Dependency Injection Core

- [ ] **ApplicationContext:** Can you explain the difference between a `BeanFactory` and an `ApplicationContext`?
- [ ] **Auto-Configuration:** Can you explain what `@EnableAutoConfiguration` does and how `@ConditionalOnClass` / `@ConditionalOnMissingBean` operate?
- [ ] **Constructor Injection:** Can you explain the 5 critical reasons why Constructor Injection is mandatory over Field Injection?
- [ ] **`@Bean` vs. `@Component`:** Can you explain when you MUST use `@Bean` instead of `@Component` (e.g. configuring 3rd-party library classes)?
- [ ] **Configuration Properties:** Can you bind an external hierarchy using `@ConfigurationProperties(prefix = "orderflow")` and explain why it is superior to `@Value`?

---

## 2. Spring MVC & REST API Design

- [ ] **DispatcherServlet Lifecycle:** Can you draw the full lifecycle from an incoming HTTP TCP socket through `HandlerMapping`, `HandlerAdapter`, `HandlerMethodArgumentResolver`, Controller, `HttpMessageConverter`, and back?
- [ ] **HTTP Verbs & Statuses:** Can you explain the exact semantic difference between `POST` (201 Created), `PUT` (200/204 Idempotent Replace), and `PATCH` (200 Partial Update)?
- [ ] **Request & Response DTOs:** Can you explain why returning JPA Entities directly from a controller causes circular JSON recursion (`StackOverflowError`) and security leaks?
- [ ] **Jakarta Bean Validation:** Can you validate nested collection payloads using `@Valid`, `@NotNull`, `@NotBlank`, and `@Size`?
- [ ] **Centralized Exception Handling:** Can you write a `@RestControllerAdvice` class with `@ExceptionHandler` to return consistent RFC-7807/custom JSON error responses for 400, 404, 409, 422, and 500?

---

## 3. JPA (Jakarta Persistence API) & Associations

- [ ] **Owning vs. Inverse Side:** Can you explain why the table containing the foreign key MUST be the owning side and why `mappedBy` belongs on the inverse side?
- [ ] **FetchType.LAZY Traps:** Can you explain why `@ManyToOne` defaults to `FetchType.EAGER` in JPA and why you must explicitly override it to `LAZY`?
- [ ] **Cascade & Orphan Removal:** Can you explain the exact difference between `CascadeType.REMOVE` and `orphanRemoval = true`?
- [ ] **Entity Identity in JPA:** Can you explain why you should never call collection fields inside a JPA entity's `equals()`, `hashCode()`, or `toString()`?
- [ ] **Assigned Primary Keys:** Can you explain why entities with manually assigned IDs require implementing `Persistable<ID>` in Spring Data JPA?

---

## 4. Hibernate 6 Internals & Transactions

- [ ] **The 4 Entity States:** Can you trace an entity transitioning through `Transient` $\to$ `Managed` $\to$ `Detached` $\to$ `Removed`?
- [ ] **Dirty Checking:** Can you explain why modifying a field inside a `@Transactional` method generates an SQL `UPDATE` without calling `save()`?
- [ ] **First-Level Cache:** Can you explain why calling `findById(1L)` twice within the same `@Transactional` method issues only 1 SQL query?
- [ ] **`@Transactional` Proxy Mechanics:** Can you explain how CGLIB proxies intercept transaction boundaries and why self-invocation (`this.method()`) completely bypasses `@Transactional`?
- [ ] **The N+1 Query Problem:** Can you reproduce the N+1 problem on a `@OneToMany` association and solve it using both JPQL `JOIN FETCH` and `@EntityGraph`?

---

## 5. Pure JDBC vs. JPA Comparison

- [ ] **Performance vs. Productivity:** Can you explain why bulk inserting 500,000 rows is significantly faster in Pure JDBC (`addBatch()`) than in JPA?
- [ ] **Hybrid Architecture:** Can you articulate when a high-scale microservice should use Spring Data JPA vs. when it should drop down to Pure JDBC / `JdbcClient`?

---

## 6. Manual Practical Verification Task

To prove you have mastered Phase 2:
1. Open a blank file or terminal.
2. Without looking at this project's code or prompting an AI:
   - Create a `@RestController` for a `PaymentController`.
   - Add a `POST /api/v1/payments` endpoint taking a `@Valid @RequestBody CreatePaymentRequest` DTO.
   - Inject `PaymentService` via Constructor Injection.
   - Inside the service, demonstrate `@Transactional` deducting a wallet balance and throwing a custom `InsufficientBalanceException` if balance < amount.
   - Map `InsufficientBalanceException` to HTTP 422 Unprocessable Entity inside a `@RestControllerAdvice`.

If you can write and explain this pipeline end-to-end, **you are officially ready for Phase 3 (Spring Security, Redis & Kafka)**!
