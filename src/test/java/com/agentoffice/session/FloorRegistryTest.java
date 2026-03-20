package com.agentoffice.session;

import com.agentoffice.session.FloorSlot.FloorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class FloorRegistryTest {

    // lobbyY=64, floorHeight=8, buildingX=0, buildingZ=0, desksPerFloor=6, maxFloors=8
    private FloorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FloorRegistry(64, 8, 0, 0, 6, 8, Logger.getLogger("test"));
    }

    @Test
    void assignsFirstProjectToFloor1() {
        Optional<FloorSlot> result = registry.assign("/projects/alpha", "alpha");

        assertTrue(result.isPresent());
        FloorSlot slot = result.get();
        assertEquals(1, slot.floorNumber());
        assertEquals(72, slot.yBase()); // 64 + 8*1
        assertEquals("alpha", slot.projectName());
        assertEquals(FloorState.OCCUPIED, slot.state());
    }

    @Test
    void deduplicatesSameProject() {
        Optional<FloorSlot> first  = registry.assign("/projects/alpha", "alpha");
        Optional<FloorSlot> second = registry.assign("/projects/alpha", "alpha");

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get().floorNumber(), second.get().floorNumber());

        // Only one slot should exist in total
        assertEquals(1, registry.getAllSlots().size());
    }

    @Test
    void assignsLowestVacantSlot() {
        registry.assign("/projects/alpha", "alpha");
        registry.assign("/projects/beta",  "beta");
        registry.assign("/projects/gamma", "gamma");

        // Vacate the middle slot (floor 2)
        registry.vacate("/projects/beta");

        // A 4th project should reuse floor 2, not create floor 4
        Optional<FloorSlot> fourth = registry.assign("/projects/delta", "delta");

        assertTrue(fourth.isPresent());
        assertEquals(2, fourth.get().floorNumber());
    }

    @Test
    void enforcesMaxFloors() {
        for (int i = 1; i <= 8; i++) {
            Optional<FloorSlot> slot = registry.assign("/projects/p" + i, "p" + i);
            assertTrue(slot.isPresent(), "Expected floor " + i + " to be assigned");
        }

        // 9th assignment should return empty because cap is 8
        Optional<FloorSlot> overflow = registry.assign("/projects/p9", "p9");
        assertTrue(overflow.isEmpty());
    }

    @Test
    void vacateMarksSlotVacant() {
        registry.assign("/projects/alpha", "alpha");
        registry.vacate("/projects/alpha");

        List<FloorSlot> all = registry.getAllSlots();
        assertEquals(1, all.size());

        FloorSlot slot = all.get(0);
        assertEquals(FloorState.VACANT, slot.state());
        assertNull(slot.projectPath());
    }

    @Test
    void getOccupiedSlotsReturnsOnlyOccupied() {
        registry.assign("/projects/alpha", "alpha");
        registry.assign("/projects/beta",  "beta");
        registry.vacate("/projects/alpha");

        List<FloorSlot> occupied = registry.getOccupiedSlots();
        assertEquals(1, occupied.size());
        assertEquals("beta", occupied.get(0).projectName());
    }

    @Test
    void yBaseComputedCorrectly() {
        Optional<FloorSlot> floor1 = registry.assign("/projects/alpha", "alpha");
        Optional<FloorSlot> floor2 = registry.assign("/projects/beta",  "beta");

        assertTrue(floor1.isPresent());
        assertTrue(floor2.isPresent());

        // floor 1: lobbyY + floorHeight*1 = 64 + 8 = 72
        assertEquals(72, floor1.get().yBase());
        // floor 2: lobbyY + floorHeight*2 = 64 + 16 = 80
        assertEquals(80, floor2.get().yBase());
    }
}
