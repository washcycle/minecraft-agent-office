package com.agentoffice.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a beads task is no longer in_progress and its agent NPC should be removed.
 */
public class AgentDespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String taskId;

    public AgentDespawnEvent(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() { return taskId; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
