package com.agentoffice.npc;

import com.agentoffice.beads.BeadsClient;
import com.agentoffice.beads.BeadsException;
import com.agentoffice.beads.BeadsTask;
import com.agentoffice.config.BlockPos;
import com.agentoffice.config.PluginConfig;
import com.agentoffice.session.FloorRegistry;
import com.agentoffice.session.FloorSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Permanent team leader NPC at the configured position.
 * Broadcasts status summaries to nearby players on a timer.
 * Right-click shows the full task list.
 */
public class TeamLeaderNpc implements Listener {

    private final Plugin plugin;
    private final PluginConfig config;
    private final BeadsClient beadsClient;
    private final AgentRegistry agentRegistry;
    private final FloorRegistry floorRegistry;
    private final Logger logger;

    private ArmorStand entity;
    private BukkitRunnable broadcastTask;

    public TeamLeaderNpc(Plugin plugin, PluginConfig config, BeadsClient beadsClient,
                          AgentRegistry agentRegistry, FloorRegistry floorRegistry, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.beadsClient = beadsClient;
        this.agentRegistry = agentRegistry;
        this.floorRegistry = floorRegistry;
        this.logger = logger;
    }

    /** Spawns the team leader and starts the broadcast timer. Call on plugin enable. */
    public void spawn() {
        World world = Bukkit.getWorld(config.getOfficeWorld());
        if (world == null) {
            logger.warning("[AgentOffice] Cannot spawn team leader — office world not found.");
            return;
        }

        BlockPos pos = config.getTeamLeaderPos();
        Location loc = new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);

        entity = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setCustomNameVisible(true);
        entity.setCustomName("§6[Team Lead] §fOffice");
        entity.setBasePlate(false);

        startBroadcastTimer();
        logger.info("[AgentOffice] Team leader spawned at " + pos);
    }

    /** Removes the team leader entity. Call on plugin disable. */
    public void remove() {
        if (broadcastTask != null) broadcastTask.cancel();
        if (entity != null && entity.isValid()) entity.remove();
        entity = null;
    }

    private void startBroadcastTimer() {
        long intervalTicks = config.getTeamLeaderBroadcastIntervalSeconds() * 20L;
        broadcastTask = new BukkitRunnable() {
            @Override
            public void run() {
                broadcastStatus();
            }
        };
        broadcastTask.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void broadcastStatus() {
        if (entity == null || !entity.isValid()) return;

        List<FloorSlot> occupied = floorRegistry.getOccupiedSlots();
        Map<String, AgentNpc> activeAgents = agentRegistry.getAgents();

        StringBuilder sb = new StringBuilder("§6[Team Lead] ");
        if (occupied.isEmpty()) {
            sb.append("§e Office is quiet — no active sessions");
        } else {
            sb.append("§a").append(occupied.size()).append(" session")
              .append(occupied.size() == 1 ? "" : "s").append(", ")
              .append("§f").append(activeAgents.size()).append(" agent")
              .append(activeAgents.size() == 1 ? "" : "s").append(" working");
            for (FloorSlot slot : occupied) {
                long floorAgents = activeAgents.keySet().stream()
                        .filter(id -> slot.projectName() != null && id.startsWith(slot.projectName() + ":"))
                        .count();
                sb.append(" §7| §6Fl.").append(slot.floorNumber())
                  .append(" §f[").append(slot.projectName()).append("] ")
                  .append("§a").append(floorAgents).append(" agent").append(floorAgents == 1 ? "" : "s");
            }
        }

        String message = sb.toString();
        Location leaderLoc = entity.getLocation();
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(leaderLoc) <= 32) {
                player.sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (entity == null || !entity.equals(event.getRightClicked())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Fetch full task list from beads
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<BeadsTask> all = beadsClient.listAll();
                Bukkit.getScheduler().runTask(plugin, () -> sendTaskList(player, all));
            } catch (BeadsException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§c[Team Lead] Failed to fetch task list: " + e.getMessage()));
            }
        });
    }

    private void sendTaskList(Player player, List<BeadsTask> tasks) {
        player.sendMessage("§6════ Agent Office — Task Board ════");
        for (String status : List.of("in_progress", "open", "done")) {
            List<BeadsTask> group = tasks.stream()
                    .filter(t -> status.equals(t.status()))
                    .toList();
            if (group.isEmpty()) continue;

            String header = switch (status) {
                case "in_progress" -> "§a▶ In Progress";
                case "open" -> "§e◆ Open";
                case "done" -> "§8✓ Done";
                default -> "§7" + status;
            };
            player.sendMessage(header + " §7(" + group.size() + ")");
            for (BeadsTask t : group) {
                String assignee = t.assignee() != null ? " §7[" + t.assignee() + "]" : "";
                player.sendMessage("  §f" + t.title() + assignee);
            }
        }
        player.sendMessage("§6══════════════════════════════════");
    }
}
