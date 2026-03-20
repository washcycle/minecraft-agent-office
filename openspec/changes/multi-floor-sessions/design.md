## Context

The plugin currently has one flat office with a fixed desk grid. All NPCs share one floor. Beads tasks are polled from a single configured directory.

Claude Code sessions are started as terminal processes (`claude` in a VS Code terminal). Each runs in a repo directory. The user may have 2–5 sessions active simultaneously across different repos.

Claude Code exposes a hooks system in `~/.claude/settings.json` — shell commands fired on lifecycle events with JSON on stdin. This is the authoritative real-time signal for session start/stop.

## Goals / Non-Goals

**Goals:**
- One floor per active Claude Code session (project-scoped)
- Floors built dynamically, vacated and reused as sessions come and go
- Hook script is simple to install and silent (never disrupts Claude)
- Beads tasks for each session's project populate that floor's desks
- Elevator routes to the correct floor

**Non-Goals:**
- Supporting non-Claude agents or non-beads task systems
- Animated floor demolition (vacated floors stay as structures, just dim)
- Multiple simultaneous sessions in the same repo getting separate floors (same project = same floor, sessions merged)
- Building a custom Minecraft structure — floors are simple flat platforms with desks

## Decisions

### D1: Floor identity = project path, not session ID
Two terminal windows in the same repo share one floor. Deduplication is by `cwd` (canonical path). This avoids two identical NPC sets for the same work, and keeps the floor count bounded by repos, not window count.

**Consequence:** `office-sessions.json` maps multiple session IDs to one project; FloorRegistry keys on project path.

### D2: Hook script writes a flat JSON file; plugin polls it
No HTTP server, no socket, no daemon. The hook appends/removes entries from `~/.claude/office-sessions.json`. The plugin's `SessionPoller` reads it every 5 seconds on a `BukkitRunnable`.

Simple, crash-safe, and survives plugin restarts — the file persists across server bounces.

**Consequence:** Up to 5s lag on session start/stop appearing in the office. Acceptable.

### D3: Floor assignment is lowest-available-slot, high-water-mark grows upward
`FloorRegistry` maintains a list of slots. Each slot has a Y base coordinate, a state (VACANT or OCCUPIED), and an assigned project. New sessions claim the lowest VACANT slot. If all slots are occupied, a new slot is built above the current top.

Vacated floors are NOT demolished — they stay as structures with lights dimmed (torches removed or soul torches swapped in). The next session claims the slot and relights it.

**Consequence:** The building only grows, never shrinks. Acceptable for a dev tool.

### D4: Floor construction is animated, ~1 second
When the high-water mark grows, blocks are placed tick-by-tick via `BukkitRunnable`: floor slab → walls → ceiling → desks → torches → elevator shaft extension. ~20 block placements spread over 20 ticks.

If a slot is being reused (VACANT → OCCUPIED), no construction — just relight and reassign desks.

### D5: `floor.desks-per-floor` replaces the flat `desks` list
Config changes from a list of absolute coordinates to:
```yaml
floor:
  height: 8          # blocks per floor (floor slab to ceiling)
  width: 10          # floor footprint width
  depth: 10          # floor footprint depth
  desks-per-floor: 6 # desks auto-placed in a grid on each floor
```
Desk positions are computed at runtime from the floor's base Y and the building's X/Z origin. No manual coordinate editing needed.

### D6: BeadsPoller runs one poll cycle per active project
Instead of one `bd list` call, `BeadsPoller` iterates active sessions from `FloorRegistry`, runs `bd list --status=in_progress --json` with `ProcessBuilder.directory()` set to each project path, and emits `AgentSpawnEvent` / `AgentDespawnEvent` tagged with the floor.

### D7: Hook script is Python, uses only stdlib
No dependencies to install. Uses `json`, `pathlib`, `datetime`. Reads stdin, updates `office-sessions.json` atomically (write to `.tmp` then `rename`). Exits 0 always, suppresses all stdout/stderr.

## Risks / Trade-offs

- **Stale sessions:** If Claude crashes without firing `Stop`, the session stays in `office-sessions.json` indefinitely. Mitigation: SessionPoller checks if the session's jsonl file mtime is older than N minutes and auto-expires it.
- **Floor Y collision:** If the building grows too tall, floors may exit the world height limit. Mitigation: cap at 8 floors (64 blocks up from lobby), log a warning if exceeded.
- **Block placement permission:** The plugin needs write access to the world. Assumed — it's a dev server.
