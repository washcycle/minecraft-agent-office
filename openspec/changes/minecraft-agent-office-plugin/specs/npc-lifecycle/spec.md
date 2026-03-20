## ADDED Requirements

### Requirement: NPC spawns at elevator on task claim
When a bd task transitions to `in_progress`, the system SHALL spawn a Citizens2 NPC at the elevator entrance and assign it to the next available desk.

#### Scenario: Task claimed, desk available
- **WHEN** bd task status changes to `in_progress`
- **THEN** an NPC spawns at the elevator position, plays arrival sound (ding), walks to the assigned desk, and begins the working behavior loop

#### Scenario: Task claimed, all desks occupied
- **WHEN** bd task status changes to `in_progress` and all desks are occupied
- **THEN** the NPC spawns at elevator but waits in a lobby position until a desk frees up

### Requirement: NPC departs on task close
When a bd task transitions to `closed`, the system SHALL trigger the assigned NPC's departure sequence.

#### Scenario: Task closed normally
- **WHEN** bd task status changes to `closed`
- **THEN** the NPC saves work animation plays, NPC walks to elevator, elevator sound plays, NPC despawns

#### Scenario: Task closed during conference room scene
- **WHEN** bd task closes while NPC is in a conference room scene
- **THEN** NPC completes the current scene, then begins departure sequence

### Requirement: NPC slumps when task is blocked
When a bd task transitions to `blocked`, the system SHALL update the NPC's visual state to reflect being stuck.

#### Scenario: Task becomes blocked
- **WHEN** bd task status changes to `blocked`
- **THEN** NPC head tilts down (toward desk), behavior loop switches to STUCK state, blocked sound plays

#### Scenario: Blocker resolved
- **WHEN** a blocked task's blocker is resolved and task returns to `in_progress`
- **THEN** NPC resumes normal working behavior loop

### Requirement: Desk assignment persists across behavior states
The system SHALL remember each NPC's assigned desk throughout all behavior states (working, stuck, conference room, coffee run).

#### Scenario: NPC returns from conference room
- **WHEN** a conference room scene ends
- **THEN** all participating NPCs pathfind back to their previously assigned desks
