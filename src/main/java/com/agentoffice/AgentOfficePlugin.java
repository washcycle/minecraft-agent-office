package com.agentoffice;

import com.agentoffice.beads.BeadsCliClient;
import com.agentoffice.beads.BeadsPoller;
import com.agentoffice.claude.ClaudeParaphraser;
import com.agentoffice.command.OfficeCommand;
import com.agentoffice.config.PluginConfig;
import com.agentoffice.elevator.ElevatorController;
import com.agentoffice.layout.OfficeLayout;
import com.agentoffice.npc.AgentRegistry;
import com.agentoffice.npc.TeamLeaderNpc;
import com.agentoffice.session.FloorConstructor;
import com.agentoffice.session.FloorRegistry;
import com.agentoffice.session.SessionEventHandler;
import com.agentoffice.session.SessionPoller;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class AgentOfficePlugin extends JavaPlugin {

    private static AgentOfficePlugin instance;

    private PluginConfig pluginConfig;
    private BeadsCliClient beadsClient;
    private BeadsPoller beadsPoller;
    private ClaudeParaphraser paraphraser;
    private OfficeLayout officeLayout;
    private FloorRegistry floorRegistry;
    private FloorConstructor floorConstructor;
    private ElevatorController elevator;
    private AgentRegistry agentRegistry;
    private SessionPoller sessionPoller;
    private SessionEventHandler sessionEventHandler;
    private TeamLeaderNpc teamLeader;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load and validate config
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        if (!pluginConfig.load()) {
            getLogger().severe("Configuration invalid — disabling AgentOffice.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Core subsystems
        beadsClient = new BeadsCliClient(pluginConfig.getBeadsBinary(), getLogger());
        paraphraser = new ClaudeParaphraser(pluginConfig.getClaudeApiKey(), getLogger());
        officeLayout = new OfficeLayout(pluginConfig);

        // 3. Floor registry + constructor
        floorRegistry = new FloorRegistry(
                pluginConfig.getLobbyY(),
                pluginConfig.getFloorHeight(),
                pluginConfig.getBuildingX(),
                pluginConfig.getBuildingZ(),
                pluginConfig.getDesksPerFloor(),
                pluginConfig.getMaxFloors(),
                getLogger());

        World world = getServer().getWorld(pluginConfig.getOfficeWorld());

        if (world != null) {
            floorConstructor = new FloorConstructor(
                    this, world,
                    pluginConfig.getBuildingX(),
                    pluginConfig.getBuildingZ(),
                    pluginConfig.getFloorWidth(),
                    pluginConfig.getFloorDepth(),
                    getLogger());
        } else {
            getLogger().warning("[AgentOffice] Office world '" + pluginConfig.getOfficeWorld() + "' not loaded yet — floor construction unavailable.");
        }

        // 4. Elevator + agent registry
        elevator = new ElevatorController(this, officeLayout);
        agentRegistry = new AgentRegistry(this, pluginConfig, floorRegistry, elevator, paraphraser, getLogger());
        getServer().getPluginManager().registerEvents(agentRegistry, this);

        // 5. Session event handler (wires SessionStart/Stop → floor assignment)
        if (floorConstructor != null) {
            sessionEventHandler = new SessionEventHandler(floorRegistry, floorConstructor, agentRegistry, world, getLogger());
            getServer().getPluginManager().registerEvents(sessionEventHandler, this);
        }

        // 6. Session poller — reads ~/.claude/office-sessions.json
        sessionPoller = new SessionPoller(this, pluginConfig.getSessionsFile(),
                pluginConfig.getSessionExpiryMinutes(), getLogger());
        // Run immediately to seed floor registry from existing sessions, then every 5s
        sessionPoller.runTaskTimerAsynchronously(this, 0L, 100L); // 5s = 100 ticks

        // 7. Beads poller — starts after a short delay to let session poller seed floors
        beadsPoller = new BeadsPoller(beadsClient, floorRegistry, this, getLogger());
        long pollTicks = pluginConfig.getPollIntervalSeconds() * 20L;
        // Delay beads restore by 20 ticks (1s) to let session poller fire first
        getServer().getScheduler().runTaskLater(this, () -> {
            beadsPoller.startupRestore();
            beadsPoller.runTaskTimerAsynchronously(AgentOfficePlugin.this, pollTicks, pollTicks);
        }, 20L);

        // 8. Team leader NPC
        teamLeader = new TeamLeaderNpc(this, pluginConfig, beadsClient, agentRegistry, floorRegistry, getLogger());
        getServer().getPluginManager().registerEvents(teamLeader, this);
        teamLeader.spawn();

        // 9. Register commands
        var officeCmd = new OfficeCommand(this, agentRegistry, floorRegistry);
        var cmd = getCommand("office");
        if (cmd != null) {
            cmd.setExecutor(officeCmd);
            cmd.setTabCompleter(officeCmd);
        }

        getLogger().info("AgentOffice enabled — multi-floor high-rise ready.");
    }

    @Override
    public void onDisable() {
        if (sessionPoller != null) { try { sessionPoller.cancel(); } catch (Exception ignored) {} }
        if (beadsPoller != null) { try { beadsPoller.cancel(); } catch (Exception ignored) {} }
        if (teamLeader != null) teamLeader.remove();
        if (agentRegistry != null) agentRegistry.getAgents().values().forEach(npc -> npc.remove());
        getLogger().info("AgentOffice disabled.");
        instance = null;
    }

    public static AgentOfficePlugin getInstance() { return instance; }
    public PluginConfig getPluginConfig2() { return pluginConfig; }
    public FloorRegistry getFloorRegistry() { return floorRegistry; }
    public AgentRegistry getAgentRegistry() { return agentRegistry; }
}
