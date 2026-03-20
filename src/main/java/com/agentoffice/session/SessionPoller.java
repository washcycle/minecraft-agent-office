package com.agentoffice.session;

import com.agentoffice.event.SessionStartEvent;
import com.agentoffice.event.SessionStopEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * BukkitRunnable that polls ~/.claude/office-sessions.json every run and fires
 * SessionStartEvent / SessionStopEvent for project-path deltas.
 *
 * <p>Stale entries (started older than {@code expiryMinutes}) are treated as
 * stopped and removed from the file atomically.</p>
 */
public class SessionPoller extends BukkitRunnable {

    private final Plugin plugin;
    private final Path sessionsFilePath;
    private final long expiryMinutes;
    private final Logger logger;

    /** Previously known sessions: projectPath → sessionId */
    private final Map<String, String> knownProjects = new HashMap<>();

    public SessionPoller(Plugin plugin, String sessionsFilePath, long expiryMinutes, Logger logger) {
        this.plugin = plugin;
        this.sessionsFilePath = Paths.get(sessionsFilePath);
        this.expiryMinutes = expiryMinutes;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            tick();
        } catch (Exception e) {
            logger.warning("[AgentOffice] SessionPoller unexpected error — skipping cycle: " + e.getMessage());
        }
    }

    private void tick() throws Exception {
        if (!Files.exists(sessionsFilePath)) {
            logger.fine("[AgentOffice] Sessions file not found, treating as empty: " + sessionsFilePath);
            handleDelta(new HashMap<>());
            return;
        }

        String raw = Files.readString(sessionsFilePath);
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        JsonArray sessions = root.has("sessions") ? root.getAsJsonArray("sessions") : new JsonArray();

        Instant cutoff = Instant.now().minus(expiryMinutes, ChronoUnit.MINUTES);

        // Separate fresh from stale
        Map<String, String> freshProjects = new HashMap<>();   // projectPath → sessionId
        Map<String, String> staleProjects = new HashMap<>();   // projectPath → sessionId
        JsonArray survivingEntries = new JsonArray();

        for (JsonElement el : sessions) {
            JsonObject entry = el.getAsJsonObject();
            String path    = entry.has("path")    ? entry.get("path").getAsString()    : null;
            String id      = entry.has("id")      ? entry.get("id").getAsString()      : null;
            String started = entry.has("started") ? entry.get("started").getAsString() : null;

            if (path == null || id == null) continue;

            boolean stale = false;
            if (started != null) {
                try {
                    Instant startedAt = Instant.parse(started);
                    stale = startedAt.isBefore(cutoff);
                } catch (Exception parseEx) {
                    logger.warning("[AgentOffice] Could not parse started timestamp '" + started + "': " + parseEx.getMessage());
                }
            }

            if (stale) {
                staleProjects.put(path, id);
            } else {
                freshProjects.put(path, id);
                survivingEntries.add(entry);
            }
        }

        // Atomically rewrite file without stale entries if any were pruned
        if (staleProjects.size() > 0) {
            JsonObject updated = new JsonObject();
            updated.add("sessions", survivingEntries);
            Path tmp = sessionsFilePath.resolveSibling(sessionsFilePath.getFileName() + ".tmp");
            Files.writeString(tmp, updated.toString());
            Files.move(tmp, sessionsFilePath, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Fire stop for stale entries
            for (Map.Entry<String, String> stale : staleProjects.entrySet()) {
                String projectPath = stale.getKey();
                String projectName = basename(projectPath);
                logger.fine("[AgentOffice] Session expired: " + projectName);
                fireStop(projectPath, projectName);
                knownProjects.remove(projectPath);
            }
        }

        handleDelta(freshProjects);
    }

    /**
     * Compares {@code current} (projectPath → sessionId) against {@code knownProjects},
     * fires start/stop events for the diff, then updates knownProjects.
     */
    private void handleDelta(Map<String, String> current) {
        // New project paths → start
        for (Map.Entry<String, String> entry : current.entrySet()) {
            String projectPath = entry.getKey();
            if (!knownProjects.containsKey(projectPath)) {
                String projectName = basename(projectPath);
                logger.fine("[AgentOffice] Session started: " + projectName);
                fireStart(projectPath, projectName);
            }
        }

        // Removed project paths → stop
        Set<String> removed = new HashSet<>(knownProjects.keySet());
        removed.removeAll(current.keySet());
        for (String projectPath : removed) {
            String projectName = basename(projectPath);
            logger.fine("[AgentOffice] Session stopped: " + projectName);
            fireStop(projectPath, projectName);
        }

        knownProjects.clear();
        knownProjects.putAll(current);
    }

    private void fireStart(String projectPath, String projectName) {
        if (Bukkit.getServer() == null) return;
        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.getPluginManager().callEvent(new SessionStartEvent(projectPath, projectName)));
    }

    private void fireStop(String projectPath, String projectName) {
        if (Bukkit.getServer() == null) return;
        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.getPluginManager().callEvent(new SessionStopEvent(projectPath, projectName)));
    }

    private static String basename(String path) {
        if (path == null || path.isEmpty()) return "";
        Path p = Paths.get(path);
        Path fileName = p.getFileName();
        return fileName != null ? fileName.toString() : path;
    }
}
