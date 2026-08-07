package com.pablomusaber.watson.knowledge_agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
public class ObsidianCliWrapper {

    private static final String OBSIDIAN = "/usr/bin/obsidian";
    private static final String VAULT = "SecondBrain";
    private static final int MAX_LISTED_FILES = 50;

    private volatile Path vaultRoot;

    @Tool(name = "obsidian_search", description = "Search the Obsidian vault for notes matching a text query")
    public String search(
            @ToolParam(description = "Search query text") String query,
            @ToolParam(description = "Maximum number of results to return") int limit) {
        return run("search", "query=" + query, "limit=" + limit, "format=json");
    }

    @Tool(name = "obsidian_read", description = "Read the full contents of a note in the Obsidian vault by file path")
    public String readNote(
            @ToolParam(description = "File path relative to vault root, e.g. Inbox/MyNote.md") String path) {
        return run("read", "path=" + path);
    }

    @Tool(name = "obsidian_create", description = "Create a new note in the Obsidian vault")
    public String createNote(
            @ToolParam(description = "Note name without .md extension") String name,
            @ToolParam(description = "Initial markdown content for the note") String content,
            @ToolParam(description = "Optional template name to apply") String template) {
        List<String> args = new ArrayList<>(List.of("create", "name=" + name, "content=" + content));
        if (template != null && !template.isBlank()) {
            args.add("template=" + template);
        }
        return run(args.toArray(new String[0]));
    }

    @Tool(name = "obsidian_append", description = "Append content to an existing note in the Obsidian vault")
    public String appendNote(
            @ToolParam(description = "File path relative to vault root, e.g. Inbox/MyNote.md") String path,
            @ToolParam(description = "Content to append") String content) {
        return run("append", "path=" + path, "content=" + content);
    }

    @Tool(name = "obsidian_list_files", description = "List files in a folder of the Obsidian vault")
    public String listNotes(
            @ToolParam(description = "Folder path relative to vault root (empty for every note in the vault)") String folder) {
        if (folder == null || folder.isBlank()) {
            return capLines(run("files", "ext=md"));
        }
        return capLines(run("files", "folder=" + folder));
    }

    @Tool(name = "obsidian_tags", description = "List all tags in the Obsidian vault with their occurrence counts")
    public String getTags() {
        return run("tags", "counts", "sort=count");
    }

    @Tool(name = "obsidian_tasks", description = "List tasks in the Obsidian vault, optionally filtered by file")
    public String getTasks(
            @ToolParam(description = "Optional file path to filter tasks by") String path) {
        if (path == null || path.isBlank()) {
            return run("tasks", "total");
        }
        return run("tasks", "path=" + path);
    }

    @Tool(name = "obsidian_file_info", description = "Get metadata about a file in the Obsidian vault")
    public String getFileInfo(
            @ToolParam(description = "File path relative to vault root") String path) {
        return run("file", "path=" + path);
    }

    @Tool(name = "obsidian_recent_notes", description = "List the N most recently modified notes in the Obsidian vault, sorted newest first")
    public String recentNotes(@ToolParam(description = "Maximum number of notes to return") int limit) {
        long start = System.nanoTime();
        Path root = resolveVaultRoot();
        log.info("recentNotes: resolveVaultRoot took {} ms", (System.nanoTime() - start) / 1_000_000);
        if (root == null) {
            return "Error: could not resolve vault root path.";
        }

        long walkStart = System.nanoTime();
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> notes = walk
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !root.relativize(p).toString().startsWith(".trash")
                            && !root.relativize(p).toString().startsWith(".obsidian"))
                    .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                    .limit(limit)
                    .toList();
            log.info("recentNotes: walk+sort took {} ms", (System.nanoTime() - walkStart) / 1_000_000);

            if (notes.isEmpty()) {
                return "(no output)";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            StringBuilder sb = new StringBuilder();
            for (Path note : notes) {
                String relativePath = root.relativize(note).toString();
                String modified = Instant.ofEpochMilli(lastModifiedMillis(note))
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
                sb.append(relativePath).append("\t").append(modified).append("\n");
            }
            log.info("recentNotes: total took {} ms", (System.nanoTime() - start) / 1_000_000);
            return sb.toString().strip();
        } catch (IOException e) {
            log.error("Failed to walk vault for recent notes", e);
            return "Error: " + e.getMessage();
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path resolveVaultRoot() {
        Path root = vaultRoot;
        if (root != null) {
            return root;
        }
        synchronized (this) {
            if (vaultRoot == null) {
                String output = run("vault", "info=path").strip();
                if (output.isEmpty() || output.startsWith("Error")) {
                    log.error("Failed to resolve vault root path: {}", output);
                    return null;
                }
                vaultRoot = Path.of(output);
            }
            return vaultRoot;
        }
    }

    private String capLines(String output) {
        if (output.isEmpty() || output.equals("(no output)")) {
            return output;
        }
        String[] lines = output.split("\n");
        if (lines.length <= MAX_LISTED_FILES) {
            return output;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_LISTED_FILES; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... and ").append(lines.length - MAX_LISTED_FILES)
                .append(" more (total: ").append(lines.length).append(")");
        return sb.toString();
    }

    private String run(String... args) {
        List<String> command = new ArrayList<>();
        command.add(OBSIDIAN);
        command.add("vault=" + VAULT);
        command.addAll(List.of(args));

        log.debug("Running obsidian CLI: {}", String.join(" ", command));
        long start = System.nanoTime();

        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
        } catch (IOException e) {
            log.error("Failed to start obsidian CLI", e);
            return "Error: " + e.getMessage();
        }

        // Drain stdout on a separate thread: readLine() blocks until the process
        // writes something, which can hang far longer than the waitFor timeout
        // below if the CLI is itself waiting on the Obsidian app (e.g. cold start).
        StringBuilder outputBuilder = new StringBuilder();
        Thread drainer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            } catch (IOException ignored) {
                // stream closed by destroyForcibly below
            }
        });
        drainer.setDaemon(true);
        drainer.start();

        try {
            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                log.warn("obsidian CLI timed out after {} ms: {}", (System.nanoTime() - start) / 1_000_000,
                        String.join(" ", command));
                return "Error: obsidian CLI timed out.";
            }

            drainer.join(2000);
            String output = outputBuilder.toString().strip();
            log.debug("obsidian CLI finished in {} ms", (System.nanoTime() - start) / 1_000_000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("obsidian CLI exited with code {}: {}", exitCode, output);
                return "Error (exit " + exitCode + "): " + output;
            }

            return output.isEmpty() ? "(no output)" : output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: command was interrupted.";
        }
    }
}
