package com.agentoffice.beads;

/** Immutable snapshot of a beads issue. */
public record BeadsTask(
        String id,
        String title,
        String description,
        String assignee,   // may be null
        String status
) {}
