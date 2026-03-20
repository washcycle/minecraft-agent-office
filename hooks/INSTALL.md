# Installing the Claude Office Hook

This hook keeps `~/.claude/office-sessions.json` up to date so the AgentOffice
Minecraft plugin can display which projects have active Claude Code sessions.

---

## 1. Copy the script

```bash
mkdir -p ~/.claude/hooks
cp hooks/claude-office-hook ~/.claude/hooks/claude-office-hook
```

## 2. Make it executable

```bash
chmod +x ~/.claude/hooks/claude-office-hook
```

## 3. Register the hooks in `~/.claude/settings.json`

Open `~/.claude/settings.json` and add (or merge into) the `hooks` key:

```json
"hooks": {
  "SessionStart": [
    {
      "type": "command",
      "command": "/home/YOUR_USERNAME/.claude/hooks/claude-office-hook SessionStart"
    }
  ],
  "Stop": [
    {
      "type": "command",
      "command": "/home/YOUR_USERNAME/.claude/hooks/claude-office-hook Stop"
    }
  ]
}
```

Replace `YOUR_USERNAME` with your actual Linux username (e.g. `washcycle`).

> **If you already have entries under `SessionStart` or `Stop`**, append to the
> existing array rather than replacing it. Each event key accepts a list of
> commands that are all executed in order.

A minimal complete `settings.json` looks like this:

```json
{
  "hooks": {
    "SessionStart": [
      {
        "type": "command",
        "command": "/home/YOUR_USERNAME/.claude/hooks/claude-office-hook SessionStart"
      }
    ],
    "Stop": [
      {
        "type": "command",
        "command": "/home/YOUR_USERNAME/.claude/hooks/claude-office-hook Stop"
      }
    ]
  }
}
```

## 4. Verify the installation

Open a new `claude` session in any project directory, then inspect the sessions
file:

```bash
cat ~/.claude/office-sessions.json
```

You should see an entry for the project you just opened, for example:

```json
{
  "sessions": [
    {
      "id": "abc-123",
      "project": "my-project",
      "path": "/home/YOUR_USERNAME/dev/my-project",
      "started": "2026-03-19T21:40:00Z"
    }
  ]
}
```

When you close the `claude` session the entry will be removed automatically.

---

## Notes

- The hook script never writes to stdout or stderr and always exits 0, so it
  cannot interfere with Claude's conversation.
- The sessions file is written atomically (`.tmp` → rename) to prevent the
  Minecraft plugin from reading a partial file.
- The plugin polls `~/.claude/office-sessions.json` every 5 seconds, so there
  may be up to a 5-second delay before the in-game display updates.
