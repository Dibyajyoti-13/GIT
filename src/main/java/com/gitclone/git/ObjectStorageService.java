package com.gitclone.git;

import com.gitclone.cache.LruCache;
import com.gitclone.database.dao.GitObjectDAO;
import com.gitclone.database.dao.impl.GitObjectDAOImpl;
import com.gitclone.models.GitObject;
import com.gitclone.utils.CompressionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

/**
 * Handles persistence and retrieval of Git Loose Objects to/from the file system
 * and indexing object metadata in the MariaDB database.
 * Utilizes LruCache for high-performance retrieval.
 */
public class ObjectStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageService.class);

    private final LruCache<String, GitObjectBase> cache;
    private final GitObjectDAO gitObjectDAO;

    public ObjectStorageService() {
        this.cache = new LruCache<>(500); // LRU Cache with capacity of 500 objects
        this.gitObjectDAO = new GitObjectDAOImpl();
    }

    /**
     * Writes a Git object to the loose object database.
     *
     * @param repoRoot Base directory of the repository (e.g., /path/to/project)
     * @param repoId Database ID of the repository
     * @param object The GitObjectBase to persist
     * @return The SHA-1 hash of the written object
     * @throws IOException if disk write fails
     * @throws SQLException if database write fails
     */
    public String writeObject(Path repoRoot, int repoId, GitObjectBase object) throws IOException, SQLException {
        String sha1 = object.getSha1();
        byte[] serialized = object.serialize();
        byte[] compressed = CompressionUtils.compress(serialized);

        // Path structure: repoRoot/.git/objects/ab/cdef...
        Path objectsDir = repoRoot.resolve(".git").resolve("objects");
        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);
        Path targetDir = objectsDir.resolve(dirName);
        Path targetFile = targetDir.resolve(fileName);

        Files.createDirectories(targetDir);
        Files.write(targetFile, compressed);

        logger.debug("Successfully wrote loose object {} to {}", sha1, targetFile);

        // Put in LRU Cache
        cache.put(sha1, object);

        // Register in database GitObjects table
        GitObject dbObj = new GitObject(
                null,
                repoId,
                sha1,
                object.getType().getValue(),
                object.getSize(),
                targetFile.toAbsolutePath().toString()
        );
        gitObjectDAO.save(dbObj);

        return sha1;
    }

    /**
     * Reads a loose Git object from the file system.
     *
     * @param repoRoot Base directory of the repository
     * @param sha1 40-character SHA-1 hex string
     * @return GitObjectBase representing the deserialized object
     * @throws IOException if disk read fails
     * @throws DataFormatException if decompression fails
     */
    public GitObjectBase readObject(Path repoRoot, String sha1) throws IOException, DataFormatException {
        // Check cache first
        GitObjectBase cached = cache.get(sha1);
        if (cached != null) {
            logger.debug("Cache hit for object {}", sha1);
            return cached;
        }

        // Locate loose object file
        Path objectsDir = repoRoot.resolve(".git").resolve("objects");
        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);
        Path objectPath = objectsDir.resolve(dirName).resolve(fileName);

        if (!Files.exists(objectPath)) {
            throw new IOException("Git object not found on disk: " + sha1);
        }

        byte[] compressedBytes = Files.readAllBytes(objectPath);
        byte[] decompressedBytes = CompressionUtils.decompress(compressedBytes);

        // Find the null byte that separates the header from the content
        int nullByteIndex = -1;
        for (int i = 0; i < decompressedBytes.length; i++) {
            if (decompressedBytes[i] == '\0') {
                nullByteIndex = i;
                break;
            }
        }

        if (nullByteIndex == -1) {
            throw new DataFormatException("Malformed Git object: header separator not found");
        }

        String header = new String(decompressedBytes, 0, nullByteIndex);
        int spaceIdx = header.indexOf(' ');
        if (spaceIdx == -1) {
            throw new DataFormatException("Malformed Git object header: space separator not found");
        }

        String typeStr = header.substring(0, spaceIdx);
        GitObjectType type = GitObjectType.fromString(typeStr);

        int contentLen = decompressedBytes.length - (nullByteIndex + 1);
        byte[] contentBytes = new byte[contentLen];
        System.arraycopy(decompressedBytes, nullByteIndex + 1, contentBytes, 0, contentLen);

        GitObjectBase object;
        switch (type) {
            case BLOB:
                object = new BlobObject(contentBytes);
                break;
            case TREE:
                object = new TreeObject(contentBytes);
                break;
            case COMMIT:
                object = new CommitObject(contentBytes);
                break;
            default:
                throw new DataFormatException("Unsupported Git object type: " + typeStr);
        }

        // Cache before returning
        cache.put(sha1, object);
        return object;
    }
}
