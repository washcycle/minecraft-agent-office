## ADDED Requirements

### Requirement: FloorRegistry manages slot allocation
The `FloorRegistry` SHALL maintain an ordered list of floor slots. Each slot has a Y-base coordinate, state (VACANT or OCCUPIED), and an assigned project path. Slots are assigned lowest-first.

#### Scenario: First session gets floor 2
- **WHEN** the first session arrives and no slots exist
- **THEN** a new slot is created at the configured base Y + floor height, assigned to that project

#### Scenario: Second session gets next available floor
- **WHEN** a second session arrives with a different project path
- **THEN** it is assigned the next slot above the first occupied floor

#### Scenario: Vacant slot is reused
- **WHEN** a session ends and a new session starts
- **THEN** the new session claims the lowest VACANT slot rather than building a new one above

#### Scenario: Same project shares a floor
- **WHEN** a `SessionStartEvent` fires for a project path already assigned to an occupied slot
- **THEN** no new slot is created and the existing floor is returned

### Requirement: Floor cap prevents world overflow
The `FloorRegistry` SHALL refuse to create more than `floor.max-floors` (default: 8) slots and log a warning when the cap is reached.

#### Scenario: Cap enforced
- **WHEN** all slots are occupied and a new session arrives and the slot count equals `floor.max-floors`
- **THEN** no new slot is created, a warning is logged, and the session has no floor assigned

### Requirement: Floor provides desk positions
Each floor slot SHALL expose a list of desk positions computed from the slot's Y base, the building X/Z origin, and `floor.desks-per-floor`. Desks are arranged in a grid.

#### Scenario: Desk positions derived from floor Y
- **WHEN** a floor at Y=78 is requested for its desk list
- **THEN** all returned desk positions have y=79 (one above the floor slab)
