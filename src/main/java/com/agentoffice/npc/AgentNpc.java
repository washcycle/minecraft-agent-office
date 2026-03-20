package com.agentoffice.npc;

import com.agentoffice.beads.BeadsTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

/**
 * Wraps an ArmorStand entity to represent one beads agent in the office.
 * The NPC displays the agent's paraphrased task as a custom name tag.
 */
public class AgentNpc {

    private final String taskId;
    private ArmorStand entity;
    private String assignee;

    public AgentNpc(BeadsTask task, Location spawnLocation) {
        this.taskId = task.id();
        this.assignee = task.assignee() != null ? task.assignee() : "Agent";

        World world = spawnLocation.getWorld();
        entity = (ArmorStand) world.spawnEntity(spawnLocation, EntityType.ARMOR_STAND);

        entity.setVisible(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSmall(false);
        entity.setCustomNameVisible(true);
        entity.setCustomName(formatName(task.title()));  // raw title until paraphrase resolves
        entity.setBasePlate(false);
        entity.setArms(false);
    }

    /** Updates the floating name tag (called from main thread after paraphrase resolves). */
    public void setLabel(String paraphrase) {
        if (entity != null && entity.isValid()) {
            entity.setCustomName("[" + assignee + "] " + paraphrase);
        }
    }

    /** Teleports the entity to a new location (used by elevator + desk assignment). */
    public void teleportTo(Location location) {
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
        }
    }

    public Location getLocation() {
        return entity != null ? entity.getLocation() : null;
    }

    public ArmorStand getEntity() {
        return entity;
    }

    public String getTaskId() {
        return taskId;
    }

    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    /** Removes the entity from the world immediately (no animation). */
    public void remove() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

    private String formatName(String title) {
        return "[" + assignee + "] " + title;
    }
}
