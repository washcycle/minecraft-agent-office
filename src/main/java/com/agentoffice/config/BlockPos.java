package com.agentoffice.config;

/** Immutable integer block position. */
public record BlockPos(int x, int y, int z) {
    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
