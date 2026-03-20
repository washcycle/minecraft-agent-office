## ADDED Requirements

### Requirement: Team leader NPC spawns at configured position
One permanent team leader NPC SHALL exist at the position defined in `config.yml` under `team-leader.position`. It SHALL be an ArmorStand named `[Team Lead] <server-name>`. Unlike agent NPCs, the team leader persists across task changes and is never despawned by the poller.

#### Scenario: Team leader spawns on plugin enable
- **WHEN** the plugin enables and `team-leader.position` is set
- **THEN** a team leader ArmorStand NPC exists at that position

#### Scenario: Team leader survives task changes
- **WHEN** agent NPCs are spawned or despawned due to beads task changes
- **THEN** the team leader NPC is unaffected

### Requirement: Periodic status broadcast
Every `team-leader.broadcast-interval-seconds`, the team leader SHALL send a status message to all players within 32 blocks in the format:

```
[Team Lead] 🟢 3 agents working | alice: Refactoring auth | bob: Writing tests | charlie: Fixing CI
```

If no tasks are in progress, the message SHALL be: `[Team Lead] 🟡 Office is quiet — no active tasks`

#### Scenario: Status broadcast with active agents
- **WHEN** the broadcast timer fires and 2 agents are active
- **THEN** nearby players receive one chat line listing each agent's paraphrased task

#### Scenario: Status broadcast with no agents
- **WHEN** the broadcast timer fires and 0 agents are active
- **THEN** nearby players receive the "office is quiet" message

### Requirement: Team leader right-click shows full task list
When a player right-clicks the team leader NPC, the plugin SHALL send that player a formatted list of all tasks from `bd list --json` (all statuses) grouped by status.

#### Scenario: Player queries team leader
- **WHEN** a player right-clicks the team leader NPC
- **THEN** the player receives a multi-line chat message with all tasks grouped by status (in_progress, open, done)
