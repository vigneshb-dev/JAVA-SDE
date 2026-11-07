# Complete Maven Architecture & Engineering Guide

> **"Maven is not just a dependency downloader; it is an opinionated, standardized declarative build lifecycle system."**

---

## 1. Coordinates: The GAV (GroupId, ArtifactId, Version)

Every Java library and application in the world is uniquely identified by three coordinates:

```xml
<groupId>com.backendlearninglab</groupId>
<artifactId>orderflow-backend</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

- **`groupId`**: Reverse domain name representing the organization or developer controlling the project (e.g., `org.postgresql`, `com.zaxxer`, `com.backendlearninglab`). Prevents namespace collisions.
- **`artifactId`**: The specific name of this project/module. By convention, this becomes the base filename of the generated `.jar` file.
- **`version`**: The release or build identifier.
  - `SNAPSHOT`: Indicates an active, mutable work-in-progress build. Maven Central rejects SNAPSHOT uploads.
  - Semantic Versioning (`MAJOR.MINOR.PATCH`): e.g., `1.0.0`, `42.7.3`.

---

## 2. Dependency Scopes

Maven dependencies have a `<scope>` tag that tells the compiler and packaging plugins where and when that JAR file is needed:

| Scope | Compiling Main Code (`src/main`) | Compiling Test Code (`src/test`) | Execution Runtime | Packaged into Final JAR | Real-World Example |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **`compile`** (default) | **Yes** | **Yes** | **Yes** | **Yes** | `postgresql`, `HikariCP` |
| **`test`** | **No** | **Yes** | Only in test runner | **No** | `junit-jupiter`, `h2` |
| **`provided`** | **Yes** | **Yes** | **No** (Provided by Host/OS) | **No** | `servlet-api` (Tomcat) |
| **`runtime`** | **No** | **Yes** | **Yes** | **Yes** | Logging implementations |
| **`system`** | **Yes** | **Yes** | **No** (Manual file path) | **No** | Legacy local `.jar` files |

> [!WARNING]
> **Common Mistake:** Putting test libraries like JUnit or Mockito in `compile` scope. 
> This pollutes production artifacts with testing classes and inflates container image sizes!

---

## 3. The Maven Build Lifecycles & Phases

Maven has three built-in lifecycles: **`clean`**, **`default`** (build), and **`site`**.

### The `default` Lifecycle (In Sequential Execution Order):

Running a later phase **automatically executes all preceding phases**!

```
1. validate  -> Verifies project structure and that all pom.xml info is available.
2. compile   -> Compiles source code in src/main/java into target/classes.
3. test      -> Compiles src/test/java and executes tests using Surefire.
4. package   -> Takes target/classes and packages them into target/orderflow-backend-1.0.0.jar.
5. verify    -> Runs integration tests against the packaged JAR.
6. install   -> Copies the packaged JAR into your local repository (~/.m2/repository).
7. deploy    -> Uploads the final artifact to remote Nexus/Artifactory/Maven Central.
```

If you type `mvn package`, Maven automatically runs:
`validate` -> `compile` -> `test` -> `package`.

---

## 4. Transitive Dependencies & The "Nearest-Wins" Rule

If Project **A** depends on **B**, and **B** depends on **C (v1.0)**, then Project **A** automatically receives **C (v1.0)**. This is a **transitive dependency**.

### The Version Conflict Problem:
Suppose:
- App -> Library X -> Jackson 2.12
- App -> Library Y -> Jackson 2.15

Which version of Jackson ends up on the classpath?

### The "Nearest-Definition-Wins" Rule:
Maven resolves conflicting versions by tree depth:
1. The dependency closest to the root `pom.xml` in the tree wins.
2. If depth is equal, the **first declared** in the `pom.xml` wins.

### How to Diagnose & Fix:

```powershell
# 1. Print the entire resolved dependency hierarchy:
mvn dependency:tree

# 2. Filter for a specific problematic library:
mvn dependency:tree -Dincludes=com.fasterxml.jackson*
```

### Overriding Conflicts with `<dependencyManagement>`:
In parent POMs or root configurations, specify the exact version you mandate:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 5. Essential Maven Commands Reference

| Command | What It Does Internally |
| :--- | :--- |
| `mvn clean` | Deletes the `target/` directory and all previous compilation output. |
| `mvn compile` | Compiles `src/main/java` into `target/classes`. |
| `mvn test` | Compiles and executes unit tests in `src/test/java`. |
| `mvn test -Dtest=OrderServiceTransactionTest` | Runs only one specific test class. |
| `mvn package` | Runs tests and builds `target/orderflow-backend-1.0.0-SNAPSHOT.jar`. |
| `mvn package -DskipTests` | Builds the JAR while skipping test execution (use sparingly!). |
| `mvn dependency:tree` | Visualizes the dependency tree and transitive resolution. |
| `mvn clean compile exec:java` | Compiles and runs the main class directly. |
