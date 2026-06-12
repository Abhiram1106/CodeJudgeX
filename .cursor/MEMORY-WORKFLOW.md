# Memory Workflow — CodeJudgeX (Cursor)

> Execute this shutdown ritual at the end of every chat where files were changed,
> bugs were fixed, or decisions were made. Do not abbreviate. Do not skip steps.

---

## Two categories of shutdown — different rules for each

### VAULT SHUTDOWN — AUTOMATIC, NO CONSENT NEEDED

Commit and push vault files at the end of every chat without asking the user.
This is mandatory. No exceptions. No "should I push?" No waiting.

Vault files = `.obsidian-ai-memory/`, `AGENTS.md`, `CLAUDE.md`, `.claude/`, `.cursor/`

```bash
git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md .claude/ .cursor/
git commit -m "memory: YYYY-MM-DD HH:MM cursor — <one-line summary>"
git push origin HEAD
```

### CODE SHUTDOWN — REQUIRES USER CONSENT

Application files (`backend/`, `frontend/`, `infra/`, `docs/`) are NEVER committed automatically.
Always ask the user explicitly before committing code — the push is not a separate ask.

```bash
# Only after user says yes:
git add backend/ frontend/ infra/ docs/
git commit -m "feat|fix|refactor|docs(scope): description"
git push origin HEAD   # same turn, no extra confirmation
```

Commit and push are one atomic step. Don't stop after `git commit` to ask
"want me to push?" — that's already covered by the commit consent.

---

## The shutdown steps (do these before committing anything)

### Step 1 — Write session digest

Create `.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-cursor.md`

Use template at `.obsidian-ai-memory/templates/session-digest.md`. Fill every field.
Include: files changed, decisions made, errors encountered, assumptions, next 3 tasks.

### Step 2 — Overwrite session-continuity.md

Overwrite (not append) `.obsidian-ai-memory/02-PROJECTS/session-continuity.md`

Must contain:
- Where we left off (2–3 sentences)
- Active thread (what is in progress right now)
- Current week goal
- Verification state (tests passing / failing / not run)
- Next 3 concrete tasks
- Open risks or blockers

### Step 3 — Update active-goals.md

Check off completed tasks. Add any new tasks discovered this session.

### Step 4 — Append to error-memory.md if a bug was fixed

Use template at `.obsidian-ai-memory/templates/error-entry.md`.
Append only — never overwrite. Error history is permanent.

### Step 5 — Append to decisions.md if a non-trivial decision was made

Use template at `.obsidian-ai-memory/templates/decision-entry.md`.
Append to `.obsidian-ai-memory/04-DECISIONS/decisions.md`.

### Step 6 — Update architecture docs if structure changed

If modules, packages, data flows, or technology choices changed, update `.obsidian-ai-memory/05-ARCHITECTURE/`.

### Step 7 — VAULT COMMIT + PUSH (automatic, no consent)

```bash
git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md .claude/ .cursor/
git commit -m "memory: YYYY-MM-DD HH:MM cursor — <summary>"
git push origin HEAD
```

Do not ask. Do not wait. Execute immediately.

### Step 8 — CODE COMMIT (ask user first)

Ask: *"Ready to commit the code changes?"*

If yes — commit AND push together, no separate push confirmation:
```bash
git add backend/ frontend/ infra/ docs/
git commit -m "feat|fix|refactor|docs(scope): description"
git push origin HEAD
```

### Step 9 — Final reply (digest + memory block)

Every final reply MUST start with a **full session digest** for the user, followed by the `## Memory` block.

**Session digest format (required, output directly in reply — not just in vault):**

```markdown
## Session Digest — YYYY-MM-DD

### ✅ What was done this session
- <every file written or changed, test run, bug fixed, decision made>

### 🔧 What still needs to be done
- <unchecked tasks from active-goals.md relevant to current week>

### 🧪 What you should test / verify manually
- <real curl commands, browser URLs, Docker log greps — be specific>
- Example: POST /api/v1/auth/register → expect 201 + access token in response
- Example: docker compose logs backend | grep "Flyway" → expect "Successfully applied N migrations"

### ⚠️ Open risks / known issues
- <anything incomplete, broken, or requiring attention before next step>

### 📋 Decisions made
- <non-trivial choices this session — what and why>

### 🚀 Recommended next step
<one specific sentence — exact file or command to start with next session>
```

Rules:
- "What was done" lists every file touched — not just module names
- "What to test" must be actionable — real commands, not vague descriptions
- "Open risks" must be honest — if something is broken, say so
- Do NOT skip this because the session was short or "simple"

Then append the memory block:

```markdown
## Memory
- Digest: .obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-cursor.md
- Vault: committed + pushed ✓ (automatic)
- Code commit: <hash> — <message>   |   pending user consent
- Next task: <one concrete next step from active-goals.md>
```

---

## What never goes in the vault

- Raw source code dumps
- Secrets, API keys, passwords, connection strings with credentials
- Absolute paths to the user's home directory
- Full stack traces (summarise and link to the session digest)
- Speculation ("we might want to...")
- Duplicates of existing entries
