package com.agentoffice.layout;

import com.agentoffice.config.BlockPos;
import com.agentoffice.config.DeskConfig;

import java.util.*;

/**
 * Thread-safe registry tracking desk occupancy and a waiting queue for overflow agents.
 */
public class DeskRegistry {

    private final OfficeLayout layout;

    /** deskIndex → taskId (null = free) */
    private final String[] occupied;

    /** FIFO queue of task IDs waiting for a free desk */
    private final Queue<String> waitQueue = new LinkedList<>();

    public DeskRegistry(OfficeLayout layout) {
        this.layout = layout;
        this.occupied = new String[layout.getDeskCount()];
    }

    /**
     * Assigns the nearest free desk (by Euclidean distance to elevator top) to the task.
     * Returns the desk, or empty if all desks are occupied.
     */
    public synchronized Optional<DeskConfig> assignDesk(String taskId) {
        BlockPos elevator = layout.getElevatorTop();
        List<DeskConfig> desks = layout.getDesks();

        int nearest = -1;
        double nearestDist = Double.MAX_VALUE;

        for (int i = 0; i < desks.size(); i++) {
            if (occupied[i] == null) {
                DeskConfig d = desks.get(i);
                double dist = distance(elevator, d.x(), d.y(), d.z());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = i;
                }
            }
        }

        if (nearest == -1) return Optional.empty();

        occupied[nearest] = taskId;
        return Optional.of(desks.get(nearest));
    }

    /** Frees the desk held by this task. */
    public synchronized void freeDesk(String taskId) {
        for (int i = 0; i < occupied.length; i++) {
            if (taskId.equals(occupied[i])) {
                occupied[i] = null;
                return;
            }
        }
    }

    /** Enqueues a task to wait for a free desk. */
    public synchronized void enqueue(String taskId) {
        if (!waitQueue.contains(taskId)) {
            waitQueue.add(taskId);
        }
    }

    /** Polls the next waiting task, or empty if queue is empty. */
    public synchronized Optional<String> dequeueNext() {
        return Optional.ofNullable(waitQueue.poll());
    }

    public synchronized boolean isOccupied(int deskIndex) {
        return occupied[deskIndex] != null;
    }

    public synchronized int freeCount() {
        int count = 0;
        for (String s : occupied) if (s == null) count++;
        return count;
    }

    private double distance(BlockPos from, int x, int y, int z) {
        int dx = from.x() - x, dy = from.y() - y, dz = from.z() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
