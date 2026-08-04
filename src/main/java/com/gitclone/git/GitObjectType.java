package com.gitclone.git;

/**
 * Enumeration of standard Git Object types.
 */
public enum GitObjectType {
    BLOB("blob"),
    TREE("tree"),
    COMMIT("commit"),
    TAG("tag");

    private final String value;

    GitObjectType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Resolves the GitObjectType from its string name.
     */
    public static GitObjectType fromString(String text) {
        for (GitObjectType type : GitObjectType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown Git object type: " + text);
    }
}
