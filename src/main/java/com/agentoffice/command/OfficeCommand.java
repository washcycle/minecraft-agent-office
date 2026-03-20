package com.agentoffice.command;

import com.agentoffice.AgentOfficePlugin;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.layout.DeskRegistry;
import com.agentoffice.layout.OfficeLayout;
import com.agentoffice.npc.AgentNpc;
import com.agentoffice.npc.AgentRegistry;
import com.agentoffice.session.FloorRegistry;
import com.agentoffice.session.FloorSlot;
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
    private final FloorRegistry floorRegistry;

    public OfficeCommand(AgentOfficePlugin plugin, AgentRegistry agentRegistry, FloorRegistry floorRegistry) {
        this.plugin = plugin;
        this.agentRegistry = agentRegistry;
        this.floorRegistry = floorRegistry;
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
        World world = plugin.getServer().getWorld(plugin.getPluginConfig2().getOfficeWorld());
        if (world == null) {
            sender.sendMessage("§cOffice world '" + plugin.getPluginConfig2().getOfficeWorld() + "' not found.");
            return true;
        }

        var cfg = plugin.getPluginConfig2();
        var allSlots = plugin.getFloorRegistry().getAllSlots();
        int buildingX = cfg.getBuildingX(), buildingZ = cfg.getBuildingZ();

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;
                // Per-floor desk markers
                for (var slot : allSlots) {
                    var desks = slot.computeDesks(buildingX, buildingZ, cfg.getDesksPerFloor());
                    var particle = slot.state() == FloorSlot.FloorState.OCCUPIED
                            ? Particle.VILLAGER_HAPPY : Particle.SMOKE_NORMAL;
                    for (DeskConfig desk : desks) {
                        Location loc = new Location(world, desk.x() + 0.5, desk.y() + 1, desk.z() + 0.5);
                        world.spawnParticle(particle, loc, 3, 0.2, 0.2, 0.2, 0);
                    }
                }
                // Elevator shaft marker (note particles at each floor level)
                var elevBase = cfg.getElevatorPos();
                for (var slot : allSlots) {
                    Location floorEntry = new Location(world, elevBase.x() + 0.5, slot.yBase() + 1, elevBase.z() + 0.5);
                    world.spawnParticle(Particle.NOTE, floorEntry, 2, 0.2, 0.2, 0.2, 0);
                }
                // Lobby elevator marker
                Location lobbyLoc = new Location(world, elevBase.x() + 0.5, cfg.getLobbyY() + 1, elevBase.z() + 0.5);
                world.spawnParticle(Particle.NOTE, lobbyLoc, 5, 0.3, 0.3, 0.3, 0);

                if (elapsed >= 10) cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        sender.sendMessage("§aShowing high-rise layout for 10 seconds ("
                + allSlots.size() + " floor" + (allSlots.size() == 1 ? "" : "s") + ")...");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        Map<String, AgentNpc> agents = agentRegistry.getAgents();
        var allSlots = floorRegistry.getAllSlots();

        sender.sendMessage("§6=== AgentOffice High-Rise ===");
        sender.sendMessage("§7Floors: §f" + allSlots.size() + " built, §a"
                + floorRegistry.getOccupiedSlots().size() + " active");
        sender.sendMessage("§7Agents: §f" + agents.size() + " total");

        if (allSlots.isEmpty()) {
            sender.sendMessage("§7No floors yet — start a Claude Code session.");
        } else {
            for (int i = allSlots.size() - 1; i >= 0; i--) {
                FloorSlot slot = allSlots.get(i);
                if (slot.state() == FloorSlot.FloorState.VACANT) {
                    sender.sendMessage("§8  Fl." + slot.floorNumber() + " [vacant]");
                } else {
                    long floorAgentCount = agents.keySet().stream()
                            .filter(id -> slot.projectName() != null && id.startsWith(slot.projectName() + ":"))
                            .count();
                    sender.sendMessage("§a  Fl." + slot.floorNumber() + " §f[" + slot.projectName() + "] §7— §a"
                            + floorAgentCount + " agent" + (floorAgentCount == 1 ? "" : "s"));
                    for (AgentNpc npc : agents.values()) {
                        if (slot.projectName() != null && npc.getTaskId().startsWith(slot.projectName() + ":")) {
                            String label = npc.getEntity() != null && npc.getEntity().getCustomName() != null
                                    ? npc.getEntity().getCustomName() : npc.getTaskId();
                            sender.sendMessage("§7    • §f" + label);
                        }
                    }
                }
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
