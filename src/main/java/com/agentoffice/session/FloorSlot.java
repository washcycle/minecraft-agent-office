package com.agentoffice.session;

import com.agentoffice.config.DeskConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single floor slot in the virtual office building.
 *
 * <p>floorNumber is 1-based (lobby = 0). yBase is the Y coordinate of the
 * floor slab. projectPath and projectName are null when the slot is VACANT.
 */
public record FloorSlot(
        int floorNumber,
        int yBase,
        String projectPath,
        String projectName,
        FloorState state) {

    /** Lifecycle state of a floor slot. */
    public enum FloorState {
        VACANT,
        OCCUPIED
    }

    /**
     * Arranges {@code desksPerFloor} desks in a grid at {@code y = yBase + 1}.
     *
     * <p>Desks start at ({@code buildingX + 2}, {@code buildingZ + 2}) and are
     * spaced 2 blocks apart. Rows are filled with 3 desks each; the next row
     * begins after every 3 desks. All desks face south ("south").
     *
     * @param buildingX    X origin of the building
     * @param buildingZ    Z origin of the building
     * @param desksPerFloor number of desks to generate
     * @return ordered list of desk configurations
     */
    public List<DeskConfig> computeDesks(int buildingX, int buildingZ, int desksPerFloor) {
        List<DeskConfig> desks = new ArrayList<>(desksPerFloor);
        int deskY = yBase + 1;
        int desksPerRow = 3;
        int spacing = 2;

        for (int i = 0; i < desksPerFloor; i++) {
            int col = i % desksPerRow;
            int row = i / desksPerRow;
            int deskX = buildingX + 2 + col * spacing;
            int deskZ = buildingZ + 2 + row * spacing;
            desks.add(new DeskConfig(deskX, deskY, deskZ, "south"));
        }

        return desks;
    }
}
