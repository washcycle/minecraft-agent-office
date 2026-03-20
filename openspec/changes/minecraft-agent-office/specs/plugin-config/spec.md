## ADDED Requirements

### Requirement: Plugin configuration file
The plugin SHALL read all configuration from `config.yml` on startup and expose it via a `PluginConfig` singleton. Invalid or missing required keys SHALL prevent the plugin from enabling and log a clear error.

#### Scenario: Valid config loads successfully
- **WHEN** `config.yml` contains valid values for all required keys
- **THEN** the plugin enables without errors and `PluginConfig` is populated

#### Scenario: Missing Claude API key
- **WHEN** `config.yml` is missing the `claude-api-key` field
- **THEN** the plugin logs `[AgentOffice] ERROR: claude-api-key is required` and disables itself

#### Scenario: Missing beads binary
- **WHEN** `beads-binary` is set to a path that does not exist
- **THEN** the plugin logs `[AgentOffice] ERROR: beads binary not found at <path>` and disables itself

### Requirement: Configuration schema
The plugin config SHALL support the following keys:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `office-world` | String | `world` | Name of the Minecraft world used as the office |
| `claude-api-key` | String | (required) | Anthropic API key |
| `beads-binary` | String | `bd` | Path or name of the beads CLI binary |
| `poll-interval-seconds` | Int | `10` | How often to poll beads for task changes |
| `team-leader.position` | BlockPos | (required) | x,y,z of the team leader NPC |
| `team-leader.broadcast-interval-seconds` | Int | `30` | How often the team leader speaks |
| `elevator.position` | BlockPos | (required) | x,y,z of elevator base |
| `elevator.height` | Int | `4` | Elevator shaft height in blocks |
| `desks` | List\<DeskConfig\> | (required) | List of desk positions and facings |

#### Scenario: Reload command updates config
- **WHEN** an operator runs `/office reload`
- **THEN** `config.yml` is re-read and `PluginConfig` is updated without restarting the server
