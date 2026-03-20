## ADDED Requirements

### Requirement: Poll beads for task changes
The `BeadsPoller` SHALL run on a background `BukkitRunnable` at the configured `poll-interval-seconds`. Each cycle it SHALL call `bd list --status=in_progress --json` and compare the result to the previous snapshot to determine which tasks are new (need agent spawn) and which are gone (need agent despawn).

#### Scenario: New in-progress task detected
- **WHEN** a task appears in the poll result that was not in the previous snapshot
- **THEN** an `AgentSpawnEvent` is fired on the main thread with the task details

#### Scenario: Task removed from in-progress
- **WHEN** a task present in the previous snapshot is absent from the current poll result
- **THEN** an `AgentDespawnEvent` is fired on the main thread with the task ID

#### Scenario: Poller error handling
- **WHEN** the `bd` CLI call exits non-zero or times out
- **THEN** the poller logs a warning and skips that cycle without crashing

### Requirement: Startup state restore
On plugin enable, the poller SHALL perform an immediate poll and fire `AgentSpawnEvent` for every task currently `in_progress`, enabling NPCs to be restored after a server restart without waiting for the first interval.

#### Scenario: Server restart with active tasks
- **WHEN** the plugin enables and beads has 3 tasks in `in_progress`
- **THEN** 3 `AgentSpawnEvent`s are fired immediately, skipping elevator entry animation
