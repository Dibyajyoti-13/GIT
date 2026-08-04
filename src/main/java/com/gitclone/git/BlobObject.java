package com.gitclone.git;

/**
 * Represents a standard Git Blob object containing file data.
 */
public class BlobObject extends GitObjectBase {

    public BlobObject(byte[] content) {
        super(GitObjectType.BLOB, content);
    }
}
