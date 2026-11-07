# Spring Boot, Spring MVC, JPA & Hibernate Demos

The runnable Spring laboratories are located in `src/main/java/com/backendlearninglab/spring/`:

| Module | Location | Concepts Covered |
| :--- | :--- | :--- |
| **Manual IoC Container** | `examples/manual-di-container/` | Implementing a custom micro IoC container from scratch using Java Reflection, discovering `@MiniComponent`, and performing constructor/field injection. |
| **Manual HTTP REST Server** | `examples/manual-rest-server/` | Building a raw HTTP server using `com.sun.net.httpserver` to teach what Spring MVC's `DispatcherServlet` abstracts away. |
| **Constructor Injection** | `spring/di/ConstructorInjectionDemo.java` | Why constructor injection is mandatory over field injection (immutability, fail-fast compilation, mockability). |
| **Dirty Checking** | `spring/hibernate/demo/EntityLifecycleAndDirtyCheckingDemo.java` | Demonstrating the Hibernate Persistence Context snapshot mechanism: modifying a managed entity automatically triggers an `UPDATE` query upon commit without calling `save()`. |
| **The N+1 Query Problem** | `spring/hibernate/demo/NPlusOneProblemDemo.java` | Reproducing lazy-loading query storms ($1 + N$ queries) and eliminating them using JPQL `JOIN FETCH` and Spring Data `@EntityGraph`. |

### How to Run:
```bash
# Manual reflection-based IoC container
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.di.ManualDiDemo"

# Manual raw HTTP REST server
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.server.ManualHttpServerDemo"

# Hibernate dirty checking demo
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.spring.hibernate.demo.EntityLifecycleAndDirtyCheckingDemo"

# Hibernate N+1 problem & JOIN FETCH demo
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.spring.hibernate.demo.NPlusOneProblemDemo"
```
