# Omnix Commands — CodeJudgeX

> Project-specific CLI commands. These extend the default `omnix` command set
> for CodeJudgeX-specific operations.

---

## session-digest

**Usage:** `omnix session-digest --tool <tool-name>`
**Auto flag:** `omnix session-digest --auto --tool claude`

Generates a session digest from git diff and writes it to:
`backend/src/main/resources/db/migration/`
`.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-<tool>.md`

**When to run:** At the end of any session where files were meaningfully changed.
**Skip for:** Read-only sessions, one-liner answers, exploratory searches.

**Required fields in digest:**
- `week-goal` — current week's focus from `active-goals.md`
- Files created / changed (table format)
- Decisions made (if any)
- Errors encountered (if any)
- Next recommended step

---

## evaluate-submission

**Usage:** `omnix evaluate-submission --id <submission-uuid>`

Triggers a manual re-evaluation for a stuck or failed submission.
Publishes directly to `evaluation.queue` via the RabbitMQ management API.

**When to use:** Submission is stuck in QUEUED or RUNNING after 5 minutes.
**Do not use:** For submissions with terminal status (ACCEPTED, WRONG_ANSWER, etc.).

---

## migration-check

**Usage:** `omnix migration-check`

Validates all Flyway migration files in
`backend/src/main/resources/db/migration/` against the naming and content rules:

- `V{n}__{description}.sql` naming enforced
- No DML mixed with DDL
- Every file has a comment header block
- No modification of files with version numbers already applied to the DB

---

## typecheck

**Usage:** `omnix typecheck`

Runs `npm run typecheck` in `frontend/` and `./mvnw compile` in `backend/`.
Reports errors from both in a unified summary.

**Run before:** Every commit that touches both frontend and backend.

---

## infra-status

**Usage:** `omnix infra-status`

Checks that all required Docker services are running:
- PostgreSQL 16 (port 5432)
- Redis 7 (port 6379)
- RabbitMQ 3 (port 5672, management 15672)
- Judge0 CE (port 2358)
- Prometheus (port 9090)
- Grafana (port 3001)

Reports which services are up/down and their health status.

---

## Adding new commands

Create a `.md` spec file in this directory:

```
.omnix/commands/my-command.md
```

The file should describe:
1. What the command does
2. Arguments and flags
3. When to use it
4. What it should NOT be used for
5. Expected output or side effects
