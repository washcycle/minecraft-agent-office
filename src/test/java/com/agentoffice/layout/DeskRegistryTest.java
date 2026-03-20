package com.agentoffice.layout;

import com.agentoffice.config.BlockPos;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.config.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeskRegistryTest {

    @Mock
    private PluginConfig config;

    private OfficeLayout layout;
    private DeskRegistry registry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getElevatorPos()).thenReturn(new BlockPos(0, 64, 0));
        when(config.getElevatorHeight()).thenReturn(4);
        when(config.getDesks()).thenReturn(List.of(
                new DeskConfig(5, 64, 5, "NORTH"),
                new DeskConfig(5, 64, 8, "NORTH"),
                new DeskConfig(8, 64, 5, "NORTH")
        ));
        layout = new OfficeLayout(config);
        registry = new DeskRegistry(layout);
    }

    @Test
    void assignsNearestFreeDesk() {
        Optional<DeskConfig> assigned = registry.assignDesk("task-1");
        assertTrue(assigned.isPresent());
        // Nearest to elevator top (0, 68, 0) should be desk at (5,64,5) — closest distance
        DeskConfig desk = assigned.get();
        assertEquals(5, desk.x());
        assertEquals(5, desk.z());
    }

    @Test
    void preventsDoubleOccupancy() {
        registry.assignDesk("task-1");
        registry.assignDesk("task-2");
        registry.assignDesk("task-3");

        // All 3 desks occupied — next should be empty
        Optional<DeskConfig> overflow = registry.assignDesk("task-4");
        assertTrue(overflow.isEmpty());
    }

    @Test
    void freeDeskMakesItAvailableAgain() {
        registry.assignDesk("task-1");
        registry.assignDesk("task-2");
        registry.assignDesk("task-3");

        registry.freeDesk("task-2");

        Optional<DeskConfig> reassigned = registry.assignDesk("task-4");
        assertTrue(reassigned.isPresent());
    }

    @Test
    void queueEnqueueAndDequeue() {
        registry.enqueue("task-A");
        registry.enqueue("task-B");

        Optional<String> first = registry.dequeueNext();
        assertTrue(first.isPresent());
        assertEquals("task-A", first.get());

        Optional<String> second = registry.dequeueNext();
        assertEquals("task-B", second.get());

        Optional<String> third = registry.dequeueNext();
        assertTrue(third.isEmpty());
    }

    @Test
    void freeCountReflectsOccupancy() {
        assertEquals(3, registry.freeCount());
        registry.assignDesk("task-1");
        assertEquals(2, registry.freeCount());
        registry.freeDesk("task-1");
        assertEquals(3, registry.freeCount());
    }
}
