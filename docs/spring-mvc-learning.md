# Spring MVC & REST API Architecture Guide

---

## 1. Concept: The Front Controller Pattern & DispatcherServlet

Spring MVC is an implementation of the **Front Controller Architecture Pattern**.
Instead of configuring a separate Java Servlet for every endpoint (like historical `UserServlet`, `OrderServlet`), Spring MVC routes **all incoming HTTP requests** through a single master servlet called the **`DispatcherServlet`**.

### The Complete Request Lifecycle:

```
1. Client (Browser / Postman / Mobile App) sends HTTP Request: POST /api/v1/orders
   │
   ▼
2. Web Server (Embedded Apache Tomcat) accepts TCP connection and dispatches to:
   │
   ▼
3. DispatcherServlet (The Front Controller)
   │
   ├─► 4. HandlerMapping: Looks up which Controller & method matches the URL & HTTP Method
   │      (e.g., OrderController#placeOrder)
   │
   ├─► 5. HandlerExecutionChain: Runs configured HandlerInterceptors (preHandle: Auth, Logging)
   │
   ├─► 6. HandlerAdapter: Prepares to invoke the method:
   │      │
   │      ├─► HandlerMethodArgumentResolver:
   │      │   - @PathVariable: parses URL segments
   │      │   - @RequestBody: invokes Jackson HttpMessageConverter to parse JSON into DTO
   │      │   - @Valid: invokes Hibernate Validator; throws MethodArgumentNotValidException if invalid!
   │      │
   │      ▼
   │   7. OrderController method executes (Delegates to OrderJpaService)
   │      │
   │      ▼
   ├─► 8. Return Value Handling:
   │      - Because class has @RestController, RequestResponseBodyMethodProcessor is used
   │      - MappingJackson2HttpMessageConverter serializes the Response DTO to JSON bytes
   │
   └─► 9. Exception Handling (if any):
          HandlerExceptionResolver intercepts error and routes to @RestControllerAdvice!
```

---

## 2. Small Isolated Example

```java
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        return ApiResponse.ok(itemService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemResponse> getItem(@PathVariable Long id) {
        return ApiResponse.ok(itemService.getById(id));
    }
}
```

---

## 3. OrderFlow Project Implementation

Examine:
- [UserController.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/controller/UserController.java)
- [ProductController.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/controller/ProductController.java)
- [InventoryController.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/controller/InventoryController.java)
- [OrderController.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/controller/OrderController.java)
- [GlobalExceptionHandler.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/exception/GlobalExceptionHandler.java)

---

## 4. Common Mistake: Returning JPA Entities Directly From Controllers

```java
// CRITICAL MISTAKE:
@GetMapping("/{id}")
public OrderEntity getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}
```

### Why this is a disaster:
1. **Infinite Circular Recursion:** `OrderEntity` references `OrderItemEntity`, which references `OrderEntity`. Jackson tries to serialize both in an infinite loop, crashing with `StackOverflowError`!
2. **Security Leak:** Exposes internal database fields like `password_hash`, soft-delete flags, or private supplier costs.
3. **LazyInitializationException:** Jackson accesses un-initialized `FetchType.LAZY` collections after the database transaction has already closed!
4. **Breaking Client Contracts:** Renaming a database column instantly breaks mobile apps and external consumers.

**The Fix:** Always use dedicated **Request DTOs** and **Response DTOs** (e.g., `CreateOrderRequest` and `OrderResponse`).

---

## 5. Debugging Method: Tracing Spring MVC Dispatching

Add this to `application.properties`:
```properties
logging.level.org.springframework.web=DEBUG
```

When an HTTP request arrives, the console prints:
```text
DEBUG DispatcherServlet - POST "/api/v1/orders", parameters={}
DEBUG RequestMappingHandlerMapping - Mapped to com.backendlearninglab.spring.controller.OrderController#placeOrder(CreateOrderRequest)
DEBUG RequestResponseBodyMethodProcessor - Read "application/json;charset=UTF-8" to [CreateOrderRequest[...]]
DEBUG RequestResponseBodyMethodProcessor - Using 'application/json', given [*/*] and supported [application/json]
DEBUG RequestResponseBodyMethodProcessor - Writing [ApiResponse[...]]
DEBUG DispatcherServlet - Completed 201 CREATED
```

---

## 6. Performance Consideration: Blocking Controllers vs. Virtual Threads

By default, Apache Tomcat allocates a pool of 200 OS threads (`server.tomcat.threads.max=200`).
If your controller makes blocking I/O calls (e.g. slow database queries or remote payment gateways taking 500ms), 200 concurrent requests will exhaust the thread pool, causing all subsequent incoming connections to queue and time out (504 Gateway Timeout).

**Modern Java Solution:**
In Java 21+, enable virtual threads in `application.properties`:
```properties
spring.threads.virtual.enabled=true
```
Tomcat will spawn lightweight Virtual Threads (fibers) per request, handling 50,000+ concurrent connections effortlessly!

---

## 7. Senior Interview Question

**Question:** *"Why does the 3-tier architecture (Controller -> Service -> Repository) exist? Why not call repositories directly from controllers?"*

**Answer:**
1. **Separation of Concerns:**
   - **Controller:** Strictly responsible for HTTP transport mechanics (routing, path variables, status codes, JSON serialization, headers). It should contain ZERO business rules.
   - **Service:** The heartbeat of domain logic. Enforces business rules, coordinates multiple repositories, manages transaction boundaries (`@Transactional`), and maps entities to DTOs.
   - **Repository:** Strictly responsible for database persistence and querying.
2. **Reusability & Multi-Channel Support:** If tomorrow your system adds an asynchronous Kafka message listener, a CLI tool, or a scheduled cron job, those components can invoke the exact same `OrderService` without needing any HTTP controller!
3. **Unit Testing:** You can test complex checkout algorithms in `OrderServiceTest` by mocking repository interfaces in pure Java without running a web server.
