# Educational Exercise: Manual Dependency Injection Container

> **DISCLAIMER:** This is an educational simulation designed to demystify how Inversion of Control (IoC) and Dependency Injection (DI) work using Java Reflection. It is NOT how Spring is literally implemented internally.

---

## 1. What This Manual Container Demonstrates

1. **Inversion of Control (IoC):**
   - In traditional procedural code, `OrderProcessor` would do: `this.db = new DatabaseConnector()`.
   - With IoC, the class gives up control over instantiation. The container (`MiniApplicationContext`) instantiates classes, discovers dependencies via reflection, and injects them.

2. **Constructor Injection:**
   - Inspects `clazz.getDeclaredConstructors()`.
   - Reads parameter types (`Constructor.getParameterTypes()`).
   - Recursively calls `getBean(paramType)` to resolve and pass dependencies.

3. **Field Injection:**
   - Inspects fields annotated with `@MiniInject`.
   - Calls `field.setAccessible(true)` and sets the resolved instance.

4. **Singleton Scope:**
   - Caches instantiated objects in a `Map<Class<?>, Object> beanRegistry`.
   - Repeated calls to `getBean()` return the exact same instance in memory.

---

## 2. What Real Spring Does That This Manual Implementation DOES NOT Handle

| Feature | Our Manual Simulation | Real Spring `ApplicationContext` |
| :--- | :--- | :--- |
| **Classpath Scanning** | Requires passing classes explicitly | Scans bytecode using ASM (`SimpleMetadataReader`) without loading all classes into JVM heap |
| **Bean Lifecycle** | `newInstance()` then inject fields | 12+ phase lifecycle: `BeanDefinition` creation $\to$ `BeanFactoryPostProcessor` $\to$ Instantiation $\to$ Property Population $\to$ `BeanNameAware` $\to$ `ApplicationContextAware` $\to$ `@PostConstruct` / `InitializingBean` $\to$ `BeanPostProcessor` $\to$ Destruction (`@PreDestroy`) |
| **AOP Proxies** | None (returns raw instance) | Wraps beans in dynamic JDK or CGLIB proxies to inject `@Transactional`, `@Async`, `@Cacheable`, and security checks |
| **Scopes** | Singleton only | Singleton, Prototype, Request, Session, Application, WebSocket |
| **Circular Dependencies** | Throws `IllegalStateException` | Three-level cache (`singletonObjects`, `earlySingletonObjects`, `singletonFactories`) with ObjectFactory lambdas to expose uninitialized proxies |
| **Configuration Model** | Annotations only | `@Configuration`, `@Bean`, XML, Groovy, Functional Java DSL |
| **Conditional Registration**| None | `@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean` (The foundation of Spring Boot Auto-Configuration) |
| **Thread Safety** | Basic `synchronized` on `getBean` | Highly optimized concurrent lock-striping via `ConcurrentHashMap` |

---

## 3. How to Run

```powershell
mvn exec:java -Dexec.mainClass="com.backendlearninglab.examples.di.ManualDiDemo"
```
