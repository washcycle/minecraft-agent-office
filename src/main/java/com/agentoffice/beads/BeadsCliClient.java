package com.agentoffice.beads;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implements BeadsClient by shelling out to the `bd` CLI.
 * JSON output is a JSON array of task objects.
 */
public class BeadsCliClient implements BeadsClient {

    private static final int TIMEOUT_SECONDS = 10;
    private final String beadsBinary;
    private final Logger logger;
    private final Gson gson = new Gson();

    public BeadsCliClient(String beadsBinary, Logger logger) {
        this.beadsBinary = beadsBinary;
        this.logger = logger;
    }

    @Override
    public List<BeadsTask> listInProgress() throws BeadsException {
        return run(beadsBinary, "list", "--status=in_progress", "--json");
    }

    @Override
    public List<BeadsTask> listAll() throws BeadsException {
        return run(beadsBinary, "list", "--json");
    }

    private List<BeadsTask> run(String... command) throws BeadsException {
        try {
            Process proc = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();

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
