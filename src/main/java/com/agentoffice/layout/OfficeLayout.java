package com.agentoffice.layout;

import com.agentoffice.config.BlockPos;
import com.agentoffice.config.DeskConfig;
import com.agentoffice.config.PluginConfig;

import java.util.List;

/**
 * Derived geometry for the office world: desk positions and elevator shaft extents.
 */
public class OfficeLayout {

    private final PluginConfig config;
    private final List<DeskConfig> overrideDesks;

    public OfficeLayout(PluginConfig config) {
        this.config = config;
        this.overrideDesks = null;
    }

    /** Constructor for per-floor use: provide a pre-computed desk list. */
    public OfficeLayout(List<DeskConfig> desks, PluginConfig config) {
        this.config = config;
        this.overrideDesks = desks;
    }

    public BlockPos getElevatorBase() {
        return config.getElevatorPos();
    }

    /** Top of the elevator shaft — base Y + height. */
    public BlockPos getElevatorTop() {
        BlockPos base = config.getElevatorPos();
        return new BlockPos(base.x(), base.y() + config.getElevatorHeight(), base.z());
    }

    /** Lobby position — one block in front of elevator top (Z+1). */
    public BlockPos getElevatorLobby() {
        BlockPos top = getElevatorTop();
        return new BlockPos(top.x(), top.y(), top.z() + 1);
    }

    public List<DeskConfig> getDesks() {
        return overrideDesks != null ? overrideDesks : config.getDesks();
    }

    public int getDeskCount() {
        return getDesks().size();
    }
}
