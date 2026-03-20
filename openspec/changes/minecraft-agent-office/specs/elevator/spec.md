## ADDED Requirements

### Requirement: Elevator entry animation
When an agent NPC is assigned to a desk, it SHALL appear at the bottom of the elevator shaft, "ride" up (teleporting 1 block per tick) to the office floor, then walk (teleport) to its assigned desk.

#### Scenario: Agent enters via elevator
- **WHEN** an `AgentSpawnEvent` fires with `isRestore=false`
- **THEN** the NPC spawns at the elevator base, ascends to the office floor over `elevator.height` ticks, then moves to its desk

#### Scenario: Restore skips elevator
- **WHEN** an `AgentSpawnEvent` fires with `isRestore=true` (server restart)
- **THEN** the NPC spawns directly at the desk position with no elevator animation

### Requirement: Elevator exit animation
When an agent's task completes, the NPC SHALL walk from its desk to the elevator, descend, and despawn at the bottom.

#### Scenario: Agent exits via elevator
- **WHEN** an `AgentDespawnEvent` fires for an NPC currently at a desk
- **THEN** the NPC moves to the elevator top position, descends over `elevator.height` ticks, and is removed from the world

### Requirement: Elevator door state
The elevator shaft SHALL have two trapdoor blocks at the top (office floor level) that open when an NPC arrives and close after the NPC exits.

#### Scenario: Doors open on arrival
- **WHEN** an NPC reaches the top of the elevator shaft
- **THEN** the trapdoor blocks transition to open state

#### Scenario: Doors close after exit
- **WHEN** an NPC has fully descended and despawned
- **THEN** the trapdoor blocks transition to closed state
