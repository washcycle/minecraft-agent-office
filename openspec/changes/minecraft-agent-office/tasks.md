## 1. Project Scaffold

- [x] 1.1 Create Maven/Gradle project with Paper API dependency (1.20.x)
- [x] 1.2 Set up `plugin.yml` with name, main class, commands (`office`), and permissions
- [x] 1.3 Create `CLAUDE.md` and `AGENTS.md` with build instructions (`./gradlew build`)
- [x] 1.4 Configure GitHub Actions CI: build + unit test on push

## 2. Plugin Config

- [x] 2.1 Create `PluginConfig` class that reads and validates `config.yml` on startup
- [x] 2.2 Implement startup failure with clear error messages for missing required keys
- [x] 2.3 Add default `config.yml` resource with commented schema
- [x] 2.4 Implement `/office reload` command that re-reads config without server restart

## 3. Beads Integration

- [x] 3.1 Create `BeadsClient` interface with `listInProgress()` and `listAll()` methods
- [x] 3.2 Implement `BeadsCliClient` using `ProcessBuilder` to shell out to `bd`
- [x] 3.3 Parse `bd list --json` output into `BeadsTask` POJOs
- [x] 3.4 Implement `BeadsPoller` as a `BukkitRunnable` that fires `AgentSpawnEvent` / `AgentDespawnEvent` on delta
- [x] 3.5 Handle `bd` CLI errors and timeouts gracefully (log + skip cycle)
- [x] 3.6 On plugin enable, run immediate poll and fire spawn events with `isRestore=true` for existing tasks

## 4. Claude Paraphrase

- [x] 4.1 Add Anthropic HTTP client dependency (OkHttp or Java 11 HttpClient)
- [x] 4.2 Implement `ClaudeParaphraser` that calls Messages API with `claude-haiku-4-5`
- [x] 4.3 Prompt: "Summarise this task in ≤10 words, first-person present tense: {title} — {description}"
- [x] 4.4 Implement async `CompletableFuture<String>` wrapper (non-blocking main thread)
- [x] 4.5 Add LRU cache keyed by task ID + title hash; return cached value on hit
- [x] 4.6 Fallback to raw task title on API error or timeout

## 5. Office Layout

- [x] 5.1 Create `DeskConfig` and `OfficeLayout` data classes loaded from `config.yml`
- [x] 5.2 Implement `DeskRegistry` tracking which desks are free/occupied and by which task ID
- [x] 5.3 Implement desk assignment: nearest free desk to elevator on `AgentSpawnEvent`
- [x] 5.4 Implement queue: if no free desk, hold NPC at elevator lobby until desk frees
- [x] 5.5 Implement `/office setup auto <rows> <cols>` command to generate desk grid
- [x] 5.6 Implement `/office visualise` command — particle markers at desks and elevator for 10s

## 6. Elevator

- [x] 6.1 Define elevator positions (base, top, lobby) from config
- [x] 6.2 Implement ascent animation: teleport NPC 1 block/tick for `elevator.height` ticks
- [x] 6.3 Implement descent animation: same mechanic in reverse
- [x] 6.4 Toggle trapdoor block state open/closed on NPC arrival and departure
- [x] 6.5 Skip elevator animation when `isRestore=true` (direct-to-desk spawn)

## 7. Agent NPC

- [x] 7.1 Implement `AgentNpc` wrapping an ArmorStand entity with `setCustomNameVisible(true)`
- [x] 7.2 Name format: `[<assignee>] <task title>` at spawn; update async when paraphrase resolves
- [x] 7.3 Implement `AgentRegistry` map of task ID → `AgentNpc`; prevent duplicate spawns
- [x] 7.4 Wire `AgentSpawnEvent` → elevator entry → desk assignment → NPC sits at desk
- [x] 7.5 Wire `AgentDespawnEvent` → elevator exit animation → entity removal → desk freed
- [x] 7.6 Implement right-click interaction: send full task details to clicking player

## 8. Team Leader NPC

- [x] 8.1 Implement `TeamLeaderNpc` as a permanent ArmorStand at `team-leader.position`
- [x] 8.2 Spawn team leader on plugin enable; ensure it survives task churn
- [x] 8.3 Implement broadcast timer: every `broadcast-interval-seconds`, send status line to nearby players (≤32 blocks)
- [x] 8.4 Format status message: active count + each agent's paraphrase inline
- [x] 8.5 Emit "office is quiet" message when no active agents
- [x] 8.6 Implement right-click: send full task list (all statuses, grouped) to clicking player

## 9. Commands & Permissions

- [x] 9.1 Register `/office` command with subcommands: `reload`, `setup`, `visualise`, `status`
- [x] 9.2 Add `agentoffice.admin` permission required for setup and reload
- [x] 9.3 Add `/office status` command showing current desk occupancy and active agent list in chat

## 10. Testing & Polish

- [x] 10.1 Unit tests for `BeadsCliClient` JSON parsing with fixture data
- [x] 10.2 Unit tests for `ClaudeParaphraser` cache logic
- [x] 10.3 Unit tests for `DeskRegistry` assignment and queue logic
- [x] 10.4 Manual integration test checklist in `TESTING.md`
- [x] 10.5 Add `README.md` with installation, config reference, and screenshot placeholder
