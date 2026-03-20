## ADDED Requirements

### Requirement: Plugin polls bd CLI for task state
The system SHALL execute bd CLI commands on defined intervals and parse JSON output to maintain internal task state.

#### Scenario: Successful poll
- **WHEN** a poll interval fires
- **THEN** the system executes `bd list --status=<status> --json` and `bd stats --json`, parses the output, and emits TaskStateEvents for any changes detected since the last poll

#### Scenario: bd CLI not found
- **WHEN** the plugin starts and bd is not found at the configured path
- **THEN** the plugin logs a warning to console and disables polling until bd is found at next startup

### Requirement: bd CLI path is configurable
The system SHALL read the bd CLI path from `config.yml` with a sensible default.

#### Scenario: Custom bd path configured
- **WHEN** `config.yml` contains `bd_path: /custom/path/bd`
- **THEN** all CLI invocations use the configured path

#### Scenario: Default path used
- **WHEN** `config.yml` does not specify `bd_path`
- **THEN** the system uses `bd` (relies on server PATH)

### Requirement: Plugin never writes to bd
The system SHALL be read-only with respect to bd — no create, update, or close commands are ever executed.

#### Scenario: Any plugin operation
- **WHEN** any plugin operation runs
- **THEN** only `bd list`, `bd stats`, and `bd show` commands are ever executed

### Requirement: TaskStateEvents are emitted for status transitions
The system SHALL emit typed internal events when task status changes are detected between polls.

#### Scenario: Task transitions to in_progress
- **WHEN** a poll detects a task that was not previously in_progress is now in_progress
- **THEN** a `TaskClaimedEvent` is emitted with task id, title, priority, and type

#### Scenario: Task transitions to blocked
- **WHEN** a poll detects a task that was not blocked is now blocked
- **THEN** a `TaskBlockedEvent` is emitted

#### Scenario: Task transitions to closed
- **WHEN** a poll detects a task no longer appears in open issues
- **THEN** a `TaskClosedEvent` is emitted with task id, priority, and type
