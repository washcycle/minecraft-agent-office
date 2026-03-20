package com.agentoffice.config;

/** Configuration for a single desk position in the office. */
public record DeskConfig(int x, int y, int z, String facing) {
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}
