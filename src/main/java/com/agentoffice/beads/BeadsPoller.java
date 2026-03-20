package com.agentoffice.beads;

import com.agentoffice.event.AgentDespawnEvent;
import com.agentoffice.event.AgentSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Background runnable that polls beads every N ticks and fires
 * AgentSpawnEvent / AgentDespawnEvent for task state deltas.
 */
public class BeadsPoller extends BukkitRunnable {

    private final BeadsClient client;
    private final Plugin plugin;
    private final Logger logger;

    /** Previous snapshot: taskId → task */
    private final Map<String, BeadsTask> previous = new HashMap<>();

    public BeadsPoller(BeadsClient client, Plugin plugin, Logger logger) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void run() {
        List<BeadsTask> current;
        try {
            current = client.listInProgress();
        } catch (BeadsException e) {
            logger.warning("[AgentOffice] Beads poll failed — skipping cycle: " + e.getMessage());
            return;
        }

        Map<String, BeadsTask> currentMap = new HashMap<>();
        for (BeadsTask t : current) currentMap.put(t.id(), t);

        // New tasks → spawn
        for (BeadsTask task : current) {
            if (!previous.containsKey(task.id())) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.getPluginManager().callEvent(new AgentSpawnEvent(task, false)));
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
     * Called on plugin enable to immediately restore NPCs for all currently in-progress tasks
     * without waiting for the first poll interval. Sets the initial snapshot so the first
     * regular poll won't re-fire spawn events.
     */
    public void startupRestore() {
        List<BeadsTask> current;
        try {
            current = client.listInProgress();
        } catch (BeadsException e) {
            logger.warning("[AgentOffice] Startup restore poll failed: " + e.getMessage());
            return;
        }

        for (BeadsTask task : current) {
            previous.put(task.id(), task);
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.getPluginManager().callEvent(new AgentSpawnEvent(task, true)));
        }

        logger.info("[AgentOffice] Startup restore: " + current.size() + " active task(s) found.");
    }
}
