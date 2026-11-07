# Core Java Learning Demos

The runnable Core Java laboratories are located in `src/main/java/com/backendlearninglab/corejava/`:

| Module | Location | Concepts Covered |
| :--- | :--- | :--- |
| **OOP Fundamentals** | `corejava/oop/OopMasteryDemo.java` | Classes, constructors, encapsulation, inheritance, polymorphism, abstract classes, interfaces, records, rich enums, static/final keywords, `equals()`, `hashCode()`, `toString()`. |
| **Collections Mastery** | `corejava/collections/CollectionsMasteryDemo.java` | ArrayList vs LinkedList memory overhead, HashSet vs TreeSet vs LinkedHashSet, HashMap internal array of buckets, hash collision resolution (LinkedList to Red-Black Tree in Java 8+). |
| **Generics & Wildcards** | `corejava/generics/GenericsMasteryDemo.java` | Invariance of generic types, Bounded Generics, PECS (Producer-Extends, Consumer-Super), Type Erasure and bridge methods. |
| **Modern Java & Streams** | `corejava/functional/FunctionalAndStreamsDemo.java` | Lambda expressions, custom Functional Interfaces, Method References, Stream lazy evaluation, parallel streams pitfalls, `Optional` anti-patterns (`.get()` vs `.map()`). |
| **Date & Time API** | `corejava/datetime/DateTimeMasteryDemo.java` | Legacy `java.util.Date` mutability problems, thread-safe `Instant`, `LocalDate`, `ZonedDateTime`, and UTC storage. |
| **Exception Hierarchy** | `ExceptionMasteryDemo.java` | Checked vs Unchecked exceptions, try-with-resources and `AutoCloseable`, suppressing exceptions, custom exception designs. |

### How to Run Any Core Java Demo:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.corejava.oop.OopMasteryDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.corejava.collections.CollectionsMasteryDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.corejava.generics.GenericsMasteryDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.corejava.functional.FunctionalAndStreamsDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.corejava.datetime.DateTimeMasteryDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.ExceptionMasteryDemo"
```
