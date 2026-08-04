package com.gitclone.git;

/**
 * Represents a single record inside a Git Tree.
 */
public class GitTreeEntry {
    private final String mode;
    private final String path;
    private final String sha1;

    public GitTreeEntry(String mode, String path, String sha1) {
        this.mode = mode;
        this.path = path;
        this.sha1 = sha1;
    }

    public String getMode() {
        return mode;
    }

    public String getPath() {
        return path;
    }

    public String getSha1() {
        return sha1;
    }
}
