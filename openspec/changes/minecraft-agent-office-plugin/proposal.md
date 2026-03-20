## Why

Real Claude agent team work is invisible — tasks are claimed, worked, and closed in terminals with no ambient sense of a team actually collaborating. This plugin makes that work visible and delightful by rendering it as a living office in Minecraft, where players can walk around and watch NPC agents do real work in real time.

## What Changes

- **New**: Paper 1.21.1 Minecraft plugin (`minecraft-agent-office-plugin`)
- **New**: NPC agents with unique desk personalities that mirror live beads task states
- **New**: Elevator arrival/departure lifecycle tied to task claim/close events
- **New**: Chat bubble engine — Claude API paraphrases real task titles into office-worker humor
- **New**: Scrum board wall with 4 columns (Backlog / Ready / In Progress / Done) driven by bd JSON output
- **New**: Conference room scenes — standup (periodic) and delivery (task completion event)
- **New**: SceneDirector for multi-NPC coordination (gather, sequence, dismiss, return)
- **New**: Environmental humor objects: coffee counter, incident sign, velocity wall, sprint vs. actual whiteboard
- **New**: Audio layer — note block ambience, event sounds, fanfare on major ship

## Capabilities

### New Capabilities

- `npc-lifecycle`: NPC agents spawn at elevator, walk to desks, work, and depart — all driven by bd task status transitions
- `npc-behavior`: Per-NPC micro-behaviors (coffee runs, glances, typing bursts, proximity reactions) with independent jittered timers and desk personality voices
- `bubble-engine`: Claude API integration that paraphrases task titles into funny office-worker chat bubbles with cooldown and proximity gating
- `scrum-board`: Live Text Display entity board reading bd task data — 4 columns, priority color coding, blocked flashing, particle effects on completion
- `conference-room`: Scripted multi-NPC scenes triggered by standup interval or task delivery events, with personality-driven dialog and sprint board gestures
- `bd-bridge`: Read-only polling layer that translates bd CLI JSON output into plugin-internal task state events
- `environmental-humor`: Static and dynamic world objects (coffee counter, days-since-blocked sign, velocity wall) that surface bd data as office jokes

### Modified Capabilities

## Impact

- New Gradle/Java 21 project under `plugin/`
- Dependencies: Paper 1.21.1 API, Citizens2, PacketEvents, Anthropic Java SDK (for Claude API)
- Requires bd CLI available on server PATH
- Reads `~/.claude/teams/` and bd task JSON — no writes to either
- Players connect with standard Java Edition client — no client mods required
