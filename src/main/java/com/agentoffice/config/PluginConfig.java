package com.agentoffice.config;

import com.agentoffice.AgentOfficePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and validates config.yml. Call load() on startup; returns false if invalid.
 * Supports reload via the /office reload command.
 */
public class PluginConfig {

    private final AgentOfficePlugin plugin;

    // Loaded values
    private String officeWorld;
    private String claudeApiKey;
    private String beadsBinary;
    private int pollIntervalSeconds;
    private BlockPos teamLeaderPos;
    private int teamLeaderBroadcastIntervalSeconds;
    private BlockPos elevatorPos;
    private int elevatorHeight;
    private List<DeskConfig> desks;

    public PluginConfig(AgentOfficePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Reads and validates config.yml. Returns true on success, false on fatal error.
     */
    public boolean load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        List<String> errors = new ArrayList<>();

        officeWorld = cfg.getString("office-world", "world");

        claudeApiKey = cfg.getString("claude-api-key", "");
        if (claudeApiKey.isBlank()) {
            errors.add("claude-api-key is required");
        }

        beadsBinary = cfg.getString("beads-binary", "bd");
        if (!new File(beadsBinary).exists() && !isOnPath(beadsBinary)) {
            plugin.getLogger().warning("[AgentOffice] beads binary '" + beadsBinary + "' not found on PATH — some features may fail.");
        }

        pollIntervalSeconds = cfg.getInt("poll-interval-seconds", 10);

        // Team leader position
        if (!cfg.contains("team-leader.position.x")) {
            errors.add("team-leader.position (x/y/z) is required");
        } else {
            teamLeaderPos = readBlockPos(cfg, "team-leader.position");
        }
        teamLeaderBroadcastIntervalSeconds = cfg.getInt("team-leader.broadcast-interval-seconds", 30);

        // Elevator
        if (!cfg.contains("elevator.position.x")) {
            errors.add("elevator.position (x/y/z) is required");
        } else {
            elevatorPos = readBlockPos(cfg, "elevator.position");
        }
        elevatorHeight = cfg.getInt("elevator.height", 4);

        // Desks
        desks = new ArrayList<>();
        var deskList = cfg.getMapList("desks");
        if (deskList.isEmpty()) {
            errors.add("desks list is required and must have at least one entry");
        } else {
            for (var map : deskList) {
                try {
                    int x = toInt(map.get("x"));
                    int y = toInt(map.get("y"));
                    int z = toInt(map.get("z"));
                    String facing = map.getOrDefault("facing", "NORTH").toString();
                    desks.add(new DeskConfig(x, y, z, facing));
                } catch (Exception e) {
                    errors.add("invalid desk entry: " + map);
                }
            }
        }

        if (!errors.isEmpty()) {
            for (String err : errors) {
                plugin.getLogger().severe("[AgentOffice] ERROR: " + err);
            }
            return false;
        }
        return true;
    }

    // --- Accessors ---

    public String getOfficeWorld() { return officeWorld; }
    public String getClaudeApiKey() { return claudeApiKey; }
    public String getBeadsBinary() { return beadsBinary; }
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public BlockPos getTeamLeaderPos() { return teamLeaderPos; }
    public int getTeamLeaderBroadcastIntervalSeconds() { return teamLeaderBroadcastIntervalSeconds; }
    public BlockPos getElevatorPos() { return elevatorPos; }
    public int getElevatorHeight() { return elevatorHeight; }
    public List<DeskConfig> getDesks() { return desks; }

    // --- Helpers ---

    private BlockPos readBlockPos(FileConfiguration cfg, String path) {
        int x = cfg.getInt(path + ".x");
        int y = cfg.getInt(path + ".y");
        int z = cfg.getInt(path + ".z");
        return new BlockPos(x, y, z);
    }

    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private boolean isOnPath(String binary) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, binary).canExecute()) return true;
        }
        return false;
    }
}
