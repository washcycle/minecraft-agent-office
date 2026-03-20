## MODIFIED Requirements

### Requirement: Layout is defined by floor geometry, not absolute desk coordinates
`config.yml` SHALL use `floor.height`, `floor.width`, `floor.depth`, and `floor.desks-per-floor` instead of a flat `desks` list. Desk positions are computed at runtime per floor from the building origin and floor Y offset.

#### Scenario: Desks computed per floor
- **WHEN** a floor at Y=78 is constructed with `desks-per-floor: 6`
- **THEN** six desk positions are generated in a 3×2 grid at Y=79, offset from the building X/Z origin

#### Scenario: Config migration
- **WHEN** `config.yml` still contains the old flat `desks` list
- **THEN** the plugin logs a warning and ignores the old list, using computed layout instead
