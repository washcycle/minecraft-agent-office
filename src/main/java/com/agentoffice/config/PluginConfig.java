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

    // Core
    private String officeWorld;
    private String claudeApiKey;
    private String beadsBinary;
    private int pollIntervalSeconds;

    // Session detection
    private String sessionsFile;
    private int sessionExpiryMinutes;

    // Team leader
    private BlockPos teamLeaderPos;
    private int teamLeaderBroadcastIntervalSeconds;

    // Elevator
    private BlockPos elevatorPos;
    private int elevatorHeight;

    // Building
    private int buildingX;
    private int buildingZ;
    private int lobbyY;

    // Floor geometry
    private int floorHeight;
    private int floorWidth;
    private int floorDepth;
    private int desksPerFloor;
    private int maxFloors;

    // Legacy — kept for backward compat warning only
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
            plugin.getLogger().warning("[AgentOffice] claude-api-key not set — NPCs will show raw task titles.");
        }

        beadsBinary = cfg.getString("beads-binary", "bd");
        if (!new File(beadsBinary).exists() && !isOnPath(beadsBinary)) {
            plugin.getLogger().warning("[AgentOffice] beads binary '" + beadsBinary + "' not found on PATH — some features may fail.");
        }

        pollIntervalSeconds = cfg.getInt("poll-interval-seconds", 10);

        // Session detection
        String rawSessionsFile = cfg.getString("sessions-file", "~/.claude/office-sessions.json");
        sessionsFile = rawSessionsFile.replace("~", System.getProperty("user.home"));
        sessionExpiryMinutes = cfg.getInt("session-expiry-minutes", 480);

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

        // Building origin
        buildingX = cfg.getInt("building.x", 0);
        buildingZ = cfg.getInt("building.z", 0);
        lobbyY = cfg.getInt("building.lobby-y", 64);

        // Floor geometry
        floorHeight = cfg.getInt("floor.height", 8);
        floorWidth = cfg.getInt("floor.width", 10);
        floorDepth = cfg.getInt("floor.depth", 10);
        desksPerFloor = cfg.getInt("floor.desks-per-floor", 6);
        maxFloors = cfg.getInt("floor.max-floors", 8);

        // Legacy flat desks list — warn and ignore
        desks = new ArrayList<>();
        var deskList = cfg.getMapList("desks");
        if (!deskList.isEmpty()) {
            plugin.getLogger().warning("[AgentOffice] Legacy 'desks' list in config.yml is ignored. Desk positions are now computed per floor.");
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
    public String getSessionsFile() { return sessionsFile; }
    public int getSessionExpiryMinutes() { return sessionExpiryMinutes; }
    public BlockPos getTeamLeaderPos() { return teamLeaderPos; }
    public int getTeamLeaderBroadcastIntervalSeconds() { return teamLeaderBroadcastIntervalSeconds; }
    public BlockPos getElevatorPos() { return elevatorPos; }
    public int getElevatorHeight() { return elevatorHeight; }
    public int getBuildingX() { return buildingX; }
    public int getBuildingZ() { return buildingZ; }
    public int getLobbyY() { return lobbyY; }
    public int getFloorHeight() { return floorHeight; }
    public int getFloorWidth() { return floorWidth; }
    public int getFloorDepth() { return floorDepth; }
    public int getDesksPerFloor() { return desksPerFloor; }
    public int getMaxFloors() { return maxFloors; }

    // Legacy accessor — kept for compile compat with old code, returns empty
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
