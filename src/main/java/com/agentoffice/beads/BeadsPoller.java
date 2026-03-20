package com.agentoffice.beads;

import com.agentoffice.event.AgentDespawnEvent;
import com.agentoffice.event.AgentSpawnEvent;
import com.agentoffice.session.FloorRegistry;
import com.agentoffice.session.FloorSlot;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Background runnable that polls beads for each active floor's project directory
 * and fires AgentSpawnEvent / AgentDespawnEvent for task state deltas.
 *
 * Task IDs are prefixed with the project slug (e.g. "armada-vscode:armada-42")
 * to ensure uniqueness across multiple projects.
 */
public class BeadsPoller extends BukkitRunnable {

    private final BeadsCliClient client;
    private final FloorRegistry floorRegistry;
    private final Plugin plugin;
    private final Logger logger;

    /** Previous snapshot: prefixed taskId → task */
    private final Map<String, BeadsTask> previous = new HashMap<>();

    public BeadsPoller(BeadsCliClient client, FloorRegistry floorRegistry, Plugin plugin, Logger logger) {
        this.client = client;
        this.floorRegistry = floorRegistry;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void run() {
        List<FloorSlot> occupied = floorRegistry.getOccupiedSlots();
        if (occupied.isEmpty()) return;

        List<BeadsTask> current = new ArrayList<>();
        for (FloorSlot slot : occupied) {
            File projectDir = new File(slot.projectPath());
            current.addAll(client.listInProgressFrom(projectDir, slot.projectName()));
        }

        Map<String, BeadsTask> currentMap = new HashMap<>();
        for (BeadsTask t : current) currentMap.put(t.id(), t);

        // New tasks → spawn
        for (BeadsTask task : current) {
            if (!previous.containsKey(task.id())) {
                // Find the floor for this task (by slug prefix)
                String slug = task.id().contains(":") ? task.id().split(":")[0] : "";
                FloorSlot floor = occupied.stream()
                        .filter(s -> s.projectName().equals(slug))
                        .findFirst().orElse(null);
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.getPluginManager().callEvent(new AgentSpawnEvent(task, false, floor)));
            }
        }

        // Gone tasks → despawn
        for (String id : previous.keySet()) {
            if (!currentMap.containsKey(id)) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.getPluginManager().callEvent(new AgentDespawnEvent(id)));
            }
        }

        previous.clear();
        previous.putAll(currentMap);
    }

    /**
     * Called on plugin enable to immediately restore NPCs for all currently in-progress tasks.
     * Requires floors to already be assigned (call after SessionPoller has seeded the registry).
     */
    public void startupRestore() {
        List<FloorSlot> occupied = floorRegistry.getOccupiedSlots();
        if (occupied.isEmpty()) {
            logger.info("[AgentOffice] Startup restore: no active sessions.");
            return;
        }

        List<BeadsTask> current = new ArrayList<>();
        for (FloorSlot slot : occupied) {
            File projectDir = new File(slot.projectPath());
            List<BeadsTask> tasks = client.listInProgressFrom(projectDir, slot.projectName());
            // Store for snapshot so first regular poll doesn't re-fire
            for (BeadsTask t : tasks) previous.put(t.id(), t);
            final FloorSlot floorSlot = slot;
            for (BeadsTask task : tasks) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.getPluginManager().callEvent(new AgentSpawnEvent(task, true, floorSlot)));
            }
            current.addAll(tasks);
        }

        logger.info("[AgentOffice] Startup restore: " + current.size() + " active task(s) across "
                + occupied.size() + " floor(s).");
    }
}
