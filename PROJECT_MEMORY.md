# Project Memory - GitCloneJava

## Project Purpose
Build a simplified, educational implementation of **Git Clone** completely from scratch in Java to understand Git internals, without using JGit or any third-party Git libraries.

---

## Current Phase: Phase 5 (Completed)

---

## Completed Work

### Phase 0: Infrastructure & Environment Setup
- **Environment Verification**: Verified JDK 25 (Java 21+), Maven 3.9.9, and MariaDB 11.8.8 compatibility.
- **Maven Configuration**: Initialized `pom.xml` with dependencies for MariaDB JDBC client, SLF4J, Logback, and JUnit 5 testing framework.
- **Logging Configuration**: Formatted Logback console and file logging.
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
- **pkt-line Framing Format**: Implemented `PktLine` helper to serialize/deserialize Git's 4-byte hex packet lines.
- **Remote Git Reference Discovery**: Implemented `GitNetworkClient` to request `/info/refs?service=git-upload-pack`.
- **Database branch indexing**: Parsed reference discovery packet streams and mapped discovered branch HEADs to the database `Branches` table.

### Phase 3: Packfile Negotiation, Demultiplexing, and Parsing
- **Sideband Channel Demultiplexing**: Implemented `SidebandDemuxer` to parse multiplexed smart protocol responses.
- **Git Delta Resolving Engine**: Implemented `DeltaResolver` decoding copy and insert commands.
- **Binary Packfile Parser**: Implemented `PackfileParser` to decode headers, inflate base objects, trace offset indices, apply delta compression chains, and store resolved loose files.
- **Upload-pack Negotiation**: Extended `GitNetworkClient` to negotiate fetches via smart protocol POST requests.

### Phase 4: BFS Checkout & Directory Reconstruction
- **Custom FIFO Queue**: Implemented `Queue` backed by a Singly Linked List.
- **Checkout Engine Service**: Developed `CheckoutService` executing BFS commit tree traversal and filesystem extraction.
- **Clone Attempt Status Tracking**: Integrated database logs in `CheckoutService` to track state transitions in the database `CloneHistory` table.

### Phase 5: CLI Entrypoint & End-to-End Git Clone Execution
- **GitCloneCLI Entry Point**: Implemented [GitCloneCLI](file:///home/delex/Documents/Playground/GIT/src/main/java/com/gitclone/cli/GitCloneCLI.java) connecting all layers in a unified sequence: DB creation, URL parsing, Smart HTTP reference discovery, upload-pack negotiation, packfile binary parsing, and BFS file extraction.
- **Testable Refactoring**: Extracted core clone flow into `cloneRepository` method to allow JUnit integration tests to assert operations without calling `System.exit()`.
- **E2E Integration Testing**: Created [GitCloneCLITest](file:///home/delex/Documents/Playground/GIT/src/test/java/com/gitclone/cli/GitCloneCLITest.java) spinning up a mock HTTP server, negotiating and transmitting dynamic binary packfiles, executing E2E CLI clones, and asserting checkout output.

---

## Architectural Decisions

1. **Decoupled CLI Runner**: Business runner logic was extracted out of the JVM `main()` entrypoint into `cloneRepository()`, allowing fully testable assertion coverage without thread-termination side effects.
2. **Dynamic SHA-1 Test Harness**: Unit test servers calculate mock object SHA-1 hashes dynamically using the production hashing utility rather than using hardcoded values, ensuring mock protocol handshakes align with parsed filesystem writes.
3. **Data Structure Integration**: Seamlessly integrated custom Doubly Linked Lists (`LruCache`) and Singly Linked Lists (`Queue`) into the parsing cache and BFS extraction layers to minimize third-party library dependencies.

---

## TODO List / Next Step
- All planned phases are fully completed. The project compiles successfully, handles MariaDB storage and JDBC metadata tracking, supports Git smart HTTP networking, parses loose and delta-packed files, and performs complete directory checkouts.