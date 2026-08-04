package com.gitclone.models;

import java.time.LocalDateTime;

/**
 * Represents a clone execution record in the history log.
 */
public class CloneHistoryEntry {
    private Integer id;
    private Integer repositoryId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public CloneHistoryEntry() {}

    public CloneHistoryEntry(Integer id, Integer repositoryId, String status, LocalDateTime startedAt, LocalDateTime completedAt, String errorMessage) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
