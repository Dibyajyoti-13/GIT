package com.gitclone.database.dao;

import com.gitclone.models.GitObject;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for GitObject entity.
 */
public interface GitObjectDAO {
    GitObject save(GitObject gitObject) throws SQLException;
    Optional<GitObject> findBySha1(int repositoryId, String sha1) throws SQLException;
    List<GitObject> findByRepositoryId(int repositoryId) throws SQLException;
    void delete(int repositoryId, String sha1) throws SQLException;
}
