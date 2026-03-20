package com.agentoffice.event;

import com.agentoffice.beads.BeadsTask;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a beads task transitions to in_progress and an agent NPC should be spawned.
 * isRestore=true means the server restarted and the NPC should skip the elevator entry animation.
 */
public class AgentSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BeadsTask task;
    private final boolean isRestore;

    public AgentSpawnEvent(BeadsTask task, boolean isRestore) {
        this.task = task;
        this.isRestore = isRestore;
    }

    public BeadsTask getTask() { return task; }
    public boolean isRestore() { return isRestore; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
