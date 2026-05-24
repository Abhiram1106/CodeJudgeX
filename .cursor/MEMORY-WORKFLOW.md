# Memory Workflow — CodeJudgeX (Cursor)

> Execute this shutdown ritual at the end of every chat where files were changed,
> bugs were fixed, or decisions were made. Do not abbreviate. Do not skip steps.

## When to run this

Run when ANY of these are true:
- A file was created or modified
- A bug was fixed
- A non-trivial decision was made
- The chat lasted more than 15 minutes

Skip only for: pure read-only sessions, one-liner answers, exploratory reading.

---

## The 10-step shutdown ritual

### Step 1 — Write session digest

Create `.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-cursor.md`

Use the template at `.obsidian-ai-memory/templates/session-digest.md`. Fill every field.
Do not leave fields blank — write "N/A" if not applicable.

### Step 2 — Overwrite session-continuity.md

Overwrite (not append) `.obsidian-ai-memory/02-PROJECTS/session-continuity.md`

This is the first file the next chat reads. It must contain:
- Where we left off (2–3 sentences)
- Active thread (max 5 bullet points — the things in progress right now)
- Current week goal (one line)
- Verification state (tests passing / failing / not run)
- Next 3 concrete tasks
- Open risks or blockers

### Step 3 — Update active-goals.md if checkboxes changed

If any goals were completed or new ones were added, update `.obsidian-ai-memory/02-PROJECTS/active-goals.md`.

### Step 4 — Append to error-memory.md if a bug was fixed

Use the template at `.obsidian-ai-memory/templates/error-entry.md`.
Append — never overwrite. Error history is permanent.

### Step 5 — Append to decisions.md if a non-trivial choice was made

Use the template at `.obsidian-ai-memory/templates/decision-entry.md`.
Append to `.obsidian-ai-memory/04-DECISIONS/decisions.md`.

### Step 6 — Update architecture docs if structure changed

If modules, packages, data flows, or technology choices changed, update the relevant file in `.obsidian-ai-memory/05-ARCHITECTURE/`.

### Step 7 — Code commit

Stage only application code files (not vault files).

```
git add backend/ frontend/ infra/ docs/ Makefile README.md AGENTS.md CLAUDE.md .cursor/ .claude/ .gitignore
git commit -m "feat|fix|refactor|docs: <concise description>"
```

Conventional commit prefixes: `feat:` `fix:` `refactor:` `docs:` `test:` `chore:`

### Step 8 — Memory commit

Stage only vault files. Separate from the code commit — always.

```
git add .obsidian-ai-memory/
git commit -m "memory: YYYY-MM-DD cursor — <one-line summary of what the session did>"
```

`git log --grep="memory:"` must cleanly reconstruct every session handoff.

### Step 9 — Push

```
git push origin HEAD
```

Ask the user before pushing if there is any doubt. If the user declines, note it in the digest.

### Step 10 — Final reply

The final reply in the chat MUST include a `## Memory` block:

```markdown
## Memory

- Digest: `.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-cursor.md`
- Code commit: `<hash> — <message>`
- Memory commit: `<hash> — memory: ...`
- Push: pushed to origin / declined by user / skipped (reason)
- Continuity: session-continuity.md updated
- Next task: <one concrete next step>
```

---

## What never goes in the vault

- Raw source code dumps
- Secrets, API keys, passwords, connection strings with credentials
- Absolute paths to the user's home directory
- Full stack traces (summarise and link to the session digest)
- Speculation ("we might want to...")
- Duplicates of existing entries
