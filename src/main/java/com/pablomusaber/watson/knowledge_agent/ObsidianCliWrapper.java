package com.pablomusaber.watson.knowledge_agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ObsidianCliWrapper {

    private static final String OBSIDIAN = "/usr/bin/obsidian";
    private static final String VAULT = "SecondBrain";

    @Tool(name = "obsidian_search", description = "Search the Obsidian vault for notes matching a text query")
    public String search(
            @ToolParam(description = "Search query text") String query,
            @ToolParam(description = "Maximum number of results to return") int limit) {
        return run("search", "query=\"" + query + "\"", "limit=" + limit, "format=json");
    }

    @Tool(name = "obsidian_read", description = "Read the full contents of a note in the Obsidian vault by file path")
    public String readNote(
            @ToolParam(description = "File path relative to vault root, e.g. Inbox/MyNote.md") String path) {
        return run("read", "path=\"" + path + "\"");
    }

    @Tool(name = "obsidian_create", description = "Create a new note in the Obsidian vault")
    public String createNote(
            @ToolParam(description = "Note name without .md extension") String name,
            @ToolParam(description = "Initial markdown content for the note") String content,
            @ToolParam(description = "Optional template name to apply") String template) {
        List<String> args = new ArrayList<>(List.of("create", "name=\"" + name + "\"", "content=\"" + content + "\""));
        if (template != null && !template.isBlank()) {
            args.add("template=" + template);
        }
        return run(args.toArray(new String[0]));
    }

    @Tool(name = "obsidian_append", description = "Append content to an existing note in the Obsidian vault")
    public String appendNote(
            @ToolParam(description = "File path relative to vault root, e.g. Inbox/MyNote.md") String path,
            @ToolParam(description = "Content to append") String content) {
        return run("append", "path=\"" + path + "\"", "content=\"" + content + "\"");
    }

    @Tool(name = "obsidian_list_files", description = "List files in a folder of the Obsidian vault")
    public String listNotes(
            @ToolParam(description = "Folder path relative to vault root (empty for root)") String folder) {
        if (folder == null || folder.isBlank()) {
            return run("files", "folder=/");
        }
        return run("files", "folder=\"" + folder + "\"");
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
        return run("tasks", "path=\"" + path + "\"");
    }

    @Tool(name = "obsidian_file_info", description = "Get metadata about a file in the Obsidian vault")
    public String getFileInfo(
            @ToolParam(description = "File path relative to vault root") String path) {
        return run("file", "path=\"" + path + "\"");
    }

    private String run(String... args) {
        List<String> command = new ArrayList<>();
        command.add(OBSIDIAN);
        command.add("vault=" + VAULT);
        command.addAll(List.of(args));

        log.debug("Running obsidian CLI: {}", String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                output = sb.toString().strip();
            }

            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return "Error: obsidian CLI timed out.";
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("obsidian CLI exited with code {}: {}", exitCode, output);
                return "Error (exit " + exitCode + "): " + output;
            }

            return output;
        } catch (IOException e) {
            log.error("Failed to run obsidian CLI", e);
            return "Error: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: command was interrupted.";
        }
    }
}
