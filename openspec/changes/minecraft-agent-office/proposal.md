## Why

AI agents (Claude) and project task management (beads) exist as pure CLI/API tools — there's no spatial, visual metaphor for observing a team of agents working. A Minecraft plugin gives you a living, walkable office where each agent is embodied as an NPC: you can literally see who's working, what they're doing, and when they finish.

## What Changes

- New Minecraft plugin (Paper API) providing a virtual office world
- `office` command suite: create office, spawn agents, configure desks, elevator
- Agent NPCs backed by real Claude API calls; each NPC is assigned a beads task
- NPCs enter the office via an elevator, walk to an assigned desk, display their current task above their head (floating text), and exit back through the elevator when their task closes
- Team leader NPC stands at a podium/monitor area and periodically broadcasts status summaries pulled from `bd list --status=in_progress`
- Beads integration: the plugin polls beads for task state changes; NPC behaviour is driven by task status transitions (`open` → `in_progress` → `done`)
- Claude integration: each agent NPC calls the Claude API to paraphrase its assigned task into a short, in-character speech bubble (e.g. "I'm refactoring the auth middleware")

## Capabilities

### New Capabilities

- `office-layout`: Physical office structure — elevator shaft, desk grid, podium, floor plan YAML config
- `agent-npc`: NPC lifecycle — spawn on task assignment, walk to desk, display floating task text, despawn on task close
- `elevator`: Animated elevator mechanics — agents queue, ride up/down, doors open/close
- `team-leader-npc`: Team leader NPC that polls beads and broadcasts status to nearby players via chat/hologram
- `claude-paraphrase`: Claude API integration — converts raw task title/description into in-character spoken text for the NPC speech bubble
- `beads-poller`: Background thread that watches beads task state; emits events the plugin responds to (agent enters, agent exits, status update)
- `plugin-config`: `config.yml` schema — office world name, desk count, Claude API key, beads poll interval, elevator position

### Modified Capabilities

<!-- none — this is a greenfield project -->

## Impact

- New plugin JAR (Paper 1.20+, Java 17+)
- Depends on: Paper API, Citizens2 or custom NPC lib, Claude Java/HTTP client, beads CLI (shelled out or via REST if available)
- No existing code modified — standalone plugin
