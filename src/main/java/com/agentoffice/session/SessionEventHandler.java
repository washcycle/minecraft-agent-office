package com.agentoffice.session;

import com.agentoffice.event.SessionStartEvent;
import com.agentoffice.event.SessionStopEvent;
import com.agentoffice.npc.AgentRegistry;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

/**
 * Listens for SessionStartEvent and SessionStopEvent and coordinates:
 *   Start → FloorRegistry.assign() → FloorConstructor.activateFloor()
 *   Stop  → AgentRegistry.despawnFloor() → FloorConstructor.vacateFloor() → FloorRegistry.vacate()
 */
public class SessionEventHandler implements Listener {

    private final FloorRegistry floorRegistry;
    private final FloorConstructor floorConstructor;
    private final AgentRegistry agentRegistry;
    private final World world;
    private final Logger logger;

    public SessionEventHandler(FloorRegistry floorRegistry, FloorConstructor floorConstructor,
                                AgentRegistry agentRegistry, World world, Logger logger) {
        this.floorRegistry = floorRegistry;
        this.floorConstructor = floorConstructor;
        this.agentRegistry = agentRegistry;
        this.world = world;
        this.logger = logger;
    }

    @EventHandler
    public void onSessionStart(SessionStartEvent event) {
        String projectPath = event.getProjectPath();
        String projectName = event.getProjectName();

        floorRegistry.assign(projectPath, projectName).ifPresentOrElse(
                slot -> {
                    logger.info("[AgentOffice] Session started: " + projectName + " → floor " + slot.floorNumber());
                    floorConstructor.activateFloor(slot, () ->
                            logger.fine("[AgentOffice] Floor " + slot.floorNumber() + " activated for " + projectName));
                },
                () -> logger.warning("[AgentOffice] No floor available for session: " + projectName)
        );
    }

    @EventHandler
    public void onSessionStop(SessionStopEvent event) {
        String projectPath = event.getProjectPath();
        String projectName = event.getProjectName();

        floorRegistry.getSlotForProject(projectPath).ifPresent(slot -> {
            logger.info("[AgentOffice] Session ended: " + projectName + " — vacating floor " + slot.floorNumber());
            agentRegistry.despawnFloor(slot);
            floorConstructor.vacateFloor(slot);
            floorRegistry.vacate(projectPath);
        });
    }
}
