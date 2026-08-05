# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 3 (Completed)

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
- **Sideband Channel Demultiplexing**: Implemented [SidebandDemuxer](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/network/SidebandDemuxer.java) to parse multiplexed smart protocol responses, routing packfile bytes to the target output stream while extracting stderr logging.
- **Git Delta Resolving Engine**: Implemented [DeltaResolver](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/DeltaResolver.java) decoding copy and insert commands to assemble delta-compressed Git objects (`OBJ_OFS_DELTA` / `OBJ_REF_DELTA`).
- **Binary Packfile Parser**: Implemented [PackfileParser](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/git/PackfileParser.java) to decode variable-length size headers, inflate base objects, trace offset indices, apply delta compression chains, and store resolved loose files.
- **Upload-pack Negotiation**: Extended `GitNetworkClient` to negotiate fetches via smart protocol POST requests.
- **Verification Coverage**: Wrote tests generating mock packfiles dynamically and validating delta command operations.

---

## Architectural Decisions

1. **Bare JDBC Database Access**: Direct JDBC queries were implemented rather than a heavy ORM framework to maintain low overhead.
2. **Sideband Demultiplexing Stream**: Sideband streams are routed packet-by-packet to avoid reading the entire payload into RAM at once, preventing memory exhaustion on larger clones.
3. **Offset Resolution Map**: Used a simple `Long -> ResolvedObject` mapping during packfile decoding to instantly resolve `OBJ_OFS_DELTA` references back to their unpacked parent byte arrays in memory.

---

## TODO List / Next Step (Phase 4)
- [ ] Implement BFS Queue-based directory checkout tree traversal (`CheckoutService.java`).
- [ ] Reconstruct directories and write Blobs into physical files on disk.
- [ ] Record clone attempt state transitions (IN_PROGRESS, SUCCESS, FAILED) in the `CloneHistory` table.
- [ ] Write integration test performing a complete clone and checkout simulation.