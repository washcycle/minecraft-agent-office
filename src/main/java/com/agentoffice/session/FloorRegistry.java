package com.agentoffice.session;

import com.agentoffice.session.FloorSlot.FloorState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manages the ordered list of {@link FloorSlot}s for the virtual office building.
 *
 * <p>Floors are numbered 1-based; floor 1 sits immediately above the lobby.
 * The Y base for floor N is {@code lobbyY + floorHeight * N}.
 *
 * <p>All public methods are thread-safe.
 */
public class FloorRegistry {

    private final int lobbyY;
    private final int floorHeight;
    private final int buildingX;
    private final int buildingZ;
    private final int desksPerFloor;
    private final int maxFloors;
    private final Logger logger;

    /** Mutable slot list; guarded by {@code this}. */
    private final List<FloorSlot> slots = new ArrayList<>();

    /**
     * Creates a new registry.
     *
     * @param lobbyY       Y coordinate of the lobby (ground) floor slab
     * @param floorHeight  blocks per floor including the slab (e.g. 8)
     * @param buildingX    X origin used for desk layout
     * @param buildingZ    Z origin used for desk layout
     * @param desksPerFloor desks to place on each floor
     * @param maxFloors    maximum number of floors (hard cap, e.g. 8)
     * @param logger       plugin logger
     */
    public FloorRegistry(
            int lobbyY,
            int floorHeight,
            int buildingX,
            int buildingZ,
            int desksPerFloor,
            int maxFloors,
            Logger logger) {
        this.lobbyY = lobbyY;
        this.floorHeight = floorHeight;
        this.buildingX = buildingX;
        this.buildingZ = buildingZ;
        this.desksPerFloor = desksPerFloor;
        this.maxFloors = maxFloors;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Assigns a floor slot to the given project.
     *
     * <ul>
     *   <li>If the project already holds a slot, that slot is returned (deduplication).</li>
     *   <li>Otherwise the lowest VACANT slot is claimed.</li>
     *   <li>If no VACANT slot exists and the total slot count is below {@code maxFloors},
     *       a new slot is created above the current high-water mark.</li>
     *   <li>Returns {@link Optional#empty()} if the cap is reached.</li>
     * </ul>
     *
     * @param projectPath absolute path of the project
     * @param projectName display name (basename) of the project
     * @return the assigned slot, or empty if the cap is reached
     */
    public synchronized Optional<FloorSlot> assign(String projectPath, String projectName) {
        // Deduplication: return existing slot for this project.
        for (int i = 0; i < slots.size(); i++) {
            FloorSlot s = slots.get(i);
            if (s.state() == FloorState.OCCUPIED && projectPath.equals(s.projectPath())) {
                logger.fine("[FloorRegistry] Project already assigned to floor " + s.floorNumber());
                return Optional.of(s);
            }
        }

        // Claim the lowest VACANT slot.
        for (int i = 0; i < slots.size(); i++) {
            FloorSlot s = slots.get(i);
            if (s.state() == FloorState.VACANT) {
                FloorSlot occupied = new FloorSlot(
                        s.floorNumber(), s.yBase(), projectPath, projectName, FloorState.OCCUPIED);
                slots.set(i, occupied);
                logger.info("[FloorRegistry] Assigned floor " + occupied.floorNumber()
                        + " to project " + projectName);
                return Optional.of(occupied);
            }
        }

        // No VACANT slot — create a new one if under cap.
        if (slots.size() >= maxFloors) {
            logger.warning("[FloorRegistry] Floor cap (" + maxFloors + ") reached; cannot assign floor for " + projectName);
            return Optional.empty();
        }

        int newFloorNumber = slots.size() + 1;
        int newYBase = lobbyY + floorHeight * newFloorNumber;
        FloorSlot newSlot = new FloorSlot(
                newFloorNumber, newYBase, projectPath, projectName, FloorState.OCCUPIED);
        slots.add(newSlot);
        logger.info("[FloorRegistry] Created and assigned floor " + newFloorNumber
                + " (yBase=" + newYBase + ") to project " + projectName);
        return Optional.of(newSlot);
    }

    /**
     * Marks the slot assigned to {@code projectPath} as VACANT.
     *
     * <p>Does nothing if the project has no assigned slot.
     *
     * @param projectPath absolute path of the project to vacate
     */
    public synchronized void vacate(String projectPath) {
        for (int i = 0; i < slots.size(); i++) {
            FloorSlot s = slots.get(i);
            if (s.state() == FloorState.OCCUPIED && projectPath.equals(s.projectPath())) {
                FloorSlot vacant = new FloorSlot(
                        s.floorNumber(), s.yBase(), null, null, FloorState.VACANT);
                slots.set(i, vacant);
                logger.info("[FloorRegistry] Vacated floor " + s.floorNumber()
                        + " (was " + s.projectName() + ")");
                return;
            }
        }
        logger.fine("[FloorRegistry] vacate() called for unknown project: " + projectPath);
    }

    /**
     * Returns all OCCUPIED slots in floor-number order.
     *
     * @return unmodifiable snapshot of occupied slots
     */
    public synchronized List<FloorSlot> getOccupiedSlots() {
        List<FloorSlot> result = new ArrayList<>();
        for (FloorSlot s : slots) {
            if (s.state() == FloorState.OCCUPIED) {
                result.add(s);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all slots (VACANT and OCCUPIED) in floor-number order.
     *
     * @return unmodifiable snapshot of all slots
     */
    public synchronized List<FloorSlot> getAllSlots() {
        return Collections.unmodifiableList(new ArrayList<>(slots));
    }

    /**
     * Looks up the slot currently assigned to the given project path.
     *
     * @param projectPath absolute path of the project
     * @return the occupied slot, or empty if not found
     */
    public synchronized Optional<FloorSlot> getSlotForProject(String projectPath) {
        for (FloorSlot s : slots) {
            if (s.state() == FloorState.OCCUPIED && projectPath.equals(s.projectPath())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
