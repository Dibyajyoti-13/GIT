package com.gitclone.network;

import com.gitclone.database.DatabaseConnectionManager;
import com.gitclone.database.dao.RepositoryDAO;
import com.gitclone.database.dao.impl.RepositoryDAOImpl;
import com.gitclone.models.Repository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GitNetworkClientTest {

    private static HttpServer mockServer;
    private static int port;
    private static Repository testRepository;
    private static RepositoryDAO repositoryDAO;

    @BeforeAll
    public static void startServer() throws IOException, SQLException {
        // Start local HttpServer on random free port
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();

        mockServer.createContext("/test-repo/info/refs", exchange -> {
            // Verify query param
            String query = exchange.getRequestURI().getQuery();
            if (!"service=git-upload-pack".equals(query)) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            // Build mock Git smart advertisement response payload
            // Header line
            byte[] line1 = PktLine.format("# service=git-upload-pack\n");
            byte[] line2 = PktLine.formatFlush();
            // First reference advertisement line containing capabilities list after a null byte
            byte[] line3 = PktLine.format("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3 HEAD\0multi_ack thin-pack side-band\n");
            // Discovered branches
            byte[] line4 = PktLine.format("18ae56087643ba4b4ab93a543f8936f1f65e649f refs/heads/main\n");
            byte[] line5 = PktLine.format("5da533081060b5546839683f16674df581eff0cb refs/heads/feature-database\n");
            byte[] line6 = PktLine.formatFlush();

            int totalLength = line1.length + line2.length + line3.length + line4.length + line5.length + line6.length;
            byte[] response = new byte[totalLength];

            int offset = 0;
            System.arraycopy(line1, 0, response, offset, line1.length); offset += line1.length;
            System.arraycopy(line2, 0, response, offset, line2.length); offset += line2.length;
            System.arraycopy(line3, 0, response, offset, line3.length); offset += line3.length;
            System.arraycopy(line4, 0, response, offset, line4.length); offset += line4.length;
            System.arraycopy(line5, 0, response, offset, line5.length); offset += line5.length;
            System.arraycopy(line6, 0, response, offset, line6.length);

            exchange.getResponseHeaders().set("Content-Type", "application/x-git-upload-pack-advertisement");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        mockServer.start();

        // Setup Database Repository object
        repositoryDAO = new RepositoryDAOImpl();
        try {
            DatabaseConnectionManager.initializeSchema();
            Repository repo = new Repository(null, "http://localhost:" + port + "/test-repo.git", "/tmp/mock-net-repo", LocalDateTime.now());
            testRepository = repositoryDAO.save(repo);
        } catch (SQLException e) {
            // DB not available in this test environment, we'll run standalone client test
        }
    }

    @AfterAll
    public static void stopServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    public void testDiscoverReferences() throws Exception {
        GitNetworkClient client = new GitNetworkClient();
        String repoUrl = "http://localhost:" + port + "/test-repo.git";
        int repoId = testRepository != null ? testRepository.getId() : 0;

        Map<String, String> refs = client.discoverReferences(repoUrl, repoId);

        assertNotNull(refs);
        assertTrue(refs.containsKey("HEAD"));
        assertTrue(refs.containsKey("refs/heads/main"));
        assertTrue(refs.containsKey("refs/heads/feature-database"));

        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", refs.get("HEAD"));
        assertEquals("18ae56087643ba4b4ab93a543f8936f1f65e649f", refs.get("refs/heads/main"));
        assertEquals("5da533081060b5546839683f16674df581eff0cb", refs.get("refs/heads/feature-database"));
    }
}
