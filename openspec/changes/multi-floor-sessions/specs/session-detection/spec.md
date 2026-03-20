## ADDED Requirements

### Requirement: Hook script writes session state to shared file
A Python hook script SHALL be provided that reads Claude Code lifecycle event JSON from stdin and maintains `~/.claude/office-sessions.json`. The script MUST exit 0 always and produce no stdout or stderr output.

#### Scenario: Session start recorded
- **WHEN** Claude Code fires a `SessionStart` event with `cwd` and `session_id`
- **THEN** the hook adds an entry to `office-sessions.json` with `id`, `project` (basename of cwd), `path` (cwd), and `started` (ISO timestamp)

#### Scenario: Session stop removes entry
- **WHEN** Claude Code fires a `Stop` event with a known `session_id`
- **THEN** the hook removes that entry from `office-sessions.json`

#### Scenario: Same project deduplicates
- **WHEN** two sessions with the same `cwd` are both active
- **THEN** `office-sessions.json` contains only one entry for that project path (the most recently started session's ID wins)

#### Scenario: Hook never disrupts Claude
- **WHEN** the hook script throws any exception
- **THEN** the exception is silently suppressed and the script exits 0

#### Scenario: Atomic file write
- **WHEN** the hook writes to `office-sessions.json`
- **THEN** it writes to a `.tmp` file first, then atomically renames it, so the plugin never reads a partial file

### Requirement: SessionPoller detects changes and fires Bukkit events
The plugin SHALL include a `SessionPoller` BukkitRunnable that reads `office-sessions.json` every 5 seconds, compares to the previous state, and fires `SessionStartEvent` or `SessionStopEvent` on the main thread for each delta.

#### Scenario: New session detected
- **WHEN** a new project path appears in `office-sessions.json`
- **THEN** `SessionPoller` fires a `SessionStartEvent` with the project path and name

#### Scenario: Ended session detected
- **WHEN** a project path disappears from `office-sessions.json`
- **THEN** `SessionPoller` fires a `SessionStopEvent` with the project path

#### Scenario: Sessions file missing
- **WHEN** `office-sessions.json` does not exist
- **THEN** `SessionPoller` treats it as zero active sessions and logs a debug message

### Requirement: Stale session expiry
The `SessionPoller` SHALL expire sessions whose `started` timestamp is older than `session-expiry-minutes` (default: 480 — 8 hours) even if no `Stop` event was received.

#### Scenario: Crashed session cleaned up
- **WHEN** a session entry has a `started` time older than the expiry threshold
- **THEN** `SessionPoller` fires a `SessionStopEvent` and the entry is removed from the file
