package com.gitclone.git;

import com.gitclone.utils.HashUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a standard Git Tree object containing directory entries.
 * Implements a Tree Data Structure storing ordered child entries.
 */
public class TreeObject extends GitObjectBase {
    private final List<GitTreeEntry> entries;

    public TreeObject() {
        super(GitObjectType.TREE, new byte[0]);
        this.entries = new ArrayList<>();
    }

    public TreeObject(List<GitTreeEntry> entries) {
        super(GitObjectType.TREE, new byte[0]);
        this.entries = new ArrayList<>(entries);
        sortEntries();
        this.content = serializeEntries();
    }

    public TreeObject(byte[] content) {
        super(GitObjectType.TREE, content);
        this.entries = parseEntries(content);
    }

    public List<GitTreeEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Sorts the entries based on Git's tree sorting rules:
     * entries are sorted by path name, but directories/trees behave as if they had a trailing slash.
     */
    private void sortEntries() {
        entries.sort((e1, e2) -> {
            String path1 = e1.getPath();
            String path2 = e2.getPath();
            // Git sorts directory trees by path as if it ends with '/'
            if (e1.getMode().equals("40000") || e1.getMode().equals("040000")) {
                path1 += "/";
            }
            if (e2.getMode().equals("40000") || e2.getMode().equals("040000")) {
                path2 += "/";
            }
            return path1.compareTo(path2);
        });
    }

    /**
     * Serializes only the entries into the raw content payload.
     */
    private byte[] serializeEntries() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (GitTreeEntry entry : entries) {
                // Format: [mode] [path]\0[20-byte SHA-1]
                out.write(entry.getMode().getBytes());
                out.write(' ');
                out.write(entry.getPath().getBytes());
                out.write('\0');
                out.write(HashUtils.hexToBytes(entry.getSha1()));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize tree entries", e);
        }
    }

    /**
     * Parses the raw content bytes into a list of GitTreeEntry objects.
     */
    private static List<GitTreeEntry> parseEntries(byte[] data) {
        List<GitTreeEntry> list = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            // Find space separating mode and path
            int spaceIdx = -1;
            for (int j = i; j < data.length; j++) {
                if (data[j] == ' ') {
                    spaceIdx = j;
                    break;
                }
            }
            if (spaceIdx == -1) break;

            String mode = new String(data, i, spaceIdx - i);

            // Find null byte separating path and SHA-1
            int nullIdx = -1;
            for (int j = spaceIdx + 1; j < data.length; j++) {
                if (data[j] == '\0') {
                    nullIdx = j;
                    break;
                }
            }
            if (nullIdx == -1) break;

            String path = new String(data, spaceIdx + 1, nullIdx - (spaceIdx + 1));

            // Extract next 20 bytes for the binary SHA-1
            if (nullIdx + 1 + 20 > data.length) {
                break;
            }
            byte[] shaBytes = new byte[20];
            System.arraycopy(data, nullIdx + 1, shaBytes, 0, 20);
            String sha1 = HashUtils.bytesToHex(shaBytes);

            list.add(new GitTreeEntry(mode, path, sha1));
            i = nullIdx + 1 + 20;
        }
        return list;
    }
}
