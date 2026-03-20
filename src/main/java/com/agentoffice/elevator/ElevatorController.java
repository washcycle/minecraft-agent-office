package com.agentoffice.elevator;

import com.agentoffice.layout.OfficeLayout;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles elevator ascent/descent animations and door (trapdoor) state.
 * NPCs are teleported 1 block per tick for the height of the shaft.
 */
public class ElevatorController {

    private final Plugin plugin;
    private final OfficeLayout layout;

    public ElevatorController(Plugin plugin, OfficeLayout layout) {
        this.plugin = plugin;
        this.layout = layout;
    }

    /**
     * Teleports the ArmorStand from elevator base to the given targetY, 1 block/tick.
     * Opens the door on arrival, then calls onComplete on the main thread.
     */
    public void ascend(ArmorStand entity, int targetY, Runnable onComplete) {
        int height = targetY - layout.getElevatorBase().y();
        World world = entity.getWorld();
        double startX = layout.getElevatorBase().x() + 0.5;
        double startY = layout.getElevatorBase().y();
        double startZ = layout.getElevatorBase().z() + 0.5;

        // Snap to elevator base
        entity.teleport(world.getBlockAt(
                layout.getElevatorBase().x(),
                layout.getElevatorBase().y(),
                layout.getElevatorBase().z()).getLocation().add(0.5, 0, 0.5));
        // height computed above using targetY

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    return;
                }
                tick++;
                entity.teleport(entity.getLocation().add(0, 1, 0));
                if (tick >= height) {
                    cancel();
                    setDoorOpen(true);
                    onComplete.run();
                }
            }
        }.runTaskTimer(plugin, 2L, 1L);
    }

    /**
     * Teleports the ArmorStand from its current position down to elevator base, 1 block/tick.
     * Closes the door at the start, despawns entity at the bottom, calls onComplete.
     */
    public void descend(ArmorStand entity, Runnable onComplete) {
        int height = layout.getElevatorTop().y() - layout.getElevatorBase().y();

        // Move entity to elevator top position first
        World world = entity.getWorld();
        entity.teleport(world.getBlockAt(
                layout.getElevatorTop().x(),
                layout.getElevatorTop().y(),
                layout.getElevatorTop().z()).getLocation().add(0.5, 0, 0.5));

        setDoorOpen(false);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    return;
                }
                tick++;
                entity.teleport(entity.getLocation().subtract(0, 1, 0));
                if (tick >= height) {
                    cancel();
                    entity.remove();
                    onComplete.run();
                }
            }
        }.runTaskTimer(plugin, 2L, 1L);
    }

    /**
     * Toggles the trapdoor blocks at the top of the elevator shaft open or closed.
     * Looks for trapdoor blocks at elevatorTop Y in a 3x3 area.
     */
    public void setDoorOpen(boolean open) {
        World world = plugin.getServer().getWorld(
                plugin.getConfig().getString("office-world", "world"));
        if (world == null) return;

        int x = layout.getElevatorTop().x();
        int y = layout.getElevatorTop().y();
        int z = layout.getElevatorTop().z();

        // Check the two trapdoor blocks flanking the elevator
        for (int dx = -1; dx <= 1; dx++) {
            Block block = world.getBlockAt(x + dx, y, z);
            if (block.getBlockData() instanceof Openable openable) {
                openable.setOpen(open);
                block.setBlockData(openable);
            }
        }
    }
}
