package com.gitclone.checkout;

import com.gitclone.database.dao.CloneHistoryDAO;
import com.gitclone.database.dao.impl.CloneHistoryDAOImpl;
import com.gitclone.git.*;
import com.gitclone.models.CloneHistoryEntry;
import com.gitclone.utils.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Executes checkout and directory tree reconstruction from Git loose objects.
 */
public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final ObjectStorageService storageService;
    private final CloneHistoryDAO cloneHistoryDAO;

    public CheckoutService(ObjectStorageService storageService) {
        this.storageService = storageService;
        this.cloneHistoryDAO = new CloneHistoryDAOImpl();
    }

    private static class CheckoutNode {
        final GitTreeEntry entry;
        final String relativeParentPath;

        CheckoutNode(GitTreeEntry entry, String relativeParentPath) {
            this.entry = entry;
            this.relativeParentPath = relativeParentPath;
        }
    }

    /**
     * Traverses the commit tree in BFS fashion using a custom Queue and extracts files to destination.
     * Logs progress and outcome in the database.
     *
     * @param repoRoot Base directory containing Git loose objects under `.git/objects`
     * @param repoId Database repository ID to track history logs
     * @param commitSha1 Commit SHA-1 to checkout
     * @param destPath Target directory to write reconstructed files
     * @throws Exception if checkout fails
     */
    public void checkout(Path repoRoot, int repoId, String commitSha1, Path destPath) throws Exception {
        logger.info("Starting checkout for commit: {} to target: {}", commitSha1, destPath);

        // 1. Log clone history attempt in Database
        CloneHistoryEntry historyEntry = new CloneHistoryEntry(
                null,
                repoId > 0 ? repoId : null,
                "IN_PROGRESS",
                LocalDateTime.now(),
                null,
                null
        );

        if (repoId > 0) {
            try {
                historyEntry = cloneHistoryDAO.save(historyEntry);
            } catch (SQLException e) {
                logger.warn("Could not log clone progress to database: {}", e.getMessage());
            }
        }

        try {
            // Retrieve Commit Object
            GitObjectBase obj = storageService.readObject(repoRoot, commitSha1);
            if (obj.getType() != GitObjectType.COMMIT) {
                throw new IllegalArgumentException("Target SHA-1 does not point to a valid Commit object: " + commitSha1);
            }
            CommitObject commit = (CommitObject) obj;
            String rootTreeSha1 = commit.getTreeSha1();

            // Retrieve root Tree Object
            GitObjectBase treeObj = storageService.readObject(repoRoot, rootTreeSha1);
            if (treeObj.getType() != GitObjectType.TREE) {
                throw new IllegalArgumentException("Commit root tree SHA-1 does not point to a valid Tree object: " + rootTreeSha1);
            }
            TreeObject rootTree = (TreeObject) treeObj;

            // Initialize BFS Queue
            Queue<CheckoutNode> queue = new Queue<>();

            // Enqueue all files/directories from root tree
            for (GitTreeEntry entry : rootTree.getEntries()) {
                queue.enqueue(new CheckoutNode(entry, ""));
            }

            Files.createDirectories(destPath);

            // Execute BFS directory reconstruction
            while (!queue.isEmpty()) {
                CheckoutNode node = queue.dequeue();
                Path fileDestPath = destPath.resolve(node.relativeParentPath).resolve(node.entry.getPath());

                String mode = node.entry.getMode();

                if (mode.equals("40000") || mode.equals("040000")) {
                    // Entry is a Directory (Tree Object)
                    Files.createDirectories(fileDestPath);
                    logger.debug("Reconstructed directory: {}", fileDestPath);

                    GitObjectBase subTreeObj = storageService.readObject(repoRoot, node.entry.getSha1());
                    if (subTreeObj.getType() == GitObjectType.TREE) {
                        TreeObject subTree = (TreeObject) subTreeObj;
                        String nextParentPath = node.relativeParentPath + node.entry.getPath() + "/";
                        for (GitTreeEntry subEntry : subTree.getEntries()) {
                            queue.enqueue(new CheckoutNode(subEntry, nextParentPath));
                        }
                    }
                } else {
                    // Entry is a File (Blob Object)
                    Files.createDirectories(fileDestPath.getParent());

                    GitObjectBase blobObj = storageService.readObject(repoRoot, node.entry.getSha1());
                    if (blobObj.getType() == GitObjectType.BLOB) {
                        Files.write(fileDestPath, blobObj.getContent());
                        logger.debug("Reconstructed file: {}", fileDestPath);
                    }
                }
            }

            // 2. Log SUCCESS in history table
            if (repoId > 0 && historyEntry.getId() != null) {
                historyEntry.setStatus("SUCCESS");
                historyEntry.setCompletedAt(LocalDateTime.now());
                cloneHistoryDAO.save(historyEntry);
            }

            logger.info("Checkout completed successfully.");

        } catch (Exception e) {
            logger.error("Checkout failed for commit: {}", commitSha1, e);

            // 3. Log FAILED in history table
            if (repoId > 0 && historyEntry.getId() != null) {
                try {
                    historyEntry.setStatus("FAILED");
                    historyEntry.setCompletedAt(LocalDateTime.now());
                    historyEntry.setErrorMessage(e.getMessage());
                    cloneHistoryDAO.save(historyEntry);
                } catch (SQLException dbEx) {
                    logger.warn("Could not log failed clone progress to database: {}", dbEx.getMessage());
                }
            }
            throw e;
        }
    }
}
