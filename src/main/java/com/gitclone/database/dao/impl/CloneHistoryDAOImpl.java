package com.gitclone.database.dao.impl;

import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.CloneHistoryDAO;
import com.gitclone.models.CloneHistoryEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of CloneHistoryDAO.
 */
public class CloneHistoryDAOImpl implements CloneHistoryDAO {

    @Override
    public CloneHistoryEntry save(CloneHistoryEntry entry) throws SQLException {
        if (entry.getId() == null) {
            String sql = "INSERT INTO CloneHistory (repository_id, status, started_at, completed_at, error_message) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (entry.getRepositoryId() != null) {
                    stmt.setInt(1, entry.getRepositoryId());
                } else {
                    stmt.setNull(1, Types.INTEGER);
                }
                stmt.setString(2, entry.getStatus());
                stmt.setTimestamp(3, entry.getStartedAt() != null ? Timestamp.valueOf(entry.getStartedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.setTimestamp(4, entry.getCompletedAt() != null ? Timestamp.valueOf(entry.getCompletedAt()) : null);
                stmt.setString(5, entry.getErrorMessage());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entry.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE CloneHistory SET repository_id = ?, status = ?, started_at = ?, completed_at = ?, error_message = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (entry.getRepositoryId() != null) {
                    stmt.setInt(1, entry.getRepositoryId());
                } else {
                    stmt.setNull(1, Types.INTEGER);
                }
                stmt.setString(2, entry.getStatus());
                stmt.setTimestamp(3, Timestamp.valueOf(entry.getStartedAt()));
                stmt.setTimestamp(4, entry.getCompletedAt() != null ? Timestamp.valueOf(entry.getCompletedAt()) : null);
                stmt.setString(5, entry.getErrorMessage());
                stmt.setInt(6, entry.getId());
                stmt.executeUpdate();
            }
        }
        return entry;
    }

    @Override
    public Optional<CloneHistoryEntry> findById(int id) throws SQLException {
        String sql = "SELECT id, repository_id, status, started_at, completed_at, error_message FROM CloneHistory WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntry(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CloneHistoryEntry> findByRepositoryId(int repositoryId) throws SQLException {
        String sql = "SELECT id, repository_id, status, started_at, completed_at, error_message FROM CloneHistory WHERE repository_id = ?";
        List<CloneHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEntry(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<CloneHistoryEntry> findAll() throws SQLException {
        String sql = "SELECT id, repository_id, status, started_at, completed_at, error_message FROM CloneHistory";
        List<CloneHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEntry(rs));
            }
        }
        return list;
    }

    private CloneHistoryEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        int repoId = rs.getInt("repository_id");
        Integer repositoryId = rs.wasNull() ? null : repoId;

        return new CloneHistoryEntry(
                rs.getInt("id"),
                repositoryId,
                rs.getString("status"),
                startedAt != null ? startedAt.toLocalDateTime() : null,
                completedAt != null ? completedAt.toLocalDateTime() : null,
                rs.getString("error_message")
        );
    }
}
