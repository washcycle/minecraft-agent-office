package com.agentoffice;

import com.agentoffice.command.OfficeCommand;
import com.agentoffice.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class AgentOfficePlugin extends JavaPlugin {

    private static AgentOfficePlugin instance;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Load and validate config
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        if (!pluginConfig.load()) {
            getLogger().severe("Configuration invalid — disabling AgentOffice.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        var officeCmd = new OfficeCommand(this);
        var cmd = getCommand("office");
        if (cmd != null) {
            cmd.setExecutor(officeCmd);
            cmd.setTabCompleter(officeCmd);
        }

        getLogger().info("AgentOffice enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AgentOffice disabled.");
        instance = null;
    }

    public static AgentOfficePlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig2() {
        return pluginConfig;
    }
}
