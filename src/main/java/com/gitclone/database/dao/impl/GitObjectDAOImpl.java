package com.gitclone.database.dao.impl;

import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.GitObjectDAO;
import com.gitclone.models.GitObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GitObjectDAO.
 */
public class GitObjectDAOImpl implements GitObjectDAO {

    @Override
    public GitObject save(GitObject gitObject) throws SQLException {
        if (gitObject.getId() == null) {
            String sql = "INSERT INTO GitObjects (repository_id, sha1, type, size, path_location) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, gitObject.getRepositoryId());
                stmt.setString(2, gitObject.getSha1());
                stmt.setString(3, gitObject.getType());
                stmt.setLong(4, gitObject.getSize());
                stmt.setString(5, gitObject.getPathLocation());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        gitObject.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE GitObjects SET repository_id = ?, sha1 = ?, type = ?, size = ?, path_location = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, gitObject.getRepositoryId());
                stmt.setString(2, gitObject.getSha1());
                stmt.setString(3, gitObject.getType());
                stmt.setLong(4, gitObject.getSize());
                stmt.setString(5, gitObject.getPathLocation());
                stmt.setInt(6, gitObject.getId());
                stmt.executeUpdate();
            }
        }
        return gitObject;
    }

    @Override
    public Optional<GitObject> findBySha1(int repositoryId, String sha1) throws SQLException {
        String sql = "SELECT id, repository_id, sha1, type, size, path_location FROM GitObjects WHERE repository_id = ? AND sha1 = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            stmt.setString(2, sha1);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToGitObject(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GitObject> findByRepositoryId(int repositoryId) throws SQLException {
        String sql = "SELECT id, repository_id, sha1, type, size, path_location FROM GitObjects WHERE repository_id = ?";
        List<GitObject> list = new ArrayList<>();
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToGitObject(rs));
                }
            }
        }
        return list;
    }

    @Override
    public void delete(int repositoryId, String sha1) throws SQLException {
        String sql = "DELETE FROM GitObjects WHERE repository_id = ? AND sha1 = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            stmt.setString(2, sha1);
            stmt.executeUpdate();
        }
    }

    private GitObject mapResultSetToGitObject(ResultSet rs) throws SQLException {
        return new GitObject(
                rs.getInt("id"),
                rs.getInt("repository_id"),
                rs.getString("sha1"),
                rs.getString("type"),
                rs.getLong("size"),
                rs.getString("path_location")
        );
    }
}
