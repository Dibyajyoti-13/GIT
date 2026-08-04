# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 2 (Completed)

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
- **Custom Doubly Linked List LRU Cache**: Implemented `LruCache` for $O(1)$ memory mapping.
- **zlib Compression Utilities**: Implemented `CompressionUtils` for deflate/inflate compression streams.
- **SHA-1 Crypto Utilities**: Implemented `HashUtils` for hashing byte streams.
- **Git Object Models**: Implemented `BlobObject`, `TreeObject`, and `CommitObject`.
- **Tree Data Structure**: Implemented recursive file mode/path sorting rules inside `TreeObject`.
- **Loose Object Storage Service**: Implemented `ObjectStorageService` to save and read loose files under `.git/objects/ab/cdef...` and register metadata in the database index.

### Phase 2: Network Layer & Git Smart HTTP Protocol
- **pkt-line Framing Format**: Implemented [PktLine](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/network/PktLine.java) helper to serialize/deserialize Git's 4-byte hex packet lines, including support for delimiter and flush packets.
- **Remote Git Reference Discovery**: Implemented [GitNetworkClient](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/network/GitNetworkClient.java) to request `/info/refs?service=git-upload-pack` using Java `HttpClient`.
- **Database branch indexing**: Parsed reference discovery packet streams and mapped discovered branch HEADs (e.g., `refs/heads/*`) to the database `Branches` table via the `BranchDAO`.
- **Unit Testing Coverage**: Developed mock test servers using JDK's standard `HttpServer` to test reference advertisement parsing, integrity checks, and data store indexing without external web dependencies.

---

## Architectural Decisions

1. **Bare JDBC Database Access**: Direct JDBC queries were implemented rather than a heavy ORM framework to maintain low overhead.
2. **PktLine List Abstraction**: Stream parsing parses packet boundaries into a sequential `List<byte[]>` which keeps logic clean and isolates it from network sockets.
3. **Mock HTTP Server for Integration Tests**: Used `com.sun.net.httpserver.HttpServer` in unit tests, allowing self-contained and reproducible testing of protocol semantics.

---

## TODO List / Next Step (Phase 3)
- [ ] Implement Git Smart protocol Upload-Pack POST call to request object packs (`git-upload-pack`).
- [ ] Parse sideband multiplexing (demux channel 1 for raw packfile, channel 2 for progress).
- [ ] Parse packfile header (signature `PACK`, version, number of objects).
- [ ] Support delta compression reconstruction (rebuilding delta objects with OBJ_OFS_DELTA and OBJ_REF_DELTA offset base lookup).