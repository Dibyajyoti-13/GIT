# GitCloneJava

An educational systems engineering project implementing a simplified **Git Clone** completely from scratch in Java. This implementation is designed to demonstrate low-level Git network protocol handling, binary packfile parsing, and object reconstruction without utilizing any third-party Git libraries.

---

## Architecture and Execution Pipeline

1. **Reference Discovery**: Performs GET requests to `<repository_url>/info/refs?service=git-upload-pack` using Java's `HttpClient` to discover remote branch references and parse capacities using the custom packet-line (`pkt-line`) framing protocol.
2. **Packfile Negotiation**: Sends smart HTTP protocol POST upload-pack negotiation streams requesting target branch commits.
3. **Sideband Demultiplexing**: Decodes the returned multiplexed sideband stream, routing raw packfile payloads (channel 1) to the decompressor while routing progress information (channel 2) to logs.
4. **Binary Packfile Parsing**: Decodes variable-length integer object headers, inflates compressed bytes, and reconstructs delta-compressed objects (`OBJ_OFS_DELTA` and `OBJ_REF_DELTA`) via copy/insert instructions.
5. **Directory Reconstruction**: Traverses commit trees recursively and extracts decompressed files onto the physical disk.

---

## Core Data Structures

- **Doubly Linked List LRU Cache**: Implemented inside `LruCache` to achieve $O(1)$ read and write performance on recently accessed Git loose objects.
- **Singly Linked Queue**: Implemented inside `Queue` to run a Breadth-First Search (BFS) traversal of the Commit tree directory hierarchy during checkout, replacing recursive traversals.
- **Git Tree Node Sorting**: Matches Git's sorting specifications (virtual trailing slash for directory trees during path ordering) to ensure SHA-1 signature hashes align with official Git expectations.

---

## Tech Stack

- **Platform**: Java 21+
- **Build System**: Maven
- **Database**: MariaDB (handles repositories index, branch references, loose object tracking, and history logs)
- **Dependencies**: `java.net.http`, `java.util.zip`, `java.security.MessageDigest`, JDBC, JUnit 5, SLF4J, Logback.

---

## Setup and Operation

### 1. Database Configuration
Ensure a local database named `git_project` exists in MariaDB. If necessary, update database configuration in `src/main/resources/db.properties`:
```properties
db.url=jdbc:mariadb://localhost:3306/git_project
db.user=user
db.password=123456
```

### 2. Build and Test Execution
Verify all custom parser engines, caching policies, network mocks, and checkout validations:
```bash
mvn clean test
```

### 3. Execution via CLI
Execute a clone operation by invoking the main CLI entry point, supplying the repository URL and target destination directory:
```bash
mvn exec:java -Dexec.mainClass="com.gitclone.cli.GitCloneCLI" -Dexec.args="https://github.com/Dibyajyoti-13/GIT.git ./cloned-repo"
```
