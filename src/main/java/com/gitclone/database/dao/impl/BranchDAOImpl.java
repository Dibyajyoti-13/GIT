package com.gitclone.database.dao.impl;

import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.BranchDAO;
import com.gitclone.models.Branch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of BranchDAO.
 */
public class BranchDAOImpl implements BranchDAO {

    @Override
    public Branch save(Branch branch) throws SQLException {
        if (branch.getId() == null) {
            String sql = "INSERT INTO Branches (repository_id, name, commit_sha1, is_local) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, branch.getRepositoryId());
                stmt.setString(2, branch.getName());
                stmt.setString(3, branch.getCommitSha1());
                stmt.setBoolean(4, branch.isLocal());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        branch.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE Branches SET repository_id = ?, name = ?, commit_sha1 = ?, is_local = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, branch.getRepositoryId());
                stmt.setString(2, branch.getName());
                stmt.setString(3, branch.getCommitSha1());
                stmt.setBoolean(4, branch.isLocal());
                stmt.setInt(5, branch.getId());
                stmt.executeUpdate();
            }
        }
        return branch;
    }

    @Override
    public Optional<Branch> findByName(int repositoryId, String name) throws SQLException {
        String sql = "SELECT id, repository_id, name, commit_sha1, is_local FROM Branches WHERE repository_id = ? AND name = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBranch(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Branch> findByRepositoryId(int repositoryId) throws SQLException {
        String sql = "SELECT id, repository_id, name, commit_sha1, is_local FROM Branches WHERE repository_id = ?";
        List<Branch> list = new ArrayList<>();
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBranch(rs));
                }
            }
        }
        return list;
    }

    @Override
    public void delete(int repositoryId, String name) throws SQLException {
        String sql = "DELETE FROM Branches WHERE repository_id = ? AND name = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, repositoryId);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private Branch mapResultSetToBranch(ResultSet rs) throws SQLException {
        return new Branch(
                rs.getInt("id"),
                rs.getInt("repository_id"),
                rs.getString("name"),
                rs.getString("commit_sha1"),
                rs.getBoolean("is_local")
        );
    }
}
