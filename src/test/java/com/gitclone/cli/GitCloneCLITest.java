package com.gitclone.cli;

import com.gitclone.network.PktLine;
import com.gitclone.utils.CompressionUtils;
import com.gitclone.utils.HashUtils;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GitCloneCLITest {

    private static HttpServer mockServer;
    private static int port;
    
    // Dynamically computed SHA-1 hashes of mock objects to ensure protocol consistency
    private static String mockCommitSha1;
    private static byte[] mockPackfile;

    @BeforeAll
    public static void startServer() throws IOException {
        // 1. Build Mock Git Objects in memory and compute their actual SHA-1 hashes
        
        // 1a. Raw Blob Object: content "End-to-End Clone Success!"
        byte[] blobBytes = "End-to-End Clone Success!".getBytes();
        byte[] compBlob = CompressionUtils.compress(blobBytes);
        byte[] blobHeader = { (byte) 0xB9, 0x01 }; // Type: BLOB (3), Size: 25

        // Compute Blob SHA-1
        String blobHeaderStr = "blob " + blobBytes.length + "\0";
        byte[] blobHeaderBytes = blobHeaderStr.getBytes();
        byte[] blobLooseContent = new byte[blobHeaderBytes.length + blobBytes.length];
        System.arraycopy(blobHeaderBytes, 0, blobLooseContent, 0, blobHeaderBytes.length);
        System.arraycopy(blobBytes, 0, blobLooseContent, blobHeaderBytes.length, blobBytes.length);
        byte[] blobShaBytes = HashUtils.sha1(blobLooseContent);

        // 1b. Raw Tree Object: contains entry "100644 README.md\0[binary SHA-1]"
        ByteArrayOutputStream treeOut = new ByteArrayOutputStream();
        treeOut.write("100644 README.md\0".getBytes());
        treeOut.write(blobShaBytes);
        byte[] treeBytes = treeOut.toByteArray();
        byte[] compTree = CompressionUtils.compress(treeBytes);
        byte[] treeHeader = { (byte) 0xA5, 0x02 }; // Type: TREE (2), Size: 37

        // Compute Tree SHA-1
        byte[] treeLooseHeader = ("tree " + treeBytes.length + "\0").getBytes();
        byte[] treeLooseContent = new byte[treeLooseHeader.length + treeBytes.length];
        System.arraycopy(treeLooseHeader, 0, treeLooseContent, 0, treeLooseHeader.length);
        System.arraycopy(treeBytes, 0, treeLooseContent, treeLooseHeader.length, treeBytes.length);
        String treeSha1Hex = HashUtils.sha1Hex(treeLooseContent);

        // 1c. Raw Commit Object: refers to root tree SHA-1.
        String commitContent = "tree " + treeSha1Hex + "\n" +
                "author Author <a@m.com> 1234567890 +0000\n" +
                "committer Committer <c@m.com> 1234567890 +0000\n\n" +
                "Mock E2E Commit\n";
        byte[] commitBytes = commitContent.getBytes();
        byte[] compCommit = CompressionUtils.compress(commitBytes);
        byte[] commitHeader = { (byte) 0x91, 0x09 }; // Type: COMMIT (1), Size: 145

        // Compute Commit SHA-1
        byte[] commitLooseHeader = ("commit " + commitBytes.length + "\0").getBytes();
        byte[] commitLooseContent = new byte[commitLooseHeader.length + commitBytes.length];
        System.arraycopy(commitLooseHeader, 0, commitLooseContent, 0, commitLooseHeader.length);
        System.arraycopy(commitBytes, 0, commitLooseContent, commitLooseHeader.length, commitBytes.length);
        mockCommitSha1 = HashUtils.sha1Hex(commitLooseContent);

        // 1d. Assemble raw PACK file
        byte[] packHeader = {
                'P', 'A', 'C', 'K', // Signature
                0, 0, 0, 2,         // Version 2
                0, 0, 0, 3          // Count: 3 objects
        };

        ByteArrayOutputStream packStream = new ByteArrayOutputStream();
        packStream.write(packHeader);
        // Object 1: Commit
        packStream.write(commitHeader);
        packStream.write(compCommit);
        // Object 2: Tree
        packStream.write(treeHeader);
        packStream.write(compTree);
        // Object 3: Blob
        packStream.write(blobHeader);
        packStream.write(compBlob);

        mockPackfile = packStream.toByteArray();

        // 2. Start local HttpServer
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();

        // GET reference discovery endpoint
        mockServer.createContext("/mock-repo/info/refs", exchange -> {
            byte[] line1 = PktLine.format("# service=git-upload-pack\n");
            byte[] line2 = PktLine.formatFlush();
            // Advertise the dynamically calculated commit SHA-1 for main
            byte[] line3 = PktLine.format(mockCommitSha1 + " refs/heads/main\0multi_ack thin-pack side-band\n");
            byte[] line4 = PktLine.formatFlush();

            int totalLength = line1.length + line2.length + line3.length + line4.length;
            byte[] response = new byte[totalLength];

            int offset = 0;
            System.arraycopy(line1, 0, response, offset, line1.length); offset += line1.length;
            System.arraycopy(line2, 0, response, offset, line2.length); offset += line2.length;
            System.arraycopy(line3, 0, response, offset, line3.length); offset += line3.length;
            System.arraycopy(line4, 0, response, offset, line4.length);

            exchange.getResponseHeaders().set("Content-Type", "application/x-git-upload-pack-advertisement");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // POST upload-pack response endpoint
        mockServer.createContext("/mock-repo/git-upload-pack", exchange -> {
            try {
                // Consume request body to prevent connection reset
                try (java.io.InputStream is = exchange.getRequestBody()) {
                    byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {}
                }

                // Wrap packfile in Sideband Channel 1 pkt-lines
                byte[] sidebandPayload = new byte[mockPackfile.length + 1];
                sidebandPayload[0] = 1; // Channel 1
                System.arraycopy(mockPackfile, 0, sidebandPayload, 1, mockPackfile.length);

                byte[] pktLinePayload = PktLine.format(sidebandPayload);
                byte[] pktFlush = PktLine.formatFlush();

                ByteArrayOutputStream finalResponse = new ByteArrayOutputStream();
                finalResponse.write(pktLinePayload);
                finalResponse.write(pktFlush);

                byte[] responseBytes = finalResponse.toByteArray();

                exchange.getResponseHeaders().set("Content-Type", "application/x-git-upload-pack-result");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        mockServer.start();
    }

    @AfterAll
    public static void stopServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    public void testEndToEndCLIClone(@TempDir Path tempDestDir) throws Exception {
        String repoUrl = "http://localhost:" + port + "/mock-repo.git";

        // Run E2E cloneRepository directly
        GitCloneCLI.cloneRepository(repoUrl, tempDestDir.toString());

        // Verify checkout file exists with reconstructed content
        Path readmeFile = tempDestDir.resolve("README.md");
        assertTrue(Files.exists(readmeFile), "README.md should be extracted to target directory.");
        try {
            String content = Files.readString(readmeFile);
            assertEquals("End-to-End Clone Success!", content);
        } catch (IOException e) {
            fail("Failed reading target README.md file: " + e.getMessage());
        }
    }
}
