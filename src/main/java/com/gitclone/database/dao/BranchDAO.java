package com.gitclone.database.dao;

import com.gitclone.models.Branch;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Branch entity.
 */
public interface BranchDAO {
    Branch save(Branch branch) throws SQLException;
    Optional<Branch> findByName(int repositoryId, String name) throws SQLException;
    List<Branch> findByRepositoryId(int repositoryId) throws SQLException;
    void delete(int repositoryId, String name) throws SQLException;
}
