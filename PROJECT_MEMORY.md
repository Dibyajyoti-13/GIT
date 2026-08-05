# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 4 (Completed)

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
- **pkt-line Framing Format**: Implemented `PktLine` helper to serialize/deserialize Git's 4-byte hex packet lines, including support for delimiter and flush packets.
- **Remote Git Reference Discovery**: Implemented `GitNetworkClient` to request `/info/refs?service=git-upload-pack` using Java `HttpClient`.
- **Database branch indexing**: Parsed reference discovery packet streams and mapped discovered branch HEADs (e.g., `refs/heads/*`) to the database `Branches` table via the `BranchDAO`.

### Phase 3: Packfile Negotiation, Demultiplexing, and Parsing
- **Sideband Channel Demultiplexing**: Implemented `SidebandDemuxer` to parse multiplexed smart protocol responses.
- **Git Delta Resolving Engine**: Implemented `DeltaResolver` decoding copy and insert commands.
- **Binary Packfile Parser**: Implemented `PackfileParser` to decode headers, inflate base objects, trace offset indices, apply delta compression chains, and store resolved loose files.
- **Upload-pack Negotiation**: Extended `GitNetworkClient` to negotiate fetches via smart protocol POST requests.

### Phase 4: BFS Checkout & Directory Reconstruction
- **Custom FIFO Queue**: Implemented [Queue](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/utils/Queue.java) backed by a Singly Linked List, ensuring complete control over memory layouts during BFS traversal.
- **Checkout Engine Service**: Developed [CheckoutService](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/checkout/CheckoutService.java) executing Breadth-First Search (BFS) starting at a target Commit's root tree, reconstructing directory subtrees, and generating physical files on disk from decompressed loose Blobs.
- **Clone Attempt Status Tracking**: Integrated database logs in `CheckoutService` to track state transitions (`IN_PROGRESS`, `SUCCESS`, `FAILED`) in the database `CloneHistory` table.
- **BFS Integration Tests**: Wrote comprehensive unit tests assembling mock tree objects, verifying filesystem extraction layouts, and asserting content integrity.

---

## Architectural Decisions

1. **Bare JDBC Database Access**: Direct JDBC queries were implemented rather than a heavy ORM framework to maintain low overhead.
2. **Singly Linked Queue for BFS**: Used a custom FIFO Queue to avoid importing standard Java collections, satisfying the systems-programming goal of implementing core algorithms manually.
3. **Graceful Database Fallbacks**: Both parsing and checkout engines gracefully handle offline/missing database connections, making development, unit testing, and execution robust across offline environments.

---

## TODO List / Next Step (Phase 5)
- [ ] Implement CLI parser in `com.gitclone.cli` accepting `<repository_url>` and `<destination_directory>`.
- [ ] Implement the main execution sequence connecting network discovery, packfile fetching, object parsing, and BFS checkout.
- [ ] Write integration test verifying complete end-to-end execution.