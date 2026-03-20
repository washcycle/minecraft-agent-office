## ADDED Requirements

### Requirement: Scrum board displays bd tasks in four columns
The system SHALL render a scrum board using Text Display entities with four columns: BACKLOG, READY, IN PROGRESS, and DONE.

#### Scenario: Board renders on plugin load
- **WHEN** the plugin starts
- **THEN** the scrum board renders all current bd tasks in their correct columns using Text Display entities

#### Scenario: Task moves columns
- **WHEN** a bd task changes status
- **THEN** the corresponding Text Display entity moves to the correct column within one poll cycle

### Requirement: Cards are color-coded by priority
The system SHALL apply priority-based color formatting to each task card on the board.

#### Scenario: Priority 0 card
- **WHEN** a task with priority 0 is rendered
- **THEN** the card text is formatted in red

#### Scenario: Priority 1 card
- **WHEN** a task with priority 1 is rendered
- **THEN** the card text is formatted in gold/orange

#### Scenario: Priority 2 card
- **WHEN** a task with priority 2 is rendered
- **THEN** the card text is formatted in yellow

#### Scenario: Priority 3 card
- **WHEN** a task with priority 3 is rendered
- **THEN** the card text is formatted in green

#### Scenario: Priority 4 card
- **WHEN** a task with priority 4 is rendered
- **THEN** the card text is formatted in gray

### Requirement: Blocked cards flash red
The system SHALL make blocked task cards visually distinct by toggling their color between red and their priority color every 10 ticks.

#### Scenario: Task is blocked
- **WHEN** a task has status `blocked`
- **THEN** its card alternates between red text and normal priority color every 10 ticks (0.5 seconds)

### Requirement: In-progress cards show the assigned NPC name
The system SHALL display the assigned NPC's name on in-progress task cards.

#### Scenario: Task in progress with assigned NPC
- **WHEN** a task is in_progress and has an assigned NPC
- **THEN** the card displays the task title and the NPC desk personality name (e.g., "● The Veteran")

### Requirement: Completed tasks trigger a particle effect
The system SHALL emit a particle effect at a task card's board position when that task closes.

#### Scenario: Task closes
- **WHEN** a bd task transitions to `closed`
- **THEN** a brief particle burst (happy villager or end rod particles) fires at the card's Text Display position before the card moves to the DONE column

### Requirement: Board polls bd on defined intervals
The system SHALL poll bd CLI on defined intervals to keep board state current.

#### Scenario: Polling intervals
- **WHEN** the plugin is running
- **THEN** in_progress and blocked tasks are polled every 5 seconds, open/ready tasks every 10 seconds, and bd stats every 30 seconds
