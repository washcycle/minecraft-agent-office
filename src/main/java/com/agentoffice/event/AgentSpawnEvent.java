package com.agentoffice.event;

import com.agentoffice.beads.BeadsTask;
import com.agentoffice.session.FloorSlot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a beads task transitions to in_progress and an agent NPC should be spawned.
 * isRestore=true means the server restarted and the NPC should skip the elevator entry animation.
 * floor is the FloorSlot the NPC should occupy (null if no floor assigned).
 */
public class AgentSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BeadsTask task;
    private final boolean isRestore;
    private final FloorSlot floor;

    public AgentSpawnEvent(BeadsTask task, boolean isRestore, @Nullable FloorSlot floor) {
        this.task = task;
        this.isRestore = isRestore;
        this.floor = floor;
    }

    public BeadsTask getTask() { return task; }
    public boolean isRestore() { return isRestore; }
    @Nullable public FloorSlot getFloor() { return floor; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
