## Context

Greenfield Paper (Spigot-fork) plugin, Java 17+, targeting Minecraft 1.20.x. No existing codebase to migrate. The plugin bridges three external systems: the Minecraft server (Paper API), the beads task tracker (CLI: `bd`), and the Claude API (Anthropic HTTP). NPCs are implemented with a lightweight custom NPC system (ArmorStand + custom name tag) to avoid Citizens2 dependency complexity at MVP — Citizens2 can be added later for pathfinding upgrades.

## Goals / Non-Goals

**Goals:**
- One dedicated "office" world per server (configurable name)
- Desk grid and elevator position defined in `config.yml`
- Each in-progress beads task spawns one agent NPC at the elevator, walks to a free desk, and displays a Claude-paraphrased speech bubble as a floating name tag
- Team leader NPC at podium summarises `bd list --status=in_progress` every N seconds via nearby chat
- NPCs despawn and "take the elevator out" when their task moves to `done` or `cancelled`
- Claude API call produces ≤ 10-word paraphrase of the task title for the floating tag

**Non-Goals:**
- Full Citizens2 pathfinding (smooth walking) at MVP — NPCs teleport to desk position
- Multiplayer agent collaboration (agents don't interact with each other)
- Persistence across server restarts (NPCs respawn from live beads state on startup)
- Web dashboard or REST API
- Any Minecraft version below 1.20

## Decisions

### D1: Custom NPC vs Citizens2
**Decision**: Custom ArmorStand-based NPCs with invisible player skin (via PacketEvents or raw NMS) at MVP.
**Why**: Citizens2 is a heavyweight dependency with its own release cycle. For MVP the NPC just needs to stand at a desk and show a name tag. We can migrate to Citizens2 for pathfinding in a follow-up.
**Alternative**: Citizens2 from day one — adds smooth pathing but couples the plugin to a third-party release train.

### D2: Beads integration — shell-out vs REST
**Decision**: Shell out to `bd` CLI via `ProcessBuilder` on a background thread.
**Why**: beads has no HTTP server at the moment; shelling out is the simplest contract. Wrapped in a `BeadsClient` interface so a future REST/socket implementation can be swapped in.
**Alternative**: Parse `.beads/` SQLite/Dolt files directly — fragile, couples to internal format.

### D3: Claude paraphrase — when to call
**Decision**: Call Claude once per NPC at spawn time. Cache the result on the NPC. Re-call if the task title changes on the next poll cycle.
**Why**: Avoids hammering the API on every poll tick. Task titles rarely change mid-flight.
**Model**: `claude-haiku-4-5` — low latency, low cost, sufficient for a ≤10-word paraphrase.

### D4: Beads poll strategy
**Decision**: Single `BeandsPollerTask` (BukkitRunnable) runs every `poll-interval-seconds` (default 10). Compares current `in_progress` task set to previous snapshot; emits `AgentSpawnEvent` / `AgentDespawnEvent` for delta tasks.
**Why**: Centralised polling avoids per-NPC timers and simplifies state management.

### D5: Office layout storage
**Decision**: `config.yml` stores elevator block coordinates and a desk list (each desk: `{x, y, z, facing}`). Layout can also be auto-generated with `/office setup auto <rows> <cols>` which scans the player's current location.
**Why**: Admins need to be able to customise the office to their build. YAML is readable and editable outside the game.

### D6: Elevator animation
**Decision**: Simple vertical teleport sequence (NPC moves 1 block/tick for elevator height) with door-open/close using trapdoor block state updates.
**Why**: Full piston animation is complex and version-sensitive. Trapdoor toggle is a single `setBlockData` call and looks good enough.

## Risks / Trade-offs

- **Claude API latency at spawn** → NPC spawns immediately with task title as raw text; Claude paraphrase replaces it async when ready. Player sees a brief "raw" label.
- **`bd` CLI not on PATH of the Minecraft server process** → Plugin startup fails with clear error message; config supports explicit `beads-binary` path override.
- **NMS / PacketEvents version lock** → Custom player NPCs require version-specific NMS. Mitigation: use ArmorStand with `setCustomName` as fallback; upgrade NMS layer as a separate task.
- **Desk overflow (more tasks than desks)** → Extra agents queue at the elevator and wait for a desk to free up. Queue is visible as "waiting" NPCs in the elevator area.
- **Server restart loses NPC state** → On enable, plugin calls `bd list --status=in_progress` and re-spawns all active agents at their desks. Elevator entry animation is skipped for restore.

## Open Questions

- Should the team leader NPC speak via chat (visible to all nearby players) or via a hologram-style display entity above its head?
- Do we want agents to "type" at their desks (arm animation) — requires ProtocolLib or PacketEvents?
- Should completed tasks trigger a brief celebration animation before the agent walks to the elevator?
