## ADDED Requirements

### Requirement: New floors are built with a block-placement animation
When `FloorRegistry` creates a slot above the current high-water mark, `FloorConstructor` SHALL place blocks over approximately 20 ticks: floor slab, walls, ceiling, desks, torches, and elevator shaft extension.

#### Scenario: Floor materialises visibly
- **WHEN** a new slot is needed above the current top
- **THEN** blocks appear progressively over ~1 second on the main thread via BukkitRunnable

#### Scenario: Reused slot is not rebuilt
- **WHEN** a VACANT slot is assigned to a new session
- **THEN** no block placement occurs — only lights are restored (soul torches swapped back to regular torches)

### Requirement: Vacated floors dim but stay standing
When a session ends, `FloorConstructor` SHALL swap torches on that floor to soul torches (blue light) to signal vacancy. No blocks are removed.

#### Scenario: Vacant floor appears dim
- **WHEN** a session ends and its floor slot becomes VACANT
- **THEN** all torch blocks on that floor are replaced with soul torches within 1 tick

#### Scenario: Reclaimed floor relights
- **WHEN** a VACANT floor is assigned to a new session
- **THEN** all soul torches on that floor are replaced with regular torches within 1 tick
