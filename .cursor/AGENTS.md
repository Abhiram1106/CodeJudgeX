# Cursor Adapter — CodeJudgeX

> Cursor-specific startup and shutdown. Full engineering rules live in the root AGENTS.md.

## Startup (every chat, no exceptions)

Before any response or edit:

1. Read `.obsidian-ai-memory/02-PROJECTS/session-continuity.md`
2. Read `.obsidian-ai-memory/02-PROJECTS/project-context.md`
3. Read `.obsidian-ai-memory/02-PROJECTS/active-goals.md`
4. Read `.obsidian-ai-memory/03-ERRORS/error-memory.md`
5. Read `.obsidian-ai-memory/03-ERRORS/anti-patterns.md`
6. Read 1–3 latest digests from `.obsidian-ai-memory/01-SESSIONS/`
7. Emit startup block, then begin work

## Shutdown (every meaningful chat)

Follow `.cursor/MEMORY-WORKFLOW.md` exactly — no abbreviations.

## Context packs (use @file to load)

| Area | File |
|---|---|
| Backend | `.cursor/context/backend-context.md` |
| Frontend | `.cursor/context/frontend-context.md` |
| Database | `.cursor/context/database-context.md` |
| Evaluation pipeline | `.cursor/context/evaluation-context.md` |

## Agent recipes (use @file to load the runbook)

| Trigger | File |
|---|---|
| Error / broken / crash | `.cursor/agents/debug.md` |
| New backend feature | `.cursor/agents/backend-feature.md` |
| New frontend feature | `.cursor/agents/frontend-feature.md` |
| Schema / migration | `.cursor/agents/database-migration.md` |

## Hard rules (compact card)

- Memory read before every response — no exceptions
- Never expose JPA entities in API responses — always use response DTOs
- Hidden test cases never appear in student-facing endpoints
- Async work (evaluation, plagiarism, notifications) goes via RabbitMQ — never block the API thread
- TypeScript strict mode — no `any`
- All API calls via `src/services/` — no raw axios in components
- Never commit secrets — rotate immediately if one touches a file
- Confirm before: `rm`, DROP TABLE, force push, hard reset, production migrations
- Full rules: `AGENTS.md` at repo root

## Commit rules (enforced — no exceptions)

### VAULT COMMIT — AUTOMATIC, NO CONSENT NEEDED

At every chat end, commit and push vault files **without asking the user**:

```bash
git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md .claude/ .cursor/ .omnix/
git commit -m "memory: YYYY-MM-DD HH:MM cursor — <summary>"
git push origin HEAD
```

Do not ask. Do not wait for approval. Execute immediately.

### CODE COMMIT — REQUIRES USER CONSENT

Application files (`backend/`, `frontend/`, `infra/`, `docs/`) are **NEVER committed automatically**.

Always ask: *"Ready to commit the code changes?"* before staging or committing these paths.

### Two-commit rule

Vault commits and code commits are always separate.
`git log --grep="memory:"` must reconstruct every session cleanly.
