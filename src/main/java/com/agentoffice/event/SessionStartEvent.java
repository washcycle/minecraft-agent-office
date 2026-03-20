package com.agentoffice.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a new Claude Code session appears in ~/.claude/office-sessions.json.
 */
public class SessionStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String projectPath;
    private final String projectName;

    public SessionStartEvent(String projectPath, String projectName) {
        this.projectPath = projectPath;
        this.projectName = projectName;
    }

    public String getProjectPath() { return projectPath; }
    public String getProjectName() { return projectName; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
