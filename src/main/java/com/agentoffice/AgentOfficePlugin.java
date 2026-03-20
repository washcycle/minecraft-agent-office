package com.agentoffice;

import com.agentoffice.beads.BeadsCliClient;
import com.agentoffice.beads.BeadsPoller;
import com.agentoffice.claude.ClaudeParaphraser;
import com.agentoffice.command.OfficeCommand;
import com.agentoffice.config.PluginConfig;
import com.agentoffice.elevator.ElevatorController;
import com.agentoffice.layout.DeskRegistry;
import com.agentoffice.layout.OfficeLayout;
import com.agentoffice.npc.AgentRegistry;
import com.agentoffice.npc.TeamLeaderNpc;
import org.bukkit.plugin.java.JavaPlugin;

public class AgentOfficePlugin extends JavaPlugin {

    private static AgentOfficePlugin instance;

    private PluginConfig pluginConfig;
    private BeadsCliClient beadsClient;
    private BeadsPoller beadsPoller;
    private ClaudeParaphraser paraphraser;
    private OfficeLayout officeLayout;
    private DeskRegistry deskRegistry;
    private ElevatorController elevator;
    private AgentRegistry agentRegistry;
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

        // 2. Initialize subsystems
        beadsClient = new BeadsCliClient(pluginConfig.getBeadsBinary(), getLogger());
        paraphraser = new ClaudeParaphraser(pluginConfig.getClaudeApiKey(), getLogger());
        officeLayout = new OfficeLayout(pluginConfig);
        deskRegistry = new DeskRegistry(officeLayout);
        elevator = new ElevatorController(this, officeLayout);

        // 3. Agent registry (event listener for spawn/despawn)
        agentRegistry = new AgentRegistry(this, officeLayout, deskRegistry, elevator, paraphraser, getLogger());
        getServer().getPluginManager().registerEvents(agentRegistry, this);

        // 4. Team leader NPC
        teamLeader = new TeamLeaderNpc(this, pluginConfig, beadsClient, agentRegistry, getLogger());
        getServer().getPluginManager().registerEvents(teamLeader, this);
        teamLeader.spawn();

        // 5. Beads poller — restore existing tasks immediately, then schedule poll loop
        beadsPoller = new BeadsPoller(beadsClient, this, getLogger());
        beadsPoller.startupRestore();
        long pollTicks = pluginConfig.getPollIntervalSeconds() * 20L;
        beadsPoller.runTaskTimerAsynchronously(this, pollTicks, pollTicks);

        // 6. Register commands
        var officeCmd = new OfficeCommand(this);
        var cmd = getCommand("office");
        if (cmd != null) {
            cmd.setExecutor(officeCmd);
            cmd.setTabCompleter(officeCmd);
        }

        getLogger().info("AgentOffice enabled — " + pluginConfig.getDesks().size() + " desks ready.");
    }

    @Override
    public void onDisable() {
        if (beadsPoller != null) beadsPoller.cancel();
        if (teamLeader != null) teamLeader.remove();
        if (agentRegistry != null) {
            agentRegistry.getAgents().values().forEach(npc -> npc.remove());
        }
        getLogger().info("AgentOffice disabled.");
        instance = null;
    }

    public static AgentOfficePlugin getInstance() { return instance; }
    public PluginConfig getPluginConfig2() { return pluginConfig; }
}
