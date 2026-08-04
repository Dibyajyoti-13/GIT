package com.gitclone.models;

import java.time.LocalDateTime;

/**
 * Represents a tracked Git repository in the database.
 */
public class Repository {
    private Integer id;
    private String url;
    private String localPath;
    private LocalDateTime clonedAt;

    public Repository() {}

    public Repository(Integer id, String url, String localPath, LocalDateTime clonedAt) {
        this.id = id;
        this.url = url;
        this.localPath = localPath;
        this.clonedAt = clonedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public LocalDateTime getClonedAt() {
        return clonedAt;
    }

    public void setClonedAt(LocalDateTime clonedAt) {
        this.clonedAt = clonedAt;
    }
}
