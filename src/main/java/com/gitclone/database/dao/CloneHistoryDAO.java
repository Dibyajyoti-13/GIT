package com.gitclone.database.dao;

import com.gitclone.models.CloneHistoryEntry;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for CloneHistory entity.
 */
public interface CloneHistoryDAO {
    CloneHistoryEntry save(CloneHistoryEntry entry) throws SQLException;
    Optional<CloneHistoryEntry> findById(int id) throws SQLException;
    List<CloneHistoryEntry> findByRepositoryId(int repositoryId) throws SQLException;
    List<CloneHistoryEntry> findAll() throws SQLException;
}
