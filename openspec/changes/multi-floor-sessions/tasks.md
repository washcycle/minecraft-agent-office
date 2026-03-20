## 1. Hook Script

- [ ] 1.1 Create `hooks/claude-office-hook` Python script: reads stdin JSON, extracts `session_id` and `cwd`, updates `~/.claude/office-sessions.json` atomically
- [ ] 1.2 Handle `SessionStart` event: upsert entry keyed by project path (same cwd = same entry, update session_id)
- [ ] 1.3 Handle `Stop` event: remove entry matching `session_id`
- [ ] 1.4 Suppress all stdout/stderr and always exit 0
- [ ] 1.5 Write install instructions to `hooks/INSTALL.md`: add `SessionStart` and `Stop` entries to `~/.claude/settings.json`

## 2. Session Events & Poller

- [ ] 2.1 Create `SessionStartEvent.java` with fields: `projectPath`, `projectName`
- [ ] 2.2 Create `SessionStopEvent.java` with field: `projectPath`
- [ ] 2.3 Create `SessionPoller.java`: BukkitRunnable, reads `~/.claude/office-sessions.json` every 5s, diffs against previous state, fires events on main thread
- [ ] 2.4 Implement stale session expiry: expire entries older than `session-expiry-minutes` (default 480)
- [ ] 2.5 Handle missing sessions file gracefully (treat as empty, log debug)
- [ ] 2.6 Register and start `SessionPoller` in `AgentOfficePlugin.onEnable()`

## 3. Floor Registry

- [ ] 3.1 Create `FloorSlot.java`: record with `floorNumber`, `yBase`, `state` (VACANT/OCCUPIED), `projectPath`
- [ ] 3.2 Create `FloorRegistry.java`: ordered slot list, `assign(projectPath)` returns slot (lowest vacant or new), `vacate(projectPath)` marks slot vacant
- [ ] 3.3 Implement same-project deduplication: `assign()` returns existing occupied slot if project already assigned
- [ ] 3.4 Implement floor cap: refuse to create more than `floor.max-floors` slots, log warning
- [ ] 3.5 Add `getDeskPositions(FloorSlot)`: compute grid of `floor.desks-per-floor` positions from slot Y and building origin

## 4. Floor Construction

- [ ] 4.1 Create `FloorConstructor.java`: given a `FloorSlot` and `World`, places floor slab, walls, ceiling, desks, torches, and elevator shaft extension over 20 ticks
- [ ] 4.2 Implement `vacate(FloorSlot)`: swap all torches on that floor to soul torches in one tick
- [ ] 4.3 Implement `relight(FloorSlot)`: swap soul torches back to regular torches in one tick
- [ ] 4.4 Skip construction (only relight) when assigning a VACANT slot

## 5. Config Changes

- [ ] 5.1 Add `floor.height`, `floor.width`, `floor.depth`, `floor.desks-per-floor`, `floor.max-floors` to default `config.yml`
- [ ] 5.2 Add `sessions-file` (default: `~/.claude/office-sessions.json`) and `session-expiry-minutes` to default `config.yml`
- [ ] 5.3 Update `PluginConfig.java` to parse new floor geometry fields and sessions-file path
- [ ] 5.4 Log a warning if old flat `desks` list is detected in config and ignore it

## 6. Beads Poller Update

- [ ] 6.1 Update `BeadsCliClient` to accept a project directory per invocation (set `ProcessBuilder.directory()`)
- [ ] 6.2 Update `BeadsPoller` to iterate `FloorRegistry.getOccupiedSlots()` and poll each project directory
- [ ] 6.3 Prefix task IDs with project slug (`<slug>:<taskId>`) to ensure global uniqueness
- [ ] 6.4 Skip polling when no floors are occupied

## 7. NPC & Elevator Wiring

- [ ] 7.1 Update `AgentRegistry` to receive floor Y offset from `FloorRegistry` when spawning NPCs
- [ ] 7.2 Update `ElevatorController.ascend()` to accept a target Y (floor top) instead of a fixed position
- [ ] 7.3 Update `AgentSpawnEvent` to carry the `FloorSlot` so downstream handlers know which floor
- [ ] 7.4 Wire `SessionStartEvent` → `FloorRegistry.assign()` → `FloorConstructor.build()` → `BeadsPoller` now includes this project
- [ ] 7.5 Wire `SessionStopEvent` → despawn all NPCs on that floor → `FloorConstructor.vacate()` → `FloorRegistry.vacate()`

## 8. Team Leader Update

- [ ] 8.1 Update `TeamLeaderNpc` broadcast to show per-floor status: `"Floor 2 [armada-vscode]: 2 agents | Floor 3 [AutoPortfolio]: 1 agent"`
- [ ] 8.2 Update right-click full task board to group by floor/project

## 9. Testing & Polish

- [ ] 9.1 Unit tests for `FloorRegistry`: assign, vacate, reuse, cap, deduplication
- [ ] 9.2 Unit tests for `SessionPoller`: delta detection, stale expiry, missing file
- [ ] 9.3 Update `/office status` to show floors and per-floor agent count
- [ ] 9.4 Update `/office visualise` to show all floors' desk and elevator markers
