package com.gitclone.models;

/**
 * Represents a tracked Git branch.
 */
public class Branch {
    private Integer id;
    private Integer repositoryId;
    private String name;
    private String commitSha1;
    private boolean isLocal;

    public Branch() {}

    public Branch(Integer id, Integer repositoryId, String name, String commitSha1, boolean isLocal) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.name = name;
        this.commitSha1 = commitSha1;
        this.isLocal = isLocal;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommitSha1() {
        return commitSha1;
    }

    public void setCommitSha1(String commitSha1) {
        this.commitSha1 = commitSha1;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }
}
