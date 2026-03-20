package com.agentoffice.command;

import com.agentoffice.AgentOfficePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles the /office command and its subcommands:
 *   reload    — re-read config.yml
 *   setup     — auto-generate desk layout
 *   visualise — show particle markers
 *   status    — show active agents
 */
public class OfficeCommand implements CommandExecutor, TabCompleter {

    private final AgentOfficePlugin plugin;

    public OfficeCommand(AgentOfficePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6AgentOffice §7— subcommands: reload, setup, visualise, status");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "setup" -> handleSetup(sender, args);
            case "visualise", "visualize" -> handleVisualise(sender);
            case "status" -> handleStatus(sender);
            default -> {
                sender.sendMessage("§cUnknown subcommand. Use: reload, setup, visualise, status");
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        boolean ok = plugin.getPluginConfig2().load();
        if (ok) {
            sender.sendMessage("§aAgentOffice config reloaded successfully.");
        } else {
            sender.sendMessage("§cConfig reload failed — check server log for errors.");
        }
        return true;
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        // Full implementation in layout tasks (5.5)
        sender.sendMessage("§7[setup] Not yet implemented — coming in layout task.");
        return true;
    }

    private boolean handleVisualise(CommandSender sender) {
        // Full implementation in layout tasks (5.6)
        sender.sendMessage("§7[visualise] Not yet implemented — coming in layout task.");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        // Full implementation in commands task (9.3)
        sender.sendMessage("§7[status] Not yet implemented — coming in commands task.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "setup", "visualise", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            return List.of("auto");
        }
        return List.of();
    }
}
