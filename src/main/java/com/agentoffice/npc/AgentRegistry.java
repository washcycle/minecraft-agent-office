package com.agentoffice.npc;

import com.agentoffice.beads.BeadsTask;
import com.agentoffice.claude.ClaudeParaphraser;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.config.PluginConfig;
import com.agentoffice.elevator.ElevatorController;
import com.agentoffice.event.AgentDespawnEvent;
import com.agentoffice.event.AgentSpawnEvent;
import com.agentoffice.layout.DeskRegistry;
import com.agentoffice.layout.OfficeLayout;
import com.agentoffice.session.FloorRegistry;
import com.agentoffice.session.FloorSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of all agent NPCs: spawn, desk assignment, despawn, right-click interaction.
 * Listens for AgentSpawnEvent and AgentDespawnEvent.
 *
 * In the multi-floor model, each AgentSpawnEvent carries a FloorSlot. The NPC is spawned
 * at the elevator base and ascends to that floor's Y level, then assigned a desk on that floor.
 */
public class AgentRegistry implements Listener {

    private final Plugin plugin;
    private final PluginConfig config;
    private final FloorRegistry floorRegistry;
    private final ElevatorController elevator;
    private final ClaudeParaphraser paraphraser;
    private final Logger logger;

    /** taskId → AgentNpc */
    private final Map<String, AgentNpc> agents = new HashMap<>();
    /** floorNumber → per-floor DeskRegistry */
    private final Map<Integer, DeskRegistry> floorDesks = new HashMap<>();

    public AgentRegistry(Plugin plugin, PluginConfig config, FloorRegistry floorRegistry,
                         ElevatorController elevator, ClaudeParaphraser paraphraser, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.floorRegistry = floorRegistry;
        this.elevator = elevator;
        this.paraphraser = paraphraser;
        this.logger = logger;
    }

    @EventHandler
    public void onAgentSpawn(AgentSpawnEvent event) {
        BeadsTask task = event.getTask();
        FloorSlot floor = event.getFloor();

        if (agents.containsKey(task.id())) {
            logger.fine("[AgentOffice] Duplicate spawn for task " + task.id() + " — skipping.");
            return;
        }

        if (floor == null) {
            logger.warning("[AgentOffice] No floor assigned for task " + task.id() + " — skipping spawn.");
            return;
        }

        World world = Bukkit.getWorld(config.getOfficeWorld());
        if (world == null) {
            logger.warning("[AgentOffice] Office world not found — cannot spawn agent.");
            return;
        }

        DeskRegistry deskReg = floorDesks.computeIfAbsent(floor.floorNumber(), n -> {
            List<DeskConfig> desks = floor.computeDesks(config.getBuildingX(), config.getBuildingZ(), config.getDesksPerFloor());
            OfficeLayout layout = new OfficeLayout(desks, config);
            return new DeskRegistry(layout);
        });

        Optional<DeskConfig> desk = deskReg.assignDesk(task.id());
        if (desk.isEmpty()) {
            deskReg.enqueue(task.id());
            logger.info("[AgentOffice] No free desk on floor " + floor.floorNumber() + " for " + task.id() + " — queued.");
            return;
        }

        spawnAgentAtDesk(task, desk.get(), floor, world, event.isRestore());
    }

    private void spawnAgentAtDesk(BeadsTask task, DeskConfig desk, FloorSlot floor, World world, boolean isRestore) {
        var elevBase = config.getElevatorPos();
        Location spawnLoc = new Location(world, elevBase.x() + 0.5, elevBase.y(), elevBase.z() + 0.5);

        AgentNpc npc = new AgentNpc(task, spawnLoc);
        agents.put(task.id(), npc);

        Location deskLoc = new Location(world, desk.x() + 0.5, desk.y(), desk.z() + 0.5);

        if (isRestore) {
            npc.teleportTo(deskLoc);
        } else {
            // Ascend to floor's Y level, then move to desk
            int floorEntryY = floor.yBase() + 1;
            elevator.ascend(npc.getEntity(), floorEntryY, () -> npc.teleportTo(deskLoc));
        }

        paraphraser.paraphrase(task.id(), task.title(), task.description())
                .thenAccept(phrase -> Bukkit.getScheduler().runTask(plugin,
                        () -> npc.setLabel(phrase)));
    }

    @EventHandler
    public void onAgentDespawn(AgentDespawnEvent event) {
        String taskId = event.getTaskId();
        AgentNpc npc = agents.remove(taskId);
        if (npc == null) return;

        // Find which floor this task was on to free the desk
        for (Map.Entry<Integer, DeskRegistry> entry : floorDesks.entrySet()) {
            entry.getValue().freeDesk(taskId);
        }

        World world = Bukkit.getWorld(config.getOfficeWorld());
        if (npc.isValid() && world != null) {
            elevator.descend(npc.getEntity(), () -> {
                // After exit, check if floor's queue has something waiting
                floorDesks.values().forEach(dr -> dr.dequeueNext().ifPresent(queuedTaskId ->
                        logger.info("[AgentOffice] Dequeuing task " + queuedTaskId)));
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
                player.sendMessage("§6Agent: §f" + npc.getTaskId());
                player.sendMessage("§7Right-click the team leader for full task details.");
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Removes all NPCs on the given floor (called when a session ends). */
    public void despawnFloor(FloorSlot floor) {
        agents.entrySet().removeIf(entry -> {
            String taskId = entry.getKey();
            // Tasks on this floor are prefixed with the project slug
            if (floor.projectName() != null && taskId.startsWith(floor.projectName() + ":")) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
        floorDesks.remove(floor.floorNumber());
    }

    public Map<String, AgentNpc> getAgents() { return agents; }
    public Map<Integer, DeskRegistry> getFloorDesks() { return floorDesks; }
}
