## ADDED Requirements

### Requirement: Paraphrase task via Claude API
The `ClaudeParaphraser` SHALL call the Anthropic Messages API using `claude-haiku-4-5` to convert a beads task title and description into a short, first-person, present-tense phrase of ≤ 10 words suitable for a floating name tag.

#### Scenario: Successful paraphrase
- **WHEN** a task with title "Refactor auth middleware for compliance" is sent to the paraphraser
- **THEN** the returned string is ≤ 10 words, first-person, present-tense (e.g. "Refactoring the auth middleware for compliance")

#### Scenario: API call is asynchronous
- **WHEN** `ClaudeParaphraser.paraphrase(task)` is called
- **THEN** it returns a `CompletableFuture<String>` and does NOT block the main thread

#### Scenario: API error fallback
- **WHEN** the Claude API returns an error or times out (> 5 seconds)
- **THEN** the raw task title is used as the NPC label and the error is logged as a warning

### Requirement: Result caching
The paraphraser SHALL cache results keyed by task ID. If the task title has not changed since the last call, the cached result SHALL be returned without making a new API call.

#### Scenario: Cache hit
- **WHEN** `paraphrase` is called for a task whose title has not changed since last call
- **THEN** the cached string is returned immediately without an HTTP request
