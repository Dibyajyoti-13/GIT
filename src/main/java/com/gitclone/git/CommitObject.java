package com.gitclone.git;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a standard Git Commit object containing parents, tree SHA-1, author, committer, and message.
 */
public class CommitObject extends GitObjectBase {
    private String treeSha1;
    private final List<String> parents = new ArrayList<>();
    private String author;
    private String committer;
    private String message;

    public CommitObject(String treeSha1, List<String> parents, String author, String committer, String message) {
        super(GitObjectType.COMMIT, new byte[0]);
        this.treeSha1 = treeSha1;
        if (parents != null) {
            this.parents.addAll(parents);
        }
        this.author = author;
        this.committer = committer;
        this.message = message;
        this.content = serializeCommit();
    }

    public CommitObject(byte[] content) {
        super(GitObjectType.COMMIT, content);
        parseCommit(content);
    }

    public String getTreeSha1() {
        return treeSha1;
    }

    public List<String> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public String getAuthor() {
        return author;
    }

    public String getCommitter() {
        return committer;
    }

    public String getMessage() {
        return message;
    }

    private byte[] serializeCommit() {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeSha1).append("\n");
        for (String parent : parents) {
            sb.append("parent ").append(parent).append("\n");
        }
        if (author != null) {
            sb.append("author ").append(author).append("\n");
        }
        if (committer != null) {
            sb.append("committer ").append(committer).append("\n");
        }
        sb.append("\n");
        if (message != null) {
            sb.append(message);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void parseCommit(byte[] data) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder msgBuilder = new StringBuilder();
            boolean readingHeaders = true;

            while ((line = reader.readLine()) != null) {
                if (readingHeaders) {
                    if (line.isEmpty()) {
                        readingHeaders = false;
                        continue;
                    }
                    if (line.startsWith("tree ")) {
                        this.treeSha1 = line.substring(5).trim();
                    } else if (line.startsWith("parent ")) {
                        this.parents.add(line.substring(7).trim());
                    } else if (line.startsWith("author ")) {
                        this.author = line.substring(7).trim();
                    } else if (line.startsWith("committer ")) {
                        this.committer = line.substring(10).trim();
                    }
                } else {
                    msgBuilder.append(line).append("\n");
                }
            }
            this.message = msgBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse commit object", e);
        }
    }
}
