# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 1 (Completed)

---

## Completed Work

### Phase 0: Infrastructure & Environment Setup
- **Environment Verification**: Verified JDK 25 (Java 21+), Maven 3.9.9, and MariaDB 11.8.8 compatibility.
- **Maven Configuration**: Initialized `pom.xml` with dependencies for MariaDB JDBC client, SLF4J, Logback, and JUnit 5 testing framework.
- **Logging Configuration**: Formatted Logback console and file logging (`src/main/resources/logback.xml`).
- **Database Schema**: Created `src/main/resources/schema.sql` defining database tables.
- **JDBC Connection Management**: Developed connection configurations and lifecycle helpers.
- **DAO Abstractions**: Implemented JDBC CRUD DAO logic for `Repository`, `CloneHistory`, `GitObject`, and `Branch` entities.

### Phase 1: Git Object Model, Hashing, Compression & Loose Storage
- **Custom Doubly Linked List LRU Cache**: Implemented [LruCache](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/cache/LruCache.java) for $O(1)$ memory mapping of recently accessed Git objects.
- **zlib Compression Utilities**: Implemented [CompressionUtils](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/utils/CompressionUtils.java) for deflate/inflate compression streams.
- **SHA-1 Crypto Utilities**: Implemented [HashUtils](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/utils/HashUtils.java) for hashing byte streams and converting hex-binary.
- **Git Object Models**: Implemented object inheritance mapping [BlobObject](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/BlobObject.java), [TreeObject](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/TreeObject.java), and [CommitObject](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/CommitObject.java).
- **Tree Data Structure**: Implemented recursive file mode/path sorting rules inside `TreeObject` to match standard Git specifications.
- **Loose Object Storage Service**: Implemented [ObjectStorageService](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/ObjectStorageService.java) to save and read compressed loose files under `.git/objects/ab/cdef...` and register metadata in the database index.
- **Testing Coverage**: Added full testing suites verifying LruCache eviction, zlib compression, SHA-1 calculations, and object storage serialization/deserialization.

---

## Architectural Decisions

1. **Bare JDBC Database Access**: Direct JDBC queries were implemented rather than a heavy ORM framework to maintain low overhead.
2. **Immutability of Git Objects**: `GitObjectBase` instances are constructed with immutable properties to mirror the read-only property of physical Git objects.
3. **Decoupled Serializer/Parsers**: Serialization and parsing logic is kept inside the respective model classes to localize format constraints.

---

## TODO List / Next Step (Phase 2)
- [ ] Implement Git Smart HTTP Protocol client.
- [ ] Make GET requests for `/info/refs?service=git-upload-pack` and parse references and capabilities.
- [ ] Handle pkt-line format (Packet line formatting e.g., `001e# service=git-upload-pack\n`).
- [ ] Write integration/unit tests for network packet serialization.