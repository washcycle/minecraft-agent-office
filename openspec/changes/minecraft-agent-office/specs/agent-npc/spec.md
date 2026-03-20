## ADDED Requirements

### Requirement: Agent NPC spawning
Each in-progress beads task SHALL be represented by one agent NPC. The NPC SHALL be implemented as an invisible ArmorStand with a custom name tag displaying the Claude-paraphrased task text. The NPC name SHALL be formatted as `[<assignee>] <paraphrase>` where assignee is the beads task assignee field (or "Agent" if unset).

#### Scenario: NPC spawns for new task
- **WHEN** `AgentSpawnEvent` fires for task `beads-abc123` assigned to `alice`
- **THEN** an ArmorStand NPC named `[alice] <paraphrase>` appears at the elevator

#### Scenario: NPC name updates on paraphrase ready
- **WHEN** the Claude paraphrase completes asynchronously after spawn
- **THEN** the NPC custom name is updated to the paraphrased text on the main thread

### Requirement: Agent NPC despawning
When an `AgentDespawnEvent` fires, the NPC SHALL perform the elevator exit animation and then be removed from the world. Its desk SHALL be marked as free.

#### Scenario: NPC despawns cleanly
- **WHEN** `AgentDespawnEvent` fires for a task whose NPC is at a desk
- **THEN** the NPC performs the exit animation, desk is freed, NPC entity is removed

### Requirement: Agent NPC registry
The plugin SHALL maintain a `AgentRegistry` mapping task ID → NPC entity. This registry SHALL be used to prevent duplicate NPCs for the same task and to look up NPCs for despawn events.

#### Scenario: Duplicate spawn prevention
- **WHEN** `AgentSpawnEvent` fires for a task ID already in the registry
- **THEN** no new NPC is spawned and a debug log is emitted

### Requirement: NPC right-click interaction
When a player right-clicks an agent NPC, the plugin SHALL send that player a chat message showing the full beads task title, description, and current status.

#### Scenario: Player inspects agent
- **WHEN** a player right-clicks an agent NPC
- **THEN** the player receives a formatted chat message with the task's full details
