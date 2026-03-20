package com.agentoffice.session;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

/**
 * Builds and manages floor states in the virtual office building.
 *
 * <p>Handles animated floor construction, vacating (dimming) floors by swapping
 * torches to soul torches, and relighting floors when sessions return.
 */
public class FloorConstructor {

    private static final int FLOOR_HEIGHT = 8;

    private final Plugin plugin;
    private final World world;
    private final int buildingX;
    private final int buildingZ;
    private final int floorWidth;
    private final int floorDepth;
    private final Logger logger;

    public FloorConstructor(
            Plugin plugin,
            World world,
            int buildingX,
            int buildingZ,
            int floorWidth,
            int floorDepth,
            Logger logger) {
        this.plugin = plugin;
        this.world = world;
        this.buildingX = buildingX;
        this.buildingZ = buildingZ;
        this.floorWidth = floorWidth;
        this.floorDepth = floorDepth;
        this.logger = logger;
    }

    /**
     * Animates building a new floor over ~20 ticks.
     *
     * <p>Each construction step executes on a separate tick. Calls {@code onComplete}
     * once all steps are finished.
     *
     * @param slot       the floor slot to build
     * @param onComplete callback invoked after the final construction step
     */
    public void buildFloor(FloorSlot slot, Runnable onComplete) {
        int yBase = slot.yBase();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                switch (step) {
                    case 0 -> {
                        // Step 1: Floor slab — fill floorWidth x floorDepth at yBase with OAK_PLANKS
                        for (int x = 0; x < floorWidth; x++) {
                            for (int z = 0; z < floorDepth; z++) {
                                world.getBlockAt(buildingX + x, yBase, buildingZ + z)
                                        .setType(Material.OAK_PLANKS);
                            }
                        }
                        logger.fine("FloorConstructor: placed floor slab at y=" + yBase);
                    }
                    case 1 -> {
                        // Step 2: Walls — 2-block tall perimeter at yBase+1 and yBase+2 with OAK_LOG
                        for (int x = 0; x < floorWidth; x++) {
                            for (int wallY = yBase + 1; wallY <= yBase + 2; wallY++) {
                                world.getBlockAt(buildingX + x, wallY, buildingZ).setType(Material.OAK_LOG);
                                world.getBlockAt(buildingX + x, wallY, buildingZ + floorDepth - 1)
                                        .setType(Material.OAK_LOG);
                            }
                        }
                        for (int z = 1; z < floorDepth - 1; z++) {
                            for (int wallY = yBase + 1; wallY <= yBase + 2; wallY++) {
                                world.getBlockAt(buildingX, wallY, buildingZ + z).setType(Material.OAK_LOG);
                                world.getBlockAt(buildingX + floorWidth - 1, wallY, buildingZ + z)
                                        .setType(Material.OAK_LOG);
                            }
                        }
                        logger.fine("FloorConstructor: placed walls at y=" + (yBase + 1) + " to " + (yBase + 2));
                    }
                    case 2 -> {
                        // Step 3: Ceiling — fill width x depth at yBase+FLOOR_HEIGHT with OAK_PLANKS
                        int ceilingY = yBase + FLOOR_HEIGHT;
                        for (int x = 0; x < floorWidth; x++) {
                            for (int z = 0; z < floorDepth; z++) {
                                world.getBlockAt(buildingX + x, ceilingY, buildingZ + z)
                                        .setType(Material.OAK_PLANKS);
                            }
                        }
                        logger.fine("FloorConstructor: placed ceiling at y=" + ceilingY);
                    }
                    case 3 -> {
                        // Step 4: Torches — 1 per side at midpoint, yBase+2
                        int torchY = yBase + 2;
                        int midX = buildingX + floorWidth / 2;
                        int midZ = buildingZ + floorDepth / 2;

                        // North wall (z = buildingZ), south wall (z = buildingZ + floorDepth - 1)
                        world.getBlockAt(midX, torchY, buildingZ + 1).setType(Material.TORCH);
                        world.getBlockAt(midX, torchY, buildingZ + floorDepth - 2).setType(Material.TORCH);
                        // West wall (x = buildingX), east wall (x = buildingX + floorWidth - 1)
                        world.getBlockAt(buildingX + 1, torchY, midZ).setType(Material.TORCH);
                        world.getBlockAt(buildingX + floorWidth - 2, torchY, midZ).setType(Material.TORCH);
                        logger.fine("FloorConstructor: placed torches at y=" + torchY);
                    }
                    case 4 -> {
                        // Step 5: Clear interior air — set yBase+1 to yBase+6 inside walls to AIR
                        for (int y = yBase + 1; y <= yBase + 6; y++) {
                            for (int x = buildingX + 1; x < buildingX + floorWidth - 1; x++) {
                                for (int z = buildingZ + 1; z < buildingZ + floorDepth - 1; z++) {
                                    Material current = world.getBlockAt(x, y, z).getType();
                                    if (current != Material.TORCH && current != Material.SOUL_TORCH) {
                                        world.getBlockAt(x, y, z).setType(Material.AIR);
                                    }
                                }
                            }
                        }
                        logger.fine("FloorConstructor: cleared interior air at y=" + (yBase + 1) + " to " + (yBase + 6));
                    }
                    case 5 -> {
                        // Step 6: Elevator shaft — keep shaft open from yBase+1 through yBase+FLOOR_HEIGHT-1
                        for (int y = yBase + 1; y <= yBase + FLOOR_HEIGHT - 1; y++) {
                            world.getBlockAt(buildingX, y, buildingZ).setType(Material.AIR);
                        }
                        logger.fine("FloorConstructor: cleared elevator shaft at x=" + buildingX
                                + " z=" + buildingZ);
                    }
                    default -> {
                        // All steps done — call the completion callback and cancel the timer
                        cancel();
                        logger.info("FloorConstructor: floor " + slot.floorNumber() + " construction complete");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        return;
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Dims a floor by replacing all TORCH blocks with SOUL_TORCH.
     *
     * <p>Scans the entire floor volume synchronously.
     *
     * @param slot the floor slot to vacate
     */
    public void vacateFloor(FloorSlot slot) {
        int yBase = slot.yBase();
        int yTop = yBase + FLOOR_HEIGHT;

        for (int y = yBase; y <= yTop; y++) {
            for (int x = buildingX; x < buildingX + floorWidth; x++) {
                for (int z = buildingZ; z < buildingZ + floorDepth; z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.TORCH) {
                        world.getBlockAt(x, y, z).setType(Material.SOUL_TORCH);
                    }
                }
            }
        }
        logger.fine("FloorConstructor: vacated floor " + slot.floorNumber());
    }

    /**
     * Relights a floor by replacing all SOUL_TORCH blocks with TORCH.
     *
     * <p>Scans the entire floor volume synchronously.
     *
     * @param slot the floor slot to relight
     */
    public void relightFloor(FloorSlot slot) {
        int yBase = slot.yBase();
        int yTop = yBase + FLOOR_HEIGHT;

        for (int y = yBase; y <= yTop; y++) {
            for (int x = buildingX; x < buildingX + floorWidth; x++) {
                for (int z = buildingZ; z < buildingZ + floorDepth; z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.SOUL_TORCH) {
                        world.getBlockAt(x, y, z).setType(Material.TORCH);
                    }
                }
            }
        }
        logger.fine("FloorConstructor: relit floor " + slot.floorNumber());
    }

    /**
     * Activates a floor slot for a new session.
     *
     * <p>If the floor already exists (detected by an OAK_PLANKS slab at yBase), the
     * floor is relit immediately and {@code onComplete} is called. Otherwise a full
     * animated build is triggered via {@link #buildFloor(FloorSlot, Runnable)}.
     *
     * @param slot       the floor slot to activate
     * @param onComplete callback invoked once the floor is ready
     */
    public void activateFloor(FloorSlot slot, Runnable onComplete) {
        Material probe = world.getBlockAt(buildingX + 1, slot.yBase(), buildingZ + 1).getType();
        if (probe == Material.OAK_PLANKS) {
            // Floor already exists — relight and complete immediately
            logger.fine("FloorConstructor: reusing existing floor " + slot.floorNumber());
            relightFloor(slot);
            if (onComplete != null) {
                onComplete.run();
            }
        } else {
            // Floor is new — animate the build
            logger.fine("FloorConstructor: building new floor " + slot.floorNumber());
            buildFloor(slot, onComplete);
        }
    }
}
