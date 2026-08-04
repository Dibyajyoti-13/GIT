package com.gitclone.database.dao.impl;

import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.RepositoryDAO;
import com.gitclone.models.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of RepositoryDAO.
 */
public class RepositoryDAOImpl implements RepositoryDAO {

    @Override
    public Repository save(Repository repository) throws SQLException {
        if (repository.getId() == null) {
            String sql = "INSERT INTO Repositories (url, local_path, cloned_at) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, repository.getUrl());
                stmt.setString(2, repository.getLocalPath());
                stmt.setTimestamp(3, repository.getClonedAt() != null ? Timestamp.valueOf(repository.getClonedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        repository.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE Repositories SET url = ?, local_path = ?, cloned_at = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, repository.getUrl());
                stmt.setString(2, repository.getLocalPath());
                stmt.setTimestamp(3, Timestamp.valueOf(repository.getClonedAt()));
                stmt.setInt(4, repository.getId());
                stmt.executeUpdate();
            }
        }
        return repository;
    }

    @Override
    public Optional<Repository> findById(int id) throws SQLException {
        String sql = "SELECT id, url, local_path, cloned_at FROM Repositories WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRepository(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByUrl(String url) throws SQLException {
        String sql = "SELECT id, url, local_path, cloned_at FROM Repositories WHERE url = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, url);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRepository(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Repository> findAll() throws SQLException {
        String sql = "SELECT id, url, local_path, cloned_at FROM Repositories";
        List<Repository> list = new ArrayList<>();
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRepository(rs));
            }
        }
        return list;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Repositories WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Repository mapResultSetToRepository(ResultSet rs) throws SQLException {
        Timestamp clonedAt = rs.getTimestamp("cloned_at");
        return new Repository(
                rs.getInt("id"),
                rs.getString("url"),
                rs.getString("local_path"),
                clonedAt != null ? clonedAt.toLocalDateTime() : null
        );
    }
}
