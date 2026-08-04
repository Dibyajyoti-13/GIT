-- schema.sql: Initialize MariaDB tables for GitCloneJava project

CREATE TABLE IF NOT EXISTS Repositories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    local_path VARCHAR(4096) NOT NULL,
    cloned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS CloneHistory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT,
    status VARCHAR(50) NOT NULL, -- e.g., IN_PROGRESS, SUCCESS, FAILED
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    error_message TEXT,
    FOREIGN KEY (repository_id) REFERENCES Repositories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS GitObjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT NOT NULL,
    sha1 VARCHAR(40) NOT NULL,
    type VARCHAR(20) NOT NULL, -- e.g., commit, tree, blob, tag
    size BIGINT NOT NULL,
    path_location VARCHAR(4096), -- path to physical object file if stored separately
    FOREIGN KEY (repository_id) REFERENCES Repositories(id) ON DELETE CASCADE,
    UNIQUE KEY uq_repo_sha1 (repository_id, sha1)
);

CREATE TABLE IF NOT EXISTS Branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    commit_sha1 VARCHAR(40) NOT NULL,
    is_local BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (repository_id) REFERENCES Repositories(id) ON DELETE CASCADE,
    UNIQUE KEY uq_repo_branch (repository_id, name)
);
