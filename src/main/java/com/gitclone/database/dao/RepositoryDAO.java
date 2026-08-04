package com.gitclone.database.dao;

import com.gitclone.models.Repository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Repository entity.
 */
public interface RepositoryDAO {
    Repository save(Repository repository) throws SQLException;
    Optional<Repository> findById(int id) throws SQLException;
    Optional<Repository> findByUrl(String url) throws SQLException;
    List<Repository> findAll() throws SQLException;
    void delete(int id) throws SQLException;
}
