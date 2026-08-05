package com.gitclone.cli;

import com.gitclone.checkout.CheckoutService;
import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.RepositoryDAO;
import com.gitclone.database.dao.impl.RepositoryDAOImpl;
import com.gitclone.git.ObjectStorageService;
import com.gitclone.git.PackfileParser;
import com.gitclone.models.Repository;
import com.gitclone.network.GitNetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * CLI Command-line entry point for executing Git Clone.
 */
public class GitCloneCLI {
    private static final Logger logger = LoggerFactory.getLogger(GitCloneCLI.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp gitclone-1.0-SNAPSHOT.jar com.gitclone.cli.GitCloneCLI <repository_url> <destination_directory>");
            System.exit(1);
        }

        String repoUrl = args[0];
        String destDirStr = args[1];

        try {
            cloneRepository(repoUrl, destDirStr);
        } catch (Exception e) {
            logger.error("End-to-End Git Clone failed: ", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Executes the end-to-end repository clone operation.
     */
    public static void cloneRepository(String repoUrl, String destDirStr) throws Exception {
        Path destinationPath = Paths.get(destDirStr).toAbsolutePath().normalize();

        logger.info("Initializing Git Clone from: {} to: {}", repoUrl, destinationPath);

        // 1. Initialize DB Schema
        DatabaseConnectionManager.initializeSchema();

        // 2. Register Repository record in DB
        RepositoryDAO repositoryDAO = new RepositoryDAOImpl();
        Repository repository = new Repository(null, repoUrl, destinationPath.toString(), LocalDateTime.now());
        repository = repositoryDAO.save(repository);
        int repoId = repository.getId();
        logger.info("Registered repository in database with ID: {}", repoId);

        // 3. Remote Reference Discovery
        GitNetworkClient networkClient = new GitNetworkClient();
        Map<String, String> refs = networkClient.discoverReferences(repoUrl, repoId);

        // 4. Resolve default branch HEAD (main/master/HEAD)
        String targetBranch = "refs/heads/main";
        String wantSha1 = refs.get(targetBranch);
        if (wantSha1 == null) {
            targetBranch = "refs/heads/master";
            wantSha1 = refs.get(targetBranch);
        }
        if (wantSha1 == null) {
            targetBranch = "HEAD";
            wantSha1 = refs.get(targetBranch);
        }

        if (wantSha1 == null) {
            throw new IllegalStateException("Could not find default branch (main, master, or HEAD) on remote.");
        }

        logger.info("Targeting branch: {} at commit: {}", targetBranch, wantSha1);

        // 5. Download Packfile to temp file
        Path tempPackFile = Files.createTempFile("gitclone-", ".pack");
        try {
            logger.info("Downloading objects packfile to temporary file: {}", tempPackFile);
            try (OutputStream packOut = Files.newOutputStream(tempPackFile)) {
                networkClient.fetchPackfile(repoUrl, wantSha1, packOut);
            }

            byte[] packBytes = Files.readAllBytes(tempPackFile);
            logger.info("Packfile download completed ({} bytes). Processing objects...", packBytes.length);

            // 6. Parse Packfile & Store Loose Objects
            ObjectStorageService storageService = new ObjectStorageService();
            PackfileParser parser = new PackfileParser(storageService, destinationPath, repoId);
            parser.parse(packBytes);

            // 7. Reconstruct Directory Tree (Checkout)
            CheckoutService checkoutService = new CheckoutService(storageService);
            checkoutService.checkout(destinationPath, repoId, wantSha1, destinationPath);

            logger.info("Clone execution completed successfully! Destination: {}", destinationPath);

        } finally {
            // Cleanup temp packfile
            try {
                Files.deleteIfExists(tempPackFile);
            } catch (Exception e) {
                logger.warn("Could not delete temporary packfile: {}", e.getMessage());
            }
        }
    }
}
