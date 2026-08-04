package com.gitclone.git;

import com.gitclone.utils.HashUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base class representing a generic Git Object.
 */
public abstract class GitObjectBase {
    protected GitObjectType type;
    protected byte[] content;

    protected GitObjectBase(GitObjectType type, byte[] content) {
        this.type = type;
        this.content = content != null ? content : new byte[0];
    }

    public GitObjectType getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSize() {
        return content.length;
    }

    /**
     * Serializes the object into the standard Git loose format:
     * "[type] [size]\0[content]"
     *
     * @return serialized byte array
     */
    public byte[] serialize() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String header = type.getValue() + " " + getSize() + "\0";
            out.write(header.getBytes());
            out.write(content);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    /**
     * Calculates the SHA-1 checksum of the serialized loose Git object.
     */
    public String getSha1() {
        return HashUtils.sha1Hex(serialize());
    }
}
