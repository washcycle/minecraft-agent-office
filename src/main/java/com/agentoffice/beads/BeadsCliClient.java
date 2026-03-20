package com.agentoffice.beads;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implements BeadsClient by shelling out to the `bd` CLI.
 *
 * Supports polling multiple project directories. Each project's task IDs are
 * prefixed with the directory name (e.g. "armada-vscode:armada-42") so that
 * tasks from different repos never collide in the agent registry.
 */
public class BeadsCliClient implements BeadsClient {

    private static final int TIMEOUT_SECONDS = 10;
    private final String beadsBinary;
    private final List<File> projectDirs;
    private final Logger logger;
    private final Gson gson = new Gson();

    /** Convenience constructor — no project directories configured. */
    public BeadsCliClient(String beadsBinary, Logger logger) {
        this(beadsBinary, List.of(), logger);
    }

    public BeadsCliClient(String beadsBinary, List<String> projectPaths, Logger logger) {
        this.beadsBinary = beadsBinary;
        this.logger = logger;
        this.projectDirs = new ArrayList<>();
        for (String path : projectPaths) {
            File dir = new File(path);
            if (dir.isDirectory()) {
                projectDirs.add(dir);
            } else {
                logger.warning("[AgentOffice] beads-project path not found, skipping: " + path);
            }
        }
    }

    @Override
    public List<BeadsTask> listInProgress() throws BeadsException {
        return listAcrossProjects("--status=in_progress");
    }

    @Override
    public List<BeadsTask> listAll() throws BeadsException {
        return listAcrossProjects();
    }

    /**
     * Polls in-progress tasks from a specific project directory and prefixes
     * all task IDs with {@code slug:} to ensure global uniqueness across projects.
     *
     * @param projectDir the repo directory to run {@code bd list} in
     * @param slug       short name to prefix task IDs (e.g. "armada-vscode")
     * @return list of tasks with prefixed IDs, empty on error
     */
    public List<BeadsTask> listInProgressFrom(File projectDir, String slug) {
        try {
            List<BeadsTask> tasks = run(projectDir, buildCommand("--status=in_progress"));
            List<BeadsTask> prefixed = new ArrayList<>();
            for (BeadsTask t : tasks) {
                prefixed.add(new BeadsTask(slug + ":" + t.id(), t.title(), t.description(), t.assignee(), t.status()));
            }
            return prefixed;
        } catch (BeadsException e) {
            logger.warning("[AgentOffice] beads poll failed for " + slug + ": " + e.getMessage());
            return List.of();
        }
    }

    private List<BeadsTask> listAcrossProjects(String... extraArgs) throws BeadsException {
        List<BeadsTask> all = new ArrayList<>();

        if (projectDirs.isEmpty()) {
            // Fallback: poll from current working directory (original behaviour)
            String[] cmd = buildCommand(extraArgs);
            all.addAll(run(null, cmd));
        } else {
            for (File dir : projectDirs) {
                String slug = dir.getName();
                try {
                    String[] cmd = buildCommand(extraArgs);
                    List<BeadsTask> tasks = run(dir, cmd);
                    for (BeadsTask t : tasks) {
                        // Prefix ID with project slug so IDs are globally unique
                        all.add(new BeadsTask(
                                slug + ":" + t.id(),
                                t.title(),
                                t.description(),
                                t.assignee(),
                                t.status()
                        ));
                    }
                } catch (BeadsException e) {
                    logger.warning("[AgentOffice] Failed to poll beads in " + slug + ": " + e.getMessage());
                    // Continue with other projects
                }
            }
        }

        return all;
    }

    private String[] buildCommand(String... extraArgs) {
        List<String> cmd = new ArrayList<>();
        cmd.add(beadsBinary);
        cmd.add("list");
        cmd.add("--json");
        for (String arg : extraArgs) cmd.add(arg);
        return cmd.toArray(new String[0]);
    }

    private List<BeadsTask> run(File workingDir, String... command) throws BeadsException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(false);
            if (workingDir != null) pb.directory(workingDir);

            Process proc = pb.start();
            boolean finished = proc.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new BeadsException("bd CLI timed out after " + TIMEOUT_SECONDS + "s");
            }

            if (proc.exitValue() != 0) {
                String stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()))
                        .lines().collect(Collectors.joining("\n"));
                throw new BeadsException("bd CLI exited " + proc.exitValue() + ": " + stderr);
            }

            String stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n")).trim();

            return parseJson(stdout);

        } catch (BeadsException e) {
            throw e;
        } catch (Exception e) {
            throw new BeadsException("Failed to run bd CLI", e);
        }
    }

    private List<BeadsTask> parseJson(String json) throws BeadsException {
        if (json.isBlank()) return List.of();
        try {
            List<BeadsTask> tasks = new ArrayList<>();
            JsonArray arr = gson.fromJson(json, JsonArray.class);
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                tasks.add(new BeadsTask(
                        getString(obj, "id"),
                        getString(obj, "title"),
                        getStringOrEmpty(obj, "description"),
                        getStringOrNull(obj, "assignee"),
                        getString(obj, "status")
                ));
            }
            return tasks;
        } catch (Exception e) {
            throw new BeadsException("Failed to parse bd JSON output: " + e.getMessage(), e);
        }
    }

    private String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    private String getStringOrEmpty(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    private String getStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }
}
