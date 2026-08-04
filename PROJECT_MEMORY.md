# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 0 (Completed)

---

## Completed Work

### Phase 0: Infrastructure & Environment Setup
- **Environment Verification**: Verified JDK 25 (Java 21+), Maven 3.9.9, and MariaDB 11.8.8 compatibility.
- **Maven Configuration**: Initialized `pom.xml` with dependencies for MariaDB JDBC client, SLF4J, Logback, and JUnit 5 testing framework.
- **Logging Configuration**: Formatted Logback console and file logging (`src/main/resources/logback.xml`).
- **Database Schema**: Created `src/main/resources/schema.sql` defining `Repositories`, `CloneHistory`, `GitObjects`, and `Branches` tables.
- **JDBC Connection Management**: Developed `DatabaseConfig.java` to load parameters from `db.properties` and `DatabaseConnectionManager.java` for Connection lifecycle control and automatic schema initialization.
- **DAO Abstractions**: Implemented models and JDBC CRUD DAO logic for `Repository`, `CloneHistory`, `GitObject`, and `Branch` entities.
- **Architecture Structure**: Established physical directory/package structure with `package-info.java` placeholder markers.
- **Testing Verification**: Created `DatabaseConnectionManagerTest` unit test demonstrating connectivity, schema execution, and transactional model CRUD. The test suite builds and passes successfully.

---

## Architectural Decisions

1. **Bare JDBC Database Access**: Direct JDBC queries were implemented rather than a heavy ORM framework to maintain low overhead and direct control over SQL query compatibility.
2. **Schema Control**: The application manages its own schema setup on startup via resource-based SQL scripts in `DatabaseConnectionManager`, improving local setup simplicity.
3. **Structured Package Decomposition**: Follows a decoupled modular layout dividing cli, core, git, network, checkout, database, cache, models, utils, and exceptions.

---

## TODO List / Next Step (Phase 1)
- [ ] Implement Git Object representation (Blob, Tree, Commit, Tag).
- [ ] Create SHA-1 hashing helpers and zlib compression/decompression utilities.
- [ ] Develop custom parser to read Git objects from loose file structure.
- [ ] Connect the object models to the `GitObjects` database DAO.