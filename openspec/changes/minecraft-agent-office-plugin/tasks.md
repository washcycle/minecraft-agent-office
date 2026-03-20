## 1. Project Scaffold

- [ ] 1.1 Initialize Gradle project under `plugin/` with Java 21 and Paper 1.21.1 API dependency
- [ ] 1.2 Add Citizens2, PacketEvents, and Anthropic Java SDK dependencies to `build.gradle`
- [ ] 1.3 Create `plugin.yml` with main class, name, version, and depend on Citizens
- [ ] 1.4 Create `config.yml` with `bd_path`, `standup_interval_minutes`, and office coordinate settings
- [ ] 1.5 Create main plugin class with `onEnable`/`onDisable` lifecycle hooks

## 2. bd Bridge

- [ ] 2.1 Implement `BdPoller` with configurable bd CLI path and scheduled async polling
- [ ] 2.2 Implement polling for `in_progress` and `blocked` tasks (5s interval)
- [ ] 2.3 Implement polling for `open`/`ready` tasks (10s interval) and `bd stats` (30s interval)
- [ ] 2.4 Implement internal task state diffing to detect status transitions between polls
- [ ] 2.5 Define and emit `TaskClaimedEvent`, `TaskBlockedEvent`, `TaskClosedEvent` internal events
- [ ] 2.6 Add startup check for bd CLI availability with console warning if not found

## 3. Team State Manager

- [ ] 3.1 Implement `TeamStateManager` to maintain taskId → NPC and desk → task mappings
- [ ] 3.2 Wire `TaskClaimedEvent` to trigger NPC spawn + desk assignment
- [ ] 3.3 Wire `TaskBlockedEvent` to switch NPC to STUCK state
- [ ] 3.4 Wire `TaskClosedEvent` to trigger NPC departure sequence
- [ ] 3.5 Implement desk lobby queue for when all desks are occupied

## 4. NPC Controller (Citizens2)

- [ ] 4.1 Implement `NPCController` wrapping Citizens2 API for spawn/despawn at elevator position
- [ ] 4.2 Implement desk assignment and NPC pathfinding to desk via Citizens2 waypoints
- [ ] 4.3 Implement elevator arrival animation (spawn + walk) with ding sound
- [ ] 4.4 Implement departure sequence (save-work animation + walk to elevator + despawn)
- [ ] 4.5 Implement Citizens2 trait for desk personality (stores personality enum on NPC)
- [ ] 4.6 Implement head tracking toward monitor in WORKING state
- [ ] 4.7 Implement head-down slump in STUCK state

## 5. NPC Behavior Loop

- [ ] 5.1 Implement per-NPC independent behavior timer with jittered interval (70–110 ticks base + 0–40 jitter)
- [ ] 5.2 Implement weighted behavior roll table (typing burst, glance, coffee run, lean back, bubble, player ack)
- [ ] 5.3 Implement coffee run pathfinding (NPC walks to coffee machine, increments counter, returns)
- [ ] 5.4 Implement coworker glance (head tracking toward adjacent NPC briefly)
- [ ] 5.5 Implement player proximity detection (8 block radius) for acknowledgement trigger
- [ ] 5.6 Increase coffee run weight to 20% in STUCK state behavior table

## 6. Bubble Engine

- [ ] 6.1 Implement `BubbleEngine` with per-NPC 45-second cooldown tracking
- [ ] 6.2 Implement proximity check (8 blocks) and 15% probability roll before firing
- [ ] 6.3 Implement async Claude API call (claude-haiku-4-5) with personality + task title prompt
- [ ] 6.4 Implement 3-second timeout with fallback to personality phrase bank
- [ ] 6.5 Write 10+ fallback phrases for each of the 4 desk personalities
- [ ] 6.6 Implement Citizens2 speech trait to display bubble above NPC head
- [ ] 6.7 Ensure last-used fallback phrase is excluded from next random selection

## 7. Scrum Board

- [ ] 7.1 Implement `BoardRenderer` that manages Text Display entity lifecycle for all board cards
- [ ] 7.2 Define board column positions in config (world coordinates for each column header)
- [ ] 7.3 Implement card rendering with priority color formatting (red/gold/yellow/green/gray)
- [ ] 7.4 Implement card movement between columns on task status change
- [ ] 7.5 Implement blocked card flashing (toggle color every 10 ticks via metadata packet)
- [ ] 7.6 Implement in-progress card NPC name display ("● The Veteran")
- [ ] 7.7 Implement particle burst effect (happy villager particles) on task close
- [ ] 7.8 Implement board cleanup and re-render on plugin reload

## 8. Conference Room — SceneDirector

- [ ] 8.1 Implement `SceneDirector` singleton with scene queue (max depth 2)
- [ ] 8.2 Implement scene lock preventing concurrent scenes
- [ ] 8.3 Implement NPC gather logic (issue move commands, wait up to 10s for arrivals)
- [ ] 8.4 Implement post-scene return (all NPCs pathfind back to saved desk positions)
- [ ] 8.5 Implement standup interval scheduler (configurable, default 20 min)

## 9. Conference Room — Standup Scene

- [ ] 9.1 Implement team leader NPC standup trigger dialog ("Alright everyone, sync time.")
- [ ] 9.2 Implement per-NPC status update dialog generation (Claude API with personality + current task)
- [ ] 9.3 Implement team leader summary line after all NPCs speak
- [ ] 9.4 Implement team leader head turn toward sprint board during summary
- [ ] 9.5 Implement sprint board Text Display (goal, progress bar, velocity, blocked count)

## 10. Conference Room — Delivery Scene

- [ ] 10.1 Implement delivery trigger on `TaskClosedEvent` for priority 0/1 or epic tasks
- [ ] 10.2 Implement completing NPC walk to team leader + delivery dialog
- [ ] 10.3 Implement team leader "Conference room" callout and NPC gather
- [ ] 10.4 Implement whiteboard map art showing completed task title and time elapsed
- [ ] 10.5 Implement audience NPC personality reactions (one per desk, scripted by personality)
- [ ] 10.6 Implement "Ship it." team leader closing line
- [ ] 10.7 Implement major ship celebration (P0/epic): confetti particles + note block fanfare + "back to work" line

## 11. Environmental Humor Objects

- [ ] 11.1 Implement coffee machine visit counter sign (updates on each NPC coffee run)
- [ ] 11.2 Implement midnight reset for coffee counter
- [ ] 11.3 Implement days-since-blocked sign with record tracking (persisted to plugin data folder)
- [ ] 11.4 Implement sign reset on `TaskBlockedEvent`
- [ ] 11.5 Implement sprint vs. actual whiteboard (updates on stats poll)
- [ ] 11.6 Implement red formatting on whiteboard when opened > closed
- [ ] 11.7 Implement velocity wall bar chart using wool blocks (7-day history, updates on task close)

## 12. Audio Layer

- [ ] 12.1 Add elevator arrival sound (BLOCK_NOTE_BLOCK_PLING) on NPC spawn
- [ ] 12.2 Add ambient typing sounds (BLOCK_NOTE_BLOCK_HAT) during typing burst micro-behavior
- [ ] 12.3 Add task complete chime (UI_TOAST_CHALLENGE_COMPLETE) on task close
- [ ] 12.4 Add blocked sad note (BLOCK_NOTE_BLOCK_BASS) on task blocked
- [ ] 12.5 Add conference room marker sound (BLOCK_NOTE_BLOCK_CHIME) during whiteboard presentation
- [ ] 12.6 Add major ship fanfare sequence (multi-note BLOCK_NOTE_BLOCK_BELL pattern)

## 13. Testing & Polish

- [ ] 13.1 Write unit tests for BdPoller state diffing logic
- [ ] 13.2 Write unit tests for BubbleEngine cooldown and probability logic
- [ ] 13.3 Test NPC pathfinding in office layout (verify Citizens2 waypoints reach all desks)
- [ ] 13.4 Test scrum board rendering with 20+ simultaneous tasks
- [ ] 13.5 Test conference room scene with all 4 desks occupied
- [ ] 13.6 Test bd CLI not found startup warning
- [ ] 13.7 Verify plugin runs on both Windows and macOS Paper servers
- [ ] 13.8 Write `README.md` with setup instructions (bd path config, office world setup, Citizens2 install)
