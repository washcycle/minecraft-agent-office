## MODIFIED Requirements

### Requirement: BeadsPoller polls per active session project
The `BeadsPoller` SHALL iterate over all floors currently tracked by `FloorRegistry` and run one `bd list --status=in_progress --json` per project directory, using `ProcessBuilder.directory()` to set the working directory. Results from all projects are merged into a single delta comparison keyed by `<projectSlug>:<taskId>`.

#### Scenario: Two active sessions both polled
- **WHEN** `FloorRegistry` has two occupied floors with different project paths
- **THEN** `BeadsPoller` makes two separate `bd list` invocations and fires events for tasks from both projects

#### Scenario: Failed poll for one project does not block others
- **WHEN** `bd list` fails or times out for one project
- **THEN** that project is skipped for this cycle, a warning is logged, and other projects are still polled

#### Scenario: Task IDs are project-scoped
- **WHEN** two projects both have a task with id `abc-1`
- **THEN** they are treated as distinct: `projectA:abc-1` and `projectB:abc-1`

#### Scenario: No active sessions means no polling
- **WHEN** `FloorRegistry` has no occupied floors
- **THEN** `BeadsPoller` makes zero `bd list` invocations
