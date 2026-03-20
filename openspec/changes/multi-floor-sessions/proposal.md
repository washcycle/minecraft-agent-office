## Why

The current plugin treats all beads tasks as a single flat office floor. But the user runs multiple Claude Code sessions simultaneously — one per repo — and wants each session to feel like its own team occupying its own space. A single floor doesn't communicate which work belongs to which project.

Additionally, the plugin currently has no way to detect running Claude Code sessions at all. It only polls beads task status. This means sessions that haven't claimed a beads task are invisible, and there's no concept of a "session arriving" separate from "a task going in_progress".

## What Changes

- A **hook script** is installed globally into `~/.claude/settings.json`. It fires on `SessionStart` and `Stop`, writing to a shared JSON file (`~/.claude/office-sessions.json`) that the plugin polls.
- The plugin gains a **SessionPoller** that reads the sessions file and fires `SessionStartEvent` / `SessionStopEvent` Bukkit events.
- The single static office floor is replaced by a **FloorRegistry**: a dynamic allocator that assigns each active session to a floor. Floors are built in Minecraft (block-by-block scaffold animation) when needed, vacated when sessions end, and reused by new sessions.
- Each floor's desks are populated by polling that project's beads tasks (via `bd list --status=in_progress` run from the project directory).
- The lobby (ground floor) hosts the team leader NPC, which now broadcasts cross-floor status: total sessions, total active tasks.

## Capabilities

### New Capabilities
- `session-detection`: Hook script + SessionPoller that turns Claude Code lifecycle events into Bukkit events the plugin can react to
- `floor-registry`: Dynamic floor allocator — assign, vacate, reuse floors as sessions come and go
- `floor-construction`: Animate building a new floor in Minecraft (place blocks over ~1 second) when the high-water mark is exceeded

### Modified Capabilities
- `beads-poller`: Must now poll per project directory (one `bd list` per active session's project path) rather than from a single working directory
- `office-layout`: Desk configs are now per-floor, derived from `floor.desks-per-floor` config rather than a flat global list
- `agent-npc`: NPC spawn location now uses the floor's Y offset, not a global desk list
- `elevator`: Must route NPCs to the correct floor Y level, not always to a fixed top position

## Impact

- New: `hooks/claude-office-hook` Python script (installed by user into global Claude settings)
- New: `SessionPoller.java`, `SessionStartEvent.java`, `SessionStopEvent.java`, `FloorRegistry.java`, `FloorConstructor.java`
- Modified: `BeadsPoller.java` — takes project path per session
- Modified: `OfficeLayout.java` / `DeskRegistry.java` — scoped per floor
- Modified: `AgentRegistry.java` — receives floor Y offset when spawning NPCs
- Modified: `ElevatorController.java` — target floor Y is dynamic
- Modified: `config.yml` — replace flat `desks` list with `floor.height`, `floor.desks-per-floor`, `floor.width`; add `sessions-file` path
- New dependency: none (Python stdlib only for hook script)
