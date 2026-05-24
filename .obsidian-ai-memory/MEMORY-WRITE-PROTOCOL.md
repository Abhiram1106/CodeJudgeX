---
type: protocol
updated: 2026-05-23
tags: [protocol, memory, write]
---

# Memory Write Protocol — CodeJudgeX

> Defines WHAT gets written to the vault, WHEN, and HOW.
> Follow this at the end of every meaningful session.

## When to write a digest

**Write when ANY of these are true:**
- A file was created or meaningfully modified
- A bug was fixed
- A non-trivial decision was made
- The session lasted more than 15 minutes

**Skip only for:**
- Pure read-only sessions (no edits, no fixes, no decisions)
- One-liner answers

## File rules by type

### 01-SESSIONS/ — session digests (APPEND-ONLY by creating new files)

- **Never** edit a past digest — history is permanent
- File path: `01-SESSIONS/YYYY-MM-DD/session-HHMM-<tool>.md`
- Tool suffixes: `claude`, `cursor`, `windsurf`, `cline`, `copilot`
- Use template: `templates/session-digest.md`
- Fill EVERY field — write "N/A" if not applicable, not blank

### 02-PROJECTS/session-continuity.md — rolling handoff (OVERWRITE each session)

- **Always overwrite** — this file represents "right now", not history
- Must contain: where we left off, active thread, week goal, verification state, next 3 tasks, open risks
- The next session reads this FIRST — make it accurate and actionable

### 02-PROJECTS/active-goals.md — goal tracking (EDIT checkboxes)

- Check `[x]` when goals are completed
- Add new goals when scope expands
- Never remove an uncompleted goal without explicit user instruction

### 03-ERRORS/error-memory.md — bug log (APPEND-ONLY)

- Use template: `templates/error-entry.md`
- Append at the bottom — never edit past entries
- Every fixed bug gets an entry, no exceptions
- Include: symptom, root cause, fix, prevention rule

### 03-ERRORS/anti-patterns.md — prevention rules (APPEND when promoted)

- Append when a pattern has caused 2+ bugs or is severe enough to warrant a standing rule
- Format: one-line rule + one-line rationale
- Never remove rules — mark as `[SUPERSEDED]` if no longer applicable

### 04-DECISIONS/decisions.md — decision log (APPEND-ONLY)

- Use template: `templates/decision-entry.md`
- Append new decisions — never edit past decisions
- Mark as `[SUPERSEDED by D-NNN]` if reversed

### 05-ARCHITECTURE/ — system design (EDIT in place)

- Update when the architecture actually changes (new module, new dependency, changed flow)
- Do NOT update for speculative plans — only for decisions already implemented or formally decided

## What NEVER goes in the vault

- Raw source code dumps
- Secrets, API keys, passwords, connection strings with credentials
- Absolute paths to the user's home directory (`C:\Users\ADMIN\...` → use relative paths)
- Full stack traces — summarise in 2–3 lines, link to the session where they appeared
- Speculation framed as fact ("we will add X" → "plan: add X, not yet decided")
- Duplicate entries — check before appending

## Two-commit rule

Code changes and vault changes are ALWAYS separate commits:

```bash
# Commit 1 — application code
git add backend/ frontend/ infra/ docs/ Makefile README.md AGENTS.md CLAUDE.md .cursor/ .claude/ .gitignore
git commit -m "feat|fix|refactor|docs: <description>"

# Commit 2 — memory vault
git add .obsidian-ai-memory/
git commit -m "memory: YYYY-MM-DD <tool> — <one-line summary>"
```

`git log --grep="memory:"` must reconstruct every session handoff cleanly.
Application git history must never be polluted with vault updates.
