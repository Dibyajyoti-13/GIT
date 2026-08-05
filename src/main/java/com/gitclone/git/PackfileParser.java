package com.gitclone.git;

import com.gitclone.utils.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Parses and processes binary Git Packfiles. Reconstructs base and delta compressed objects.
 */
public class PackfileParser {
    private static final Logger logger = LoggerFactory.getLogger(PackfileParser.class);

    private final ObjectStorageService storageService;
    private final Path repoRoot;
    private final int repoId;

    // Track resolved objects for offset deltas (key: start offset of base in packfile, value: resolved content)
    private final Map<Long, ResolvedObject> offsetMap = new HashMap<>();

    private static class ResolvedObject {
        final GitObjectType type;
        final byte[] content;
        final String sha1;

        ResolvedObject(GitObjectType type, byte[] content, String sha1) {
            this.type = type;
            this.content = content;
            this.sha1 = sha1;
        }
    }

    public PackfileParser(ObjectStorageService storageService, Path repoRoot, int repoId) {
        this.storageService = storageService;
        this.repoRoot = repoRoot;
        this.repoId = repoId;
    }

    /**
     * Parses the raw packfile byte array, resolving and storing all objects.
     *
     * @param packData Raw binary packfile data
     * @throws IOException if parsing or database index updates fail
     * @throws DataFormatException if zlib decompression fails
     * @throws SQLException if database write fails
     */
    public void parse(byte[] packData) throws IOException, DataFormatException, SQLException {
        if (packData.length < 12) {
            throw new IOException("Packfile too small to contain valid header.");
        }

        // 1. Parse Header
        String signature = new String(packData, 0, 4);
        if (!"PACK".equals(signature)) {
            throw new IOException("Invalid Packfile signature: " + signature);
        }

        long version = readInt32(packData, 4);
        if (version != 2) {
            throw new IOException("Unsupported Packfile version: " + version + ". Only version 2 is supported.");
        }

        long objectCount = readInt32(packData, 8);
        logger.info("Parsing packfile. Version: {}, Expected Objects: {}", version, objectCount);

        int idx = 12;

        // 2. Parse Objects
        for (int i = 0; i < objectCount; i++) {
            long objStartOffset = idx;

            // Decode Type & Size
            byte b = packData[idx++];
            int typeInt = (b & 0x70) >> 4;
            long size = b & 0x0F;
            int shift = 4;
            while ((b & 0x80) != 0) {
                b = packData[idx++];
                size |= (long) (b & 0x7F) << shift;
                shift += 7;
            }

            GitObjectType resolvedType = null;
            byte[] resolvedContent = null;
            String baseSha1 = null;

            if (typeInt >= 1 && typeInt <= 4) {
                // Base Object: Commit, Tree, Blob, or Tag
                resolvedType = getObjectTypeFromInt(typeInt);
                DecompressionResult decompResult = decompress(packData, idx, (int) size);
                resolvedContent = decompResult.data;
                idx += decompResult.bytesRead;

            } else if (typeInt == 6) {
                // OBJ_OFS_DELTA
                // Decode negative offset relative to current object start offset
                long offsetVal = 0;
                b = packData[idx++];
                offsetVal = b & 0x7F;
                while ((b & 0x80) != 0) {
                    offsetVal += 1;
                    b = packData[idx++];
                    offsetVal = (offsetVal << 7) + (b & 0x7F);
                }

                long baseObjOffset = objStartOffset - offsetVal;

                // Decompress delta instructions
                DecompressionResult decompResult = decompress(packData, idx, (int) size);
                byte[] deltaInstructions = decompResult.data;
                idx += decompResult.bytesRead;

                // Lookup base object content from memory map
                ResolvedObject baseObj = offsetMap.get(baseObjOffset);
                if (baseObj == null) {
                    throw new IOException("Base object not found in offset map for OFS_DELTA at offset " + baseObjOffset);
                }

                resolvedType = baseObj.type;
                resolvedContent = DeltaResolver.applyDelta(baseObj.content, deltaInstructions);

            } else if (typeInt == 7) {
                // OBJ_REF_DELTA
                // Extract 20-byte base object SHA-1
                byte[] baseShaBytes = new byte[20];
                System.arraycopy(packData, idx, baseShaBytes, 0, 20);
                baseSha1 = HashUtils.bytesToHex(baseShaBytes);
                idx += 20;

                // Decompress delta instructions
                DecompressionResult decompResult = decompress(packData, idx, (int) size);
                byte[] deltaInstructions = decompResult.data;
                idx += decompResult.bytesRead;

                // Resolve base object either from memory map or disk
                resolvedContent = resolveRefDeltaContent(baseSha1, deltaInstructions);
                
                // Fetch the type of the base object (we assume it exists)
                GitObjectBase baseObj = storageService.readObject(repoRoot, baseSha1);
                resolvedType = baseObj.getType();

            } else {
                throw new IOException("Unsupported Git Packfile object type: " + typeInt);
            }

            // Construct and persist the resolved Git Object
            GitObjectBase gitObjObj;
            switch (resolvedType) {
                case BLOB:
                    gitObjObj = new BlobObject(resolvedContent);
                    break;
                case TREE:
                    gitObjObj = new TreeObject(resolvedContent);
                    break;
                case COMMIT:
                    gitObjObj = new CommitObject(resolvedContent);
                    break;
                default:
                    throw new IOException("Invalid object type constructed: " + resolvedType);
            }

            String sha1 = storageService.writeObject(repoRoot, repoId, gitObjObj);

            // Record object mapping for subsequent delta references
            ResolvedObject resolvedObj = new ResolvedObject(resolvedType, resolvedContent, sha1);
            offsetMap.put(objStartOffset, resolvedObj);
        }

        logger.info("Successfully parsed and saved all {} objects from Packfile.", objectCount);
    }

    private byte[] resolveRefDeltaContent(String baseSha1, byte[] deltaInstructions) throws IOException, DataFormatException {
        // Try locating in memory offsetMap first
        for (ResolvedObject obj : offsetMap.values()) {
            if (obj.sha1.equals(baseSha1)) {
                return DeltaResolver.applyDelta(obj.content, deltaInstructions);
            }
        }
        // Fall back to reading from loose storage
        GitObjectBase baseObj = storageService.readObject(repoRoot, baseSha1);
        return DeltaResolver.applyDelta(baseObj.getContent(), deltaInstructions);
    }

    private GitObjectType getObjectTypeFromInt(int typeInt) throws IOException {
        return switch (typeInt) {
            case 1 -> GitObjectType.COMMIT;
            case 2 -> GitObjectType.TREE;
            case 3 -> GitObjectType.BLOB;
            case 4 -> GitObjectType.TAG;
            default -> throw new IOException("Unknown Git type integer: " + typeInt);
        };
    }

    private static class DecompressionResult {
        final byte[] data;
        final int bytesRead;

        DecompressionResult(byte[] data, int bytesRead) {
            this.data = data;
            this.bytesRead = bytesRead;
        }
    }

    /**
     * Decompresses deflated zlib bytes starting at a offset.
     */
    private DecompressionResult decompress(byte[] packData, int startOffset, int expectedSize) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(packData, startOffset, packData.length - startOffset);

        ByteArrayOutputStream out = new ByteArrayOutputStream(expectedSize);
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            if (count == 0 && inflater.needsInput()) {
                break;
            }
            out.write(buffer, 0, count);
        }

        int bytesRead = (int) inflater.getBytesRead();
        inflater.end();

        return new DecompressionResult(out.toByteArray(), bytesRead);
    }

    private static long readInt32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24) |
                ((long) (data[offset + 1] & 0xFF) << 16) |
                ((long) (data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }
}
