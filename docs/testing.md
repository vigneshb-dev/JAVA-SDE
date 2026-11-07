# Java Backend Testing Philosophy & Practice Guide

> **"If you only test the happy path, you have not tested your software. Production is defined by how gracefully your code fails."**

---

## 1. The Testing Pyramid in Enterprise Java

```
             / \
            /   \
           / E2E \         Slowest, brittle, tests full deployed stack
          /───────\
         /  Integ  \       Tests Spring Context, DB (H2/Testcontainers), Redis, Kafka
        /───────────\
       /    Unit     \     Fastest, tests single classes in isolation with Mockito
      /───────────────\
```

### The 3 Testing Tiers in OrderFlow:

| Tier | Scope | Speed | Annotation / Tooling | What to Test |
| :--- | :--- | :--- | :--- | :--- |
| **Unit Tests** | Single class, no Spring context | ~5 ms | JUnit 5 (`@Test`), Mockito (`@Mock`, `@InjectMocks`) | Core domain math, discount policies, state machines, input validation. |
| **Service Integration** | Spring Context + Database | ~500 ms | `@SpringBootTest`, `@ActiveProfiles("dev")` | `@Transactional` rollback boundaries, Hibernate dirty checking, repository queries. |
| **Web MVC Integration**| DispatcherServlet HTTP pipeline | ~100 ms | `@SpringBootTest`, `@AutoConfigureMockMvc`, `@WithMockUser` | JSON serialization, validation errors (400), authentication (401), authorization (403). |

---

## 2. Mockito: What to Mock vs. What NOT to Mock

A major beginner trap is **mocking everything**. When you mock repositories, database transactions, and entity managers, your tests test your mocks, not your software!

### Rule 1: DO NOT Mock the Database in Transaction Tests
- **Why?** Mockito cannot verify `@Transactional` rollback behavior! A mocked repository will never throw a `DataIntegrityViolationException` or test foreign key constraints.
- **Best Practice:** Use an embedded in-memory database (H2 in PostgreSQL mode) or Docker Testcontainers.

### Rule 2: DO NOT Mock What You Don't Own
- Avoid mocking complex third-party library internals (e.g., Hibernate's `EntityManager` or `SecurityContextHolder`).
- Mock at the boundaries of your own interfaces (e.g. `PaymentGateway`, `SmsService`).

### Rule 3: DO Mock External Network Boundaries
- In unit tests for payment processing, mock the external Stripe or PayPal HTTP client so your test suite does not depend on internet access or real credit cards.

---

## 3. How to Test Each Major Technology

### 1. Testing Core Java & Domain Rules (Pure JUnit 5)
```java
@Test
void testOrderTotalCalculation() {
    DiscountPolicy policy = new DiscountPolicy();
    BigDecimal discounted = policy.applyDiscounts(new BigDecimal("150.00"));
    assertEquals(new BigDecimal("135.00"), discounted);
}
```

### 2. Testing `@Transactional` Rollbacks
```java
@Test
void testTransactionRollsBackOnStockFailure() {
    assertThrows(InsufficientStockException.class, () ->
        orderJpaService.placeOrder(invalidOrder)
    );
    // Crucial: Query the database to verify zero partial records were saved!
    assertEquals(0, orderRepository.count());
}
```

### 3. Testing Spring Security (401 & 403)
```java
@Test
void testRegularUserCannotAccessAdminEndpoints() throws Exception {
    mockMvc.perform(get("/api/v1/users")
            .header("Authorization", "Bearer " + userJwtToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)));
}
```

### 4. Testing Redis Caching (`@Cacheable` & `@CachePut`)
```java
@Test
void testCacheHitAvoidsDatabase() {
    // 1st call hits DB and populates cache
    ProductResponse p1 = productService.getProductById(101L);
    
    // Inspect Spring Cache directly
    Cache cache = cacheManager.getCache("products");
    assertNotNull(cache.get(101L));
}
```

### 5. Testing Asynchronous Kafka & Outbox
```java
@Test
void testOutboxEventSavedOnOrderPlacement() {
    orderJpaService.placeOrder(request);
    List<OutboxEventEntity> outbox = outboxRepository.findAll();
    assertFalse(outbox.isEmpty(), "Outbox event must be persisted atomically!");
}
```
