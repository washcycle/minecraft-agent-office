## Context

This is a greenfield Paper 1.21.1 Minecraft plugin. No existing plugin code. The server is the visualization layer for real beads (bd) task work — NPCs mirror live task states, a scrum board displays bd data, and a Claude API integration generates humorous chat bubbles. The plugin is read-only with respect to bd — it never writes to the task store.

Players connect with standard Java Edition clients (no mods). The server runs on Windows or macOS. Citizens2 handles NPC pathfinding and trait management. Text Display entities (added in 1.19.4) power the scrum board.

## Goals / Non-Goals

**Goals:**
- Visualize bd task states as living NPCs in a Minecraft office
- Render a live scrum board using Text Display entities
- Orchestrate multi-NPC conference room scenes (standup + delivery)
- Generate humorous task paraphrases via Claude API
- Environmental humor objects driven by bd data
- Work on Windows and macOS servers, vanilla Java Edition clients

**Non-Goals:**
- Writing to bd — plugin is read-only
- Authenticating players or per-player state
- Persisting NPC positions across restarts (bd state is re-read on startup)
- Supporting Bedrock Edition clients
- Supporting Fabric/Forge (Paper only)

## Decisions

### D1: bd integration via CLI polling (not file watching)
**Decision**: Poll `bd list --json`, `bd stats --json` on intervals rather than watching raw Dolt files.
**Rationale**: bd CLI is the stable public interface. Raw Dolt internals are an implementation detail that could change. CLI polling is simple, reliable, and already provides structured JSON.
**Alternative considered**: Watch `.beads/issues.jsonl` directly — fragile, couples to bd internals.
**Intervals**: in_progress/blocked every 5s, open/ready every 10s, stats every 30s.

### D2: Citizens2 for NPCs
**Decision**: Use Citizens2 trait system for all NPC behavior.
**Rationale**: Gold standard for Paper servers. Handles pathfinding, head tracking, animation hooks. Avoids reimplementing complex NPC AI.
**Alternative considered**: Packet-only fake players — more control but enormous complexity, no pathfinding.

### D3: Text Display entities for scrum board
**Decision**: Use Text Display entities (1.19.4+) instead of signs or armor stands.
**Rationale**: Clean positioned text, no hitbox, instant updates via metadata packets, supports color/formatting. Signs have 4-line limit and look dated. Armor stand name tags are jank.
**Target version**: Paper 1.21.1 (display entities stable since 1.19.4).

### D4: Per-NPC independent behavior timers with jitter
**Decision**: Each NPC has its own `BukkitRunnable` with a randomized base interval (3.5–5.5s) plus per-tick random jitter.
**Rationale**: Synchronized NPCs feel like a puppet show. Independent timers create genuine office energy.
**Implementation**: On NPC spawn, assign `baseInterval = random(70, 110) ticks` + `jitter = random(0, 40) ticks` per cycle.

### D5: SceneDirector as a server-side coordinator
**Decision**: A singleton `SceneDirector` manages all multi-NPC scenes. NPCs register themselves; SceneDirector issues move commands and sequences dialog.
**Rationale**: NPCs can't coordinate themselves — they need a central conductor that knows everyone's state. SceneDirector is the single source of truth for "is a scene in progress."
**Scene lock**: Only one scene runs at a time. Incoming triggers queue or drop if scene is active.

### D6: Claude API for bubble paraphrasing
**Decision**: Call Claude API (claude-haiku-4-5) synchronously on async thread when a bubble is due to fire.
**Rationale**: Haiku is fast and cheap — latency is acceptable for a ~15% chance bubble that fires every 45s minimum. Using haiku keeps costs low for a server that runs continuously.
**Fallback**: If Claude API call fails or times out (>3s), use a canned fallback from a per-personality phrase bank.
**Prompt**: "You are a [personality] office worker. Paraphrase this task title as a casual one-liner you'd say at your desk: '[task title]'. Max 12 words. No quotes."

### D7: Desk personality is fixed to desk position, not NPC
**Decision**: Desk A is always "The Overconfident One", Desk B always "The Anxious One", etc. The NPC at that desk adopts the personality.
**Rationale**: Simplifies dialog generation (personality is a static prompt modifier). Players learn desk characters over time, which builds attachment.

## Architecture

```
BdPoller
  → polls bd CLI on intervals
  → emits TaskStateEvent (claimed, updated, closed, blocked)

TeamStateManager
  → consumes TaskStateEvents
  → maintains: taskId → NPC mapping, desk → task mapping
  → triggers NPCController and SceneDirector

NPCController (Citizens2 wrapper)
  → spawn/despawn NPCs at elevator
  → assign desk, start behavior loop
  → execute micro-behaviors (animations, pathfinding)
  → delegate bubble requests to BubbleEngine

BubbleEngine
  → per-NPC cooldown tracking
  → proximity check (8 blocks)
  → async Claude API call (Haiku) with fallback phrase bank
  → sends chat bubble via Citizens2 speech trait

SceneDirector
  → scene queue (standup, delivery)
  → gathers NPCs, sequences dialog, dismisses
  → reads conference room sprint board data from BdPoller

BoardRenderer
  → manages Text Display entity lifecycle
  → maps bd task list → 4-column display
  → color coding, blocked flash animation (toggle metadata every 10 ticks)
  → particle effect on task close

EnvironmentalObjects
  → CoffeeMachine: visit counter, resets midnight
  → IncidentSign: days since last blocked, resets on block event
  → VelocityWall: bar chart from bd closed timestamps
  → SprintWhiteboard: goal vs. actual, updated on stats poll
```

## Risks / Trade-offs

- **Citizens2 API changes** → Mitigation: pin Citizens2 version in Gradle, test on upgrade
- **Claude API latency spikes** → Mitigation: 3s timeout + fallback phrase bank means bubbles always fire
- **bd CLI not on PATH** → Mitigation: configurable bd path in `config.yml`, startup warning if not found
- **Scene during mass task close** → Mitigation: scene queue with max depth 2, drop excess delivery triggers
- **NPC pathfinding fails in custom office layout** → Mitigation: Citizens2 waypoint system; test pathfinding in office build before release
- **Text Display entity count** → Up to ~40 cards on board at once; well within client render limits

## Migration Plan

Greenfield — no migration needed. On first server start:
1. Plugin reads bd task list
2. Spawns NPCs for all `in_progress` tasks
3. Renders scrum board from current bd state
4. Starts polling intervals

## Open Questions

- Should standup interval be configurable in `config.yml` (default: every 20 min real time)?
- How many desks in the initial office? 4 feels right for a v1, expandable later.
- Should the office world be bundled as a schematic, or documented as a manual build?
