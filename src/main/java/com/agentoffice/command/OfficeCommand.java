package com.agentoffice.command;

import com.agentoffice.AgentOfficePlugin;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.layout.DeskRegistry;
import com.agentoffice.layout.OfficeLayout;
import com.agentoffice.npc.AgentNpc;
import com.agentoffice.npc.AgentRegistry;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the /office command and its subcommands:
 *   reload    — re-read config.yml
 *   setup     — auto-generate desk layout
 *   visualise — show particle markers
 *   status    — show active agents and desk occupancy
 */
public class OfficeCommand implements CommandExecutor, TabCompleter {

    private final AgentOfficePlugin plugin;
    private final AgentRegistry agentRegistry;
    private final DeskRegistry deskRegistry;

    public OfficeCommand(AgentOfficePlugin plugin, AgentRegistry agentRegistry, DeskRegistry deskRegistry) {
        this.plugin = plugin;
        this.agentRegistry = agentRegistry;
        this.deskRegistry = deskRegistry;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cYou must be in-game to use /office setup auto.");
            return true;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("auto")) {
            sender.sendMessage("§cUsage: /office setup auto <rows> <cols>");
            return true;
        }
        int rows, cols;
        try {
            rows = Integer.parseInt(args[2]);
            cols = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cRows and cols must be integers.");
            return true;
        }

        Location origin = player.getLocation();
        int ox = origin.getBlockX(), oy = origin.getBlockY(), oz = origin.getBlockZ();

        List<Map<String, Object>> deskList = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                deskList.add(Map.of(
                        "x", ox + c * 2,
                        "y", oy,
                        "z", oz + r * 2,
                        "facing", "NORTH"
                ));
            }
        }

        FileConfiguration cfg = plugin.getConfig();
        cfg.set("desks", deskList);
        plugin.saveConfig();
        plugin.getPluginConfig2().load();

        sender.sendMessage("§a" + (rows * cols) + " desks written to config.yml starting at "
                + ox + "," + oy + "," + oz);
        return true;
    }

    private boolean handleVisualise(CommandSender sender) {
        OfficeLayout layout = new OfficeLayout(plugin.getPluginConfig2());
        World world = plugin.getServer().getWorld(plugin.getPluginConfig2().getOfficeWorld());
        if (world == null) {
            sender.sendMessage("§cOffice world '" + plugin.getPluginConfig2().getOfficeWorld() + "' not found.");
            return true;
        }

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;
                // Desk markers (green)
                for (DeskConfig desk : layout.getDesks()) {
                    Location loc = new Location(world, desk.x() + 0.5, desk.y() + 1, desk.z() + 0.5);
                    world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.3, 0.3, 0.3, 0);
                }
                // Elevator marker (yellow)
                var elev = layout.getElevatorTop();
                Location elevLoc = new Location(world, elev.x() + 0.5, elev.y() + 1, elev.z() + 0.5);
                world.spawnParticle(Particle.NOTE, elevLoc, 3, 0.3, 0.3, 0.3, 0);

                if (elapsed >= 10) cancel(); // 10 seconds at 1/s
            }
        }.runTaskTimer(plugin, 0L, 20L);

        sender.sendMessage("§aShowing office layout for 10 seconds...");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        Map<String, AgentNpc> agents = agentRegistry.getAgents();
        int totalDesks = deskRegistry.freeCount() + agents.size();
        int occupied = agents.size();
        int free = deskRegistry.freeCount();

        sender.sendMessage("§6=== AgentOffice Status ===");
        sender.sendMessage("§7Desks: §a" + free + " free §7/ §c" + occupied + " occupied §7/ §f" + totalDesks + " total");

        if (agents.isEmpty()) {
            sender.sendMessage("§7No active agents.");
        } else {
            sender.sendMessage("§7Active agents (" + agents.size() + "):");
            for (AgentNpc npc : agents.values()) {
                String label = npc.getEntity() != null && npc.getEntity().getCustomName() != null
                        ? npc.getEntity().getCustomName()
                        : npc.getTaskId();
                sender.sendMessage("§7  • §f" + label + " §8[" + npc.getTaskId() + "]");
            }
        }
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
