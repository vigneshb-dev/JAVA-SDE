# Clean Code, Refactoring & SOLID Principles Demos

Runnable software design laboratories comparing fragile anti-patterns against production-grade clean code:

| Demo Class | Location | Concepts Covered |
| :--- | :--- | :--- |
| **SOLID & Refactoring Demo** | `examples/cleancode/SolidAndRefactoringDemo.java` | Refactoring a 100-line "God Object" doing DB queries, discount math, payment charging, and SMTP emails into decoupled components adhering to Single Responsibility (SRP), Open/Closed (OCP), and Composition over Inheritance. |

### How to Run:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.cleancode.SolidAndRefactoringDemo"
```
