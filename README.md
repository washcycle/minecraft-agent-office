# Minecraft Agent Office

A Paper 1.20.x plugin that gives your Claude agents a physical presence in Minecraft. Each [beads](https://github.com/Fission-AI/beads) task in progress spawns an NPC agent that rides the elevator in, sits at a desk with a floating task label (paraphrased by Claude), and rides the elevator out when done. A team leader NPC broadcasts live status to nearby players.

## Features

- **Agent NPCs** — one ArmorStand NPC per in-progress beads task
- **Elevator entry/exit** — animated ascent and descent through a configurable shaft
- **Floating task labels** — Claude paraphrases each task into ≤10 words, shown above the NPC
- **Team leader NPC** — periodic status broadcasts + right-click for full task board
- **Desk assignment** — nearest free desk to elevator; overflow agents queue
- **Live sync** — beads polled every N seconds; NPCs appear/disappear automatically

## Quick Start

```bash
./gradlew build   # requires Java 17
# copy build/libs/minecraft-agent-office-*.jar to plugins/
```

Edit `plugins/AgentOffice/config.yml`:
```yaml
claude-api-key: "sk-ant-..."
elevator.position: {x: 10, y: 60, z: 10}
desks:
  - {x: 5, y: 64, z: 5, facing: NORTH}
```

Or stand in-game and run `/office setup auto 3 4` to generate a 3×4 desk grid.

## Commands

| Command | Description |
|---------|-------------|
| `/office reload` | Re-read config.yml |
| `/office setup auto <rows> <cols>` | Generate desk grid at your feet |
| `/office visualise` | Particle preview of desk + elevator positions |
| `/office status` | List active agents |

## How It Works

```
bd list --json (every N seconds)
  → BeadsPoller detects delta
    → AgentSpawnEvent → elevator ascent → desk assignment → Claude paraphrase
    → AgentDespawnEvent → elevator descent → desk freed → next queued agent
```

See [AGENTS.md](AGENTS.md) for development instructions.

---
*Built with [beads](https://github.com/Fission-AI/beads) + [Claude API](https://anthropic.com)*
