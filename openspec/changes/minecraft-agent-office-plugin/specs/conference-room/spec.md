## ADDED Requirements

### Requirement: Standup scene triggers on a configurable interval
The system SHALL trigger a standup conference room scene periodically based on a configurable interval (default: 20 real-time minutes).

#### Scenario: Standup interval fires
- **WHEN** the standup interval elapses and no scene is already in progress
- **THEN** the team leader NPC stands, emits "Alright everyone, sync time.", all working NPCs walk to the conference room, each NPC delivers a 1-2 line status update in their desk personality voice, the team leader summarizes, and all NPCs return to their desks

#### Scenario: Standup fires during active scene
- **WHEN** the standup interval elapses while a scene is in progress
- **THEN** the standup is skipped for this interval

### Requirement: Delivery scene triggers on significant task completion
The system SHALL trigger a delivery conference room scene when a task with priority 0 or 1, or an epic task, closes.

#### Scenario: High-priority task closes
- **WHEN** a task with priority 0 or 1 closes
- **THEN** the completing NPC walks to the team leader's desk, delivers a paraphrased summary, the team leader calls "Conference room. 2 minutes.", all available NPCs walk to the conference room, the completing NPC presents at the whiteboard (task title displayed on map art), other NPCs react with personality-voiced lines, team leader says "Ship it.", all NPCs return to desks

#### Scenario: Normal priority task closes
- **WHEN** a task with priority 2, 3, or 4 closes (and is not an epic)
- **THEN** no conference room scene triggers; NPC departure sequence begins normally

#### Scenario: Major ship (epic closes or P0 closes)
- **WHEN** an epic or priority 0 task closes
- **THEN** delivery scene plays as normal, AND a confetti particle effect fires in the conference room, a note block fanfare plays, then team leader immediately says "Okay. Back to work. We have [N] open issues."

### Requirement: SceneDirector enforces one scene at a time
The system SHALL prevent concurrent conference room scenes.

#### Scenario: Delivery trigger fires during standup
- **WHEN** a delivery trigger fires while a standup scene is running
- **THEN** the delivery scene is queued (max queue depth: 2) and begins after the standup completes

#### Scenario: Queue is full
- **WHEN** a delivery trigger fires and the scene queue is already at max depth
- **THEN** the trigger is dropped

### Requirement: NPCs return to saved desk state after any scene
The system SHALL restore each NPC to its pre-scene desk position and behavior state after a conference room scene ends.

#### Scenario: Scene ends
- **WHEN** a conference room scene completes
- **THEN** each NPC pathfinds back to its assigned desk and resumes its previous behavior loop state (WORKING or STUCK)

### Requirement: Conference room has a sprint board
The conference room SHALL display a sprint board showing overall project progress.

#### Scenario: Sprint board displays during standup
- **WHEN** a standup scene is in progress
- **THEN** the sprint board shows: sprint goal text, percent complete progress bar, issues closed this week, and blocked count (displayed in red if > 0)

#### Scenario: Team leader gestures at sprint board
- **WHEN** the team leader delivers the standup summary
- **THEN** the team leader NPC turns to face the sprint board while speaking
