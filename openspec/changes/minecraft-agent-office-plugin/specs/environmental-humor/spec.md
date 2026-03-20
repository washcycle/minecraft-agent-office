## ADDED Requirements

### Requirement: Coffee machine tracks daily NPC visit count
The system SHALL track how many times NPCs have visited the coffee machine today and display the count on a sign near the machine.

#### Scenario: NPC visits coffee machine
- **WHEN** an NPC performs a coffee run micro-behavior
- **THEN** the visit counter increments and the sign updates to "Visits today: [N]"

#### Scenario: Midnight reset
- **WHEN** server clock passes midnight
- **THEN** the visit counter resets to 0

### Requirement: Days-since-blocked sign tracks incident-free time
The system SHALL display a sign showing how many days have passed since the last time any task was blocked, and track the all-time record.

#### Scenario: No blocked tasks
- **WHEN** no task has been blocked since the sign last reset
- **THEN** the sign shows "DAYS SINCE LAST BLOCKED ISSUE: [N]" and "PREVIOUS RECORD: [M]"

#### Scenario: Task becomes blocked
- **WHEN** any bd task transitions to `blocked`
- **THEN** the sign resets to 0 and the previous record is updated if the current streak exceeded it

### Requirement: Sprint vs. actual whiteboard shows goal drift
The system SHALL display a whiteboard in the office showing the sprint goal alongside the actual closed vs. opened count.

#### Scenario: Board updates on stats poll
- **WHEN** a bd stats poll completes
- **THEN** the whiteboard updates to show the sprint goal text, issues closed this sprint, and issues opened this sprint

#### Scenario: More opened than closed
- **WHEN** issues opened this sprint exceeds issues closed
- **THEN** the "opened" line is displayed in red

### Requirement: Velocity wall shows daily close rate as a bar chart
The system SHALL render a bar chart on one office wall showing the number of tasks closed per day for the past 7 days, using note block or wool block columns.

#### Scenario: Wall renders on plugin load
- **WHEN** the plugin starts
- **THEN** the velocity wall renders using bd closed task timestamps to compute daily counts for the past 7 days

#### Scenario: Task closes
- **WHEN** a bd task closes
- **THEN** today's bar on the velocity wall increments by one block height
