## 1. Project Scaffold

- [ ] 1.1 Create Maven/Gradle project with Paper API dependency (1.20.x)
- [ ] 1.2 Set up `plugin.yml` with name, main class, commands (`office`), and permissions
- [ ] 1.3 Create `CLAUDE.md` and `AGENTS.md` with build instructions (`./gradlew build`)
- [ ] 1.4 Configure GitHub Actions CI: build + unit test on push

## 2. Plugin Config

- [ ] 2.1 Create `PluginConfig` class that reads and validates `config.yml` on startup
- [ ] 2.2 Implement startup failure with clear error messages for missing required keys
- [ ] 2.3 Add default `config.yml` resource with commented schema
- [ ] 2.4 Implement `/office reload` command that re-reads config without server restart

## 3. Beads Integration

- [ ] 3.1 Create `BeadsClient` interface with `listInProgress()` and `listAll()` methods
- [ ] 3.2 Implement `BeadsCliClient` using `ProcessBuilder` to shell out to `bd`
- [ ] 3.3 Parse `bd list --json` output into `BeadsTask` POJOs
- [ ] 3.4 Implement `BeadsPoller` as a `BukkitRunnable` that fires `AgentSpawnEvent` / `AgentDespawnEvent` on delta
- [ ] 3.5 Handle `bd` CLI errors and timeouts gracefully (log + skip cycle)
- [ ] 3.6 On plugin enable, run immediate poll and fire spawn events with `isRestore=true` for existing tasks

## 4. Claude Paraphrase

- [ ] 4.1 Add Anthropic HTTP client dependency (OkHttp or Java 11 HttpClient)
- [ ] 4.2 Implement `ClaudeParaphraser` that calls Messages API with `claude-haiku-4-5`
- [ ] 4.3 Prompt: "Summarise this task in ≤10 words, first-person present tense: {title} — {description}"
- [ ] 4.4 Implement async `CompletableFuture<String>` wrapper (non-blocking main thread)
- [ ] 4.5 Add LRU cache keyed by task ID + title hash; return cached value on hit
- [ ] 4.6 Fallback to raw task title on API error or timeout

## 5. Office Layout

- [ ] 5.1 Create `DeskConfig` and `OfficeLayout` data classes loaded from `config.yml`
- [ ] 5.2 Implement `DeskRegistry` tracking which desks are free/occupied and by which task ID
- [ ] 5.3 Implement desk assignment: nearest free desk to elevator on `AgentSpawnEvent`
- [ ] 5.4 Implement queue: if no free desk, hold NPC at elevator lobby until desk frees
- [ ] 5.5 Implement `/office setup auto <rows> <cols>` command to generate desk grid
- [ ] 5.6 Implement `/office visualise` command — particle markers at desks and elevator for 10s

## 6. Elevator

- [ ] 6.1 Define elevator positions (base, top, lobby) from config
- [ ] 6.2 Implement ascent animation: teleport NPC 1 block/tick for `elevator.height` ticks
- [ ] 6.3 Implement descent animation: same mechanic in reverse
- [ ] 6.4 Toggle trapdoor block state open/closed on NPC arrival and departure
- [ ] 6.5 Skip elevator animation when `isRestore=true` (direct-to-desk spawn)

## 7. Agent NPC

- [ ] 7.1 Implement `AgentNpc` wrapping an ArmorStand entity with `setCustomNameVisible(true)`
- [ ] 7.2 Name format: `[<assignee>] <task title>` at spawn; update async when paraphrase resolves
- [ ] 7.3 Implement `AgentRegistry` map of task ID → `AgentNpc`; prevent duplicate spawns
- [ ] 7.4 Wire `AgentSpawnEvent` → elevator entry → desk assignment → NPC sits at desk
- [ ] 7.5 Wire `AgentDespawnEvent` → elevator exit animation → entity removal → desk freed
- [ ] 7.6 Implement right-click interaction: send full task details to clicking player

## 8. Team Leader NPC

- [ ] 8.1 Implement `TeamLeaderNpc` as a permanent ArmorStand at `team-leader.position`
- [ ] 8.2 Spawn team leader on plugin enable; ensure it survives task churn
- [ ] 8.3 Implement broadcast timer: every `broadcast-interval-seconds`, send status line to nearby players (≤32 blocks)
- [ ] 8.4 Format status message: active count + each agent's paraphrase inline
- [ ] 8.5 Emit "office is quiet" message when no active agents
- [ ] 8.6 Implement right-click: send full task list (all statuses, grouped) to clicking player

## 9. Commands & Permissions

- [ ] 9.1 Register `/office` command with subcommands: `reload`, `setup`, `visualise`, `status`
- [ ] 9.2 Add `agentoffice.admin` permission required for setup and reload
- [ ] 9.3 Add `/office status` command showing current desk occupancy and active agent list in chat

## 10. Testing & Polish

- [ ] 10.1 Unit tests for `BeadsCliClient` JSON parsing with fixture data
- [ ] 10.2 Unit tests for `ClaudeParaphraser` cache logic
- [ ] 10.3 Unit tests for `DeskRegistry` assignment and queue logic
- [ ] 10.4 Manual integration test checklist in `TESTING.md`
- [ ] 10.5 Add `README.md` with installation, config reference, and screenshot placeholder
