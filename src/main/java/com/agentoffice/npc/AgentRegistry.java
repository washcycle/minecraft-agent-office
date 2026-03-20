package com.agentoffice.npc;

import com.agentoffice.beads.BeadsTask;
import com.agentoffice.claude.ClaudeParaphraser;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.elevator.ElevatorController;
import com.agentoffice.event.AgentDespawnEvent;
import com.agentoffice.event.AgentSpawnEvent;
import com.agentoffice.layout.DeskRegistry;
import com.agentoffice.layout.OfficeLayout;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of all agent NPCs: spawn, desk assignment, despawn, right-click interaction.
 * Listens for AgentSpawnEvent and AgentDespawnEvent.
 */
public class AgentRegistry implements Listener {

    private final Plugin plugin;
    private final OfficeLayout layout;
    private final DeskRegistry deskRegistry;
    private final ElevatorController elevator;
    private final ClaudeParaphraser paraphraser;
    private final Logger logger;

    /** taskId → AgentNpc */
    private final Map<String, AgentNpc> agents = new HashMap<>();

    public AgentRegistry(Plugin plugin, OfficeLayout layout, DeskRegistry deskRegistry,
                         ElevatorController elevator, ClaudeParaphraser paraphraser, Logger logger) {
        this.plugin = plugin;
        this.layout = layout;
        this.deskRegistry = deskRegistry;
        this.elevator = elevator;
        this.paraphraser = paraphraser;
        this.logger = logger;
    }

    @EventHandler
    public void onAgentSpawn(AgentSpawnEvent event) {
        BeadsTask task = event.getTask();

        if (agents.containsKey(task.id())) {
            logger.fine("[AgentOffice] Duplicate spawn for task " + task.id() + " — skipping.");
            return;
        }

        World world = Bukkit.getWorld(plugin.getConfig().getString("office-world", "world"));
        if (world == null) {
            logger.warning("[AgentOffice] Office world not found — cannot spawn agent.");
            return;
        }

        Optional<DeskConfig> desk = deskRegistry.assignDesk(task.id());

        if (desk.isEmpty()) {
            // No free desk — queue the task
            deskRegistry.enqueue(task.id());
            logger.info("[AgentOffice] No free desk for " + task.id() + " — queued.");
            return;
        }

        spawnAgentAtDesk(task, desk.get(), world, event.isRestore());
    }

    private void spawnAgentAtDesk(BeadsTask task, DeskConfig desk, World world, boolean isRestore) {
        var elevBase = layout.getElevatorBase();
        Location spawnLoc = new Location(world, elevBase.x() + 0.5, elevBase.y(), elevBase.z() + 0.5);

        AgentNpc npc = new AgentNpc(task, spawnLoc);
        agents.put(task.id(), npc);

        Location deskLoc = new Location(world, desk.x() + 0.5, desk.y(), desk.z() + 0.5);

        if (isRestore) {
            // Skip elevator — place directly at desk
            npc.teleportTo(deskLoc);
        } else {
            elevator.ascend(npc.getEntity(), () -> npc.teleportTo(deskLoc));
        }

        // Async paraphrase — update name on main thread when ready
        paraphraser.paraphrase(task.id(), task.title(), task.description())
                .thenAccept(phrase -> Bukkit.getScheduler().runTask(plugin,
                        () -> npc.setLabel(phrase)));
    }

    @EventHandler
    public void onAgentDespawn(AgentDespawnEvent event) {
        String taskId = event.getTaskId();
        AgentNpc npc = agents.remove(taskId);
        if (npc == null) return;

        deskRegistry.freeDesk(taskId);

        // Exit via elevator
        World world = Bukkit.getWorld(plugin.getConfig().getString("office-world", "world"));
        if (npc.isValid() && world != null) {
            elevator.descend(npc.getEntity(), () -> {
                // After exit, check if anything was queued
                deskRegistry.dequeueNext().ifPresent(queuedTaskId -> {
                    // Re-fire spawn for queued task (not a restore)
                    logger.info("[AgentOffice] Dequeuing task " + queuedTaskId);
                });
            });
        } else {
            npc.remove();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.ArmorStand clicked)) return;

        for (AgentNpc npc : agents.values()) {
            if (npc.getEntity() != null && npc.getEntity().equals(clicked)) {
                Player player = event.getPlayer();
                // Full task details will be shown — taskId allows lookup
                player.sendMessage("§6Agent: §f" + npc.getTaskId());
                player.sendMessage("§7Right-click the team leader for full task details.");
                event.setCancelled(true);
                return;
            }
        }
    }

    public Map<String, AgentNpc> getAgents() {
        return agents;
    }
}
