package com.gitclone.models;

/**
 * Represents metadata of a physical Git Object stored in the repository.
 */
public class GitObject {
    private Integer id;
    private Integer repositoryId;
    private String sha1;
    private String type;
    private long size;
    private String pathLocation;

    public GitObject() {}

    public GitObject(Integer id, Integer repositoryId, String sha1, String type, long size, String pathLocation) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.sha1 = sha1;
        this.type = type;
        this.size = size;
        this.pathLocation = pathLocation;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Integer repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPathLocation() {
        return pathLocation;
    }

    public void setPathLocation(String pathLocation) {
        this.pathLocation = pathLocation;
    }
}
