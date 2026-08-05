package com.gitclone.network;

import com.gitclone.database.dao.BranchDAO;
import com.gitclone.database.dao.impl.BranchDAOImpl;
import com.gitclone.models.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages network transport layer and smart HTTP connection setup.
 */
public class GitNetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(GitNetworkClient.class);

    private final HttpClient httpClient;
    private final BranchDAO branchDAO;

    public GitNetworkClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.branchDAO = new BranchDAOImpl();
    }

    /**
     * Executes GET /info/refs?service=git-upload-pack reference discovery
     * and persists discovered branches to the database.
     *
     * @param repoUrl Base URL of remote git repository (e.g., https://github.com/user/repo.git or https://github.com/user/repo)
     * @param repoId Database repository ID to link branches
     * @return Map of reference names to their corresponding SHA-1 hashes
     * @throws IOException if network request fails
     * @throws InterruptedException if request is interrupted
     * @throws SQLException if database write fails
     */
    public Map<String, String> discoverReferences(String repoUrl, int repoId) throws IOException, InterruptedException, SQLException {
        // Clean URL suffix if it ends with .git
        String baseUrl = repoUrl;
        if (baseUrl.endsWith(".git")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
        }

        URI uri = URI.create(baseUrl + "/info/refs?service=git-upload-pack");
        logger.info("Executing Reference Discovery: {}", uri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", "git/2.34.1")
                .header("Accept", "application/x-git-upload-pack-advertisement")
                .header("Accept-Encoding", "gzip, deflate, identity")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to connect to Git remote. HTTP Status: " + response.statusCode());
        }

        byte[] body = response.body();
        List<byte[]> packets = PktLine.parse(body);

        if (packets.isEmpty()) {
            throw new IOException("Empty response received from remote reference discovery.");
        }

        // Parse first packet: should match `# service=git-upload-pack` header
        String firstPacket = new String(packets.get(0), StandardCharsets.UTF_8).trim();
        if (!firstPacket.startsWith("# service=git-upload-pack")) {
            throw new IOException("Unexpected first packet header in advertisement: " + firstPacket);
        }

        Map<String, String> refs = new HashMap<>();

        // Loop starting from index 1 (skipping header, flush, etc.)
        for (int i = 1; i < packets.size(); i++) {
            byte[] payload = packets.get(i);
            if (payload.length == 0) {
                // Skip flush packets
                continue;
            }

            String line = new String(payload, StandardCharsets.UTF_8);
            // First reference advertisement line includes capability list after a null byte:
            // "SHA-1 refName\0capability1 capability2 ..."
            int nullByteIdx = line.indexOf('\0');
            if (nullByteIdx != -1) {
                line = line.substring(0, nullByteIdx);
            }

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int spaceIdx = line.indexOf(' ');
            if (spaceIdx == -1) {
                continue;
            }

            String sha1 = line.substring(0, spaceIdx);
            String refName = line.substring(spaceIdx + 1);

            refs.put(refName, sha1);
            logger.debug("Discovered reference: {} -> {}", refName, sha1);

            // If it's a branch, save to database
            if (refName.startsWith("refs/heads/")) {
                String branchName = refName.substring("refs/heads/".length());
                Branch branch = new Branch(null, repoId, branchName, sha1, false); // isLocal = false (remote branch)
                
                // Clear any existing branch with same name to allow clean update
                try {
                    branchDAO.delete(repoId, branchName);
                } catch (Exception e) {
                    // Ignore if doesn't exist
                }
                branchDAO.save(branch);
            }
        }

        logger.info("Reference discovery completed. Discovered {} references.", refs.size());
        return refs;
    }

    /**
     * Sends a POST request to git-upload-pack negotiating wants, receives the multiplexed sideband stream,
     * demultiplexes it, and writes the raw packfile to the target output stream.
     *
     * @param repoUrl Base remote repository URL
     * @param wantSha1 Commit SHA-1 we want to fetch
     * @param packOutput Target stream for the raw packfile
     * @throws IOException if network or protocol parsing fails
     * @throws InterruptedException if request is interrupted
     */
    public void fetchPackfile(String repoUrl, String wantSha1, java.io.OutputStream packOutput) throws IOException, InterruptedException {
        String baseUrl = repoUrl;
        if (baseUrl.endsWith(".git")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
        }

        URI uri = URI.create(baseUrl + "/git-upload-pack");
        logger.info("Negotiating Upload Pack: {}", uri);

        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        // Request the want commit with capability advertisement (side-band is crucial)
        requestBody.write(PktLine.format("want " + wantSha1 + " side-band thin-pack\n"));
        requestBody.write(PktLine.formatFlush());
        requestBody.write(PktLine.format("done\n"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", "git/2.34.1")
                .header("Content-Type", "application/x-git-upload-pack-request")
                .header("Accept", "application/x-git-upload-pack-result")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.toByteArray()))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("git-upload-pack negotiation failed. Status: " + response.statusCode());
        }

        byte[] body = response.body();
        logger.info("De-multiplexing smart protocol sideband response ({} bytes)...", body.length);
        SidebandDemuxer.demux(body, packOutput);
    }
}
