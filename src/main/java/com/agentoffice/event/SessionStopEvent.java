package com.agentoffice.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a Claude Code session is removed from ~/.claude/office-sessions.json
 * (either explicitly gone or expired past the stale threshold).
 */
public class SessionStopEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String projectPath;
    private final String projectName;

    public SessionStopEvent(String projectPath, String projectName) {
        this.projectPath = projectPath;
        this.projectName = projectName;
    }

    public String getProjectPath() { return projectPath; }
    public String getProjectName() { return projectName; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
