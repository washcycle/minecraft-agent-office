## ADDED Requirements

### Requirement: Office world and desk grid
The plugin SHALL confine all NPC activity to the configured `office-world`. Desk positions are defined in `config.yml` as a list of `{x, y, z, facing}` entries. Each desk SHALL be occupiable by at most one agent NPC at a time.

#### Scenario: Desk assignment
- **WHEN** an agent NPC needs a desk and at least one desk is free
- **THEN** the nearest free desk to the elevator is assigned to that NPC

#### Scenario: No free desks — agent queues
- **WHEN** an agent NPC needs a desk and all desks are occupied
- **THEN** the NPC waits at the elevator lobby position until a desk frees up

### Requirement: Auto-layout command
The plugin SHALL provide `/office setup auto <rows> <cols>` which generates a desk grid starting at the command sender's current block position, spaced 2 blocks apart, and writes the result to `config.yml`.

#### Scenario: Auto-layout writes config
- **WHEN** an op runs `/office setup auto 3 4` standing at block (10, 64, 20)
- **THEN** 12 desk entries are written to `config.yml` in a 3×4 grid starting at (10, 64, 20)

### Requirement: Office visualisation command
`/office visualise` SHALL place temporary glowing particle markers at each desk and elevator position for 10 seconds so admins can verify layout without editing YAML.

#### Scenario: Visualise shows markers
- **WHEN** an op runs `/office visualise`
- **THEN** coloured particles appear at each desk position and the elevator for 10 seconds
