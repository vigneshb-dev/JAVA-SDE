# Educational Exercise: Manual REST HTTP Server (Without Spring MVC)

> **DISCLAIMER:** This is an educational simulation using Java's built-in `com.sun.net.httpserver.HttpServer` to reveal the vast amount of plumbing Spring MVC takes care of behind the scenes.

---

## 1. What You Had to Do Manually Here

1. **Socket & Server Lifecycle:**
   - Explicitly bind to port 8089 (`new InetSocketAddress(8089)`).
   - Manually manage the worker thread pool (`Executors.newFixedThreadPool(4)`).
   - Start and stop the server process.

2. **HTTP Routing & Method Matching:**
   - Manually check `exchange.getRequestMethod()` with `if/else` checks (`GET`, `POST`, `PUT`, `DELETE`).
   - Manually send `405 Method Not Allowed` when a verb is unsupported.

3. **URL Path Variable Parsing:**
   - Hand-craft Regular Expressions (`Pattern.compile("^/api/orders/(\\d+)$")`).
   - Extract string regex groups and manually convert strings to numbers (`Long.parseLong()`).
   - Manually handle `NumberFormatException` and return 400 Bad Request.

4. **Request Body Reading & Deserialization:**
   - Read bytes from `exchange.getRequestBody()` `InputStream`.
   - Manage byte-to-string UTF-8 encoding.
   - Manually parse JSON string data.

5. **Response Headers & Output Streaming:**
   - Set `Content-Type: application/json; charset=UTF-8` manually.
   - Calculate byte length and send HTTP status code (`exchange.sendResponseHeaders(200, bytes.length)`).
   - Flush and close the underlying output stream.

---

## 2. What Spring MVC Does Automatically (The DispatcherServlet Workflow)

```
HTTP Request
     │
     ▼
[ Tomcat / Netty ] (Accepts TCP socket)
     │
     ▼
[ DispatcherServlet ] (The Front Controller)
     │
     ├─► [ HandlerMapping ] (Finds @GetMapping matching "/api/orders/{id}")
     │
     ├─► [ HandlerAdapter ] (Coordinates method invocation)
     │         │
     │         ├─► [ HandlerMethodArgumentResolver ] 
     │         │     - @PathVariable: Converts "{id}" to Long automatically
     │         │     - @RequestBody: Invokes Jackson MappingJackson2HttpMessageConverter
     │         │     - @Valid: Runs Jakarta Bean Validation (Hibernate Validator)
     │         │
     │         ▼
     │   [ OrderController ] (Your clean business method)
     │         │
     │         ▼
     │   [ ResponseEntity<OrderResponse> ]
     │
     ├─► [ HttpMessageConverter ] (Jackson serializes Java DTO -> JSON bytes)
     │
     └─► [ HandlerExceptionResolver ] (Intercepts exceptions -> @RestControllerAdvice)
```

By understanding this manual server, you now appreciate that **Spring MVC is not magic**: it is a meticulously engineered pipeline of handlers, adapters, converters, and resolvers!

---

## 3. How to Run

```powershell
mvn exec:java -Dexec.mainClass="com.backendlearninglab.examples.server.ManualHttpServerDemo"
```
