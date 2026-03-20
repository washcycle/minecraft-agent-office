# Manual Integration Test Checklist

These tests require a running Paper 1.20.x server with the plugin installed and a real `bd` binary on the server's PATH.

## Prerequisites

- [ ] Paper 1.20.x server running locally
- [ ] `AgentOffice-<version>.jar` copied to `plugins/`
- [ ] `plugins/AgentOffice/config.yml` configured with:
  - Valid `claude-api-key`
  - Valid `office-world` (must exist in the server)
  - Elevator position pointing at an existing location
  - At least 3 desks defined
- [ ] `bd` CLI on PATH with at least one in-progress task

---

## 1. Plugin Startup

- [ ] Server starts without errors related to AgentOffice
- [ ] Console shows `AgentOffice enabled — N desks ready.`
- [ ] Team leader ArmorStand spawns at configured `team-leader.position`
- [ ] Startup restore: if beads has in-progress tasks, NPCs appear directly at desks (no elevator animation)

## 2. `/office reload`

- [ ] Edit `config.yml` while server is running (change poll interval or desk count)
- [ ] Run `/office reload`
- [ ] Console shows `AgentOffice config reloaded successfully.`
- [ ] New poll interval takes effect on next cycle

## 3. `/office setup auto <rows> <cols>`

- [ ] Stand at a flat area in-game
- [ ] Run `/office setup auto 3 4`
- [ ] Chat shows `12 desks written to config.yml starting at X,Y,Z`
- [ ] Open `config.yml` and verify 12 desk entries at correct grid positions

## 4. `/office visualise`

- [ ] Run `/office visualise`
- [ ] Green particles appear above each desk for ~10 seconds
- [ ] Yellow/note particles appear above elevator top for ~10 seconds
- [ ] Particles stop after 10 seconds

## 5. `/office status`

- [ ] With active agents: output shows desk counts (free/occupied/total) and agent list with labels
- [ ] With no active agents: output shows `No active agents.`
- [ ] Desk counts match the number of in-progress beads tasks

## 6. Agent Spawn (Elevator Entry)

- [ ] Start a new beads task: `bd update <id> --status=in_progress`
- [ ] Within `poll-interval-seconds`, NPC spawns at elevator base
- [ ] NPC ascends to the top of the elevator shaft over `elevator.height` ticks
- [ ] NPC teleports to nearest free desk
- [ ] NPC's floating label updates within a few seconds to the Claude paraphrase (≤10 words)

## 7. Agent Despawn (Elevator Exit)

- [ ] Complete a beads task: `bd close <id>`
- [ ] Within `poll-interval-seconds`, NPC moves from desk to elevator top
- [ ] NPC descends to elevator base and disappears
- [ ] Desk is freed; if a queued agent was waiting, it moves to the newly freed desk

## 8. Desk Overflow Queue

- [ ] Configure fewer desks than in-progress tasks (e.g., 2 desks, 3 tasks)
- [ ] Third agent should not crash the server
- [ ] Complete one task — the queued agent should claim the freed desk

## 9. NPC Right-Click

- [ ] Right-click an agent ArmorStand → chat shows task ID and hint
- [ ] Right-click the team leader ArmorStand → chat shows full task board grouped by status

## 10. Team Leader Broadcast

- [ ] Stand within 32 blocks of team leader
- [ ] After `broadcast-interval-seconds`, team leader sends status line in chat
- [ ] With no active agents, message reads "The office is quiet — no active agents."
- [ ] Move further than 32 blocks away — broadcast should not reach you

## 11. Plugin Disable / Reload

- [ ] Run `/reload` or stop the server
- [ ] All agent ArmorStands are removed (no ghost entities)
- [ ] Team leader ArmorStand is removed
- [ ] Console shows `AgentOffice disabled.`

---

## Known Limitations (MVP)

- Elevator trapdoor animation requires a trapdoor block at `elevator.position` (top) — if absent, no block state change but no crash
- Claude paraphrase requires a valid API key; on failure the raw task title is shown instead
- Beads tasks must be JSON-parseable from `bd list --status=in_progress --json`; malformed output is logged and skipped
