# Claude Code Instructions

## Project Overview

Minecraft Agent Office — a Minecraft plugin where Claude agents (backed by beads tasks) inhabit a virtual office. Agents enter via elevator, sit at desks, paraphrase their current task, and leave when done. A team leader NPC monitors the group and emits status from the beads task list.

## Task Tracking

**Always use beads for task tracking.** This project uses the `bd` CLI.

- **Never** use TodoWrite, TaskCreate, or markdown files for task tracking
- **Always** create a beads issue before writing code: `bd create --title="..." --description="..." --type=task|bug|feature --priority=2`
- Mark in progress when starting: `bd update <id> --status=in_progress`
- Close when done: `bd close <id>`
- Check available work: `bd ready`

Priority scale: 0=critical, 1=high, 2=medium, 3=low, 4=backlog

## PR Title Convention

All PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): description`

Allowed types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`, `build`, `ci`, `style`, `revert`

## Session End

Before finishing any session:
1. `git add <files>` — stage changes
2. `git commit -m "type: description"` — conventional commit
3. `git push` — push to remote
