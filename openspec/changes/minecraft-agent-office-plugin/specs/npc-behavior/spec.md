## ADDED Requirements

### Requirement: Each NPC has an independent behavior timer with jitter
The system SHALL assign each NPC a unique behavior tick interval with randomized jitter so NPCs do not animate in sync.

#### Scenario: NPC spawns
- **WHEN** an NPC is assigned to a desk
- **THEN** its behavior timer is set to a base interval of 70–110 ticks plus a per-cycle random jitter of 0–40 ticks

### Requirement: Working NPCs perform micro-behaviors
While in WORKING state, NPCs SHALL periodically perform small animations chosen by weighted random roll each behavior tick.

#### Scenario: Behavior tick fires while working
- **WHEN** an NPC's behavior timer fires and NPC is in WORKING state
- **THEN** the system rolls against the behavior table: 10% typing burst, 8% glance at coworker, 5% coffee run, 4% lean back, 2% personality quip bubble, 1% player acknowledgement if player within 8 blocks, 70% continue typing

### Requirement: Desk personalities have distinct voices
Each desk position SHALL have a fixed personality that influences chat bubble content and conference room dialog.

#### Scenario: Desk A NPC generates a bubble
- **WHEN** Desk A NPC fires a chat bubble
- **THEN** the bubble prompt includes "overconfident, casually dismissive of complexity" as the personality modifier

#### Scenario: Desk B NPC generates a bubble
- **WHEN** Desk B NPC fires a chat bubble
- **THEN** the bubble prompt includes "anxious, second-guessing themselves" as the personality modifier

#### Scenario: Desk C NPC generates a bubble
- **WHEN** Desk C NPC fires a chat bubble
- **THEN** the bubble prompt includes "veteran, terse and slightly world-weary" as the personality modifier

#### Scenario: Desk D NPC generates a bubble
- **WHEN** Desk D NPC fires a chat bubble
- **THEN** the bubble prompt includes "enthusiastic, prone to scope creep ideas" as the personality modifier

### Requirement: NPCs acknowledge nearby players without breaking work
When a player comes within 8 blocks of a working NPC, the system SHALL trigger a brief acknowledgement.

#### Scenario: Player walks near desk
- **WHEN** a player enters within 8 blocks of a working NPC
- **THEN** NPC briefly turns head toward player, emits a short personality-voiced acknowledgement bubble, then returns to monitor-facing position

### Requirement: Stuck NPCs display dejected behavior
NPCs in STUCK state SHALL display visually distinct dejected behavior.

#### Scenario: NPC is stuck
- **WHEN** NPC is in STUCK state
- **THEN** NPC head is tilted down toward desk, behavior loop occasionally emits dejected quips ("Day 3. Still waiting."), coffee run frequency increases to 20%
