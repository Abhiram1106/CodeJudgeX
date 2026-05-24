---
type: session-digest
date: 2026-05-23
time: "00:01"
tool: claude
week-goal: Monorepo scaffold + AI agent infrastructure
tags: [session, scaffold, ai-infra, setup]
---

# Session Digest — 2026-05-23 00:01 (claude)

## Request

Two sequential requests:
1. "Organize the entire folder into a clean monorepo"
2. "Build the same AI agent workflow as described in the deep-dive analysis"

## Memory retrieved at session start

- session-continuity.md: no (did not exist — first session)
- project-context.md: partial (stub only)
- active-goals.md: stub only
- error-memory.md: empty
- anti-patterns.md: empty
- Last N session digests: none (first session)
- Docs read: architecture.md, api_design.md, backend_structure.md, frontend_design.md, security.md, database_design.md

## Files created / changed

### Monorepo scaffold

| File | Change |
|---|---|
| `backend/pom.xml` | created — Java 21 + Spring Boot 3.3, all deps |
| `backend/src/main/resources/application.yml` | created — env-var config |
| `backend/src/main/java/com/codejudgex/CodeJudgeXApplication.java` | created |
| `backend/src/main/java/com/codejudgex/{16 modules}/` | created — full package tree |
| `frontend/package.json` | created — all deps |
| `frontend/vite.config.ts` | created — API proxy |
| `frontend/tsconfig.json` | created — strict TS |
| `frontend/tailwind.config.ts` | created |
| `frontend/index.html` | created |
| `frontend/src/main.tsx` | created — providers wired |
| `frontend/src/app/App.tsx` | created |
| `frontend/src/app/globals.css` | created |
| `frontend/src/lib/axios.ts` | created — JWT interceptor |
| `infra/docker-compose.yml` | created — all 8 services |
| `infra/prometheus/prometheus.yml` | created |
| `infra/.env.example` | created |
| `docs/` | created — 12 docs moved + renamed |
| `Makefile` | created — 10 targets |
| `README.md` | created — enterprise README (13 sections) |
| `.gitignore` | updated |

### AI agent infrastructure

**Contract layer:**

| File | Change |
|---|---|
| `AGENTS.md` | rewritten — canonical contract (startup, retrieval, rules, routing, stack map, safety, shutdown) |
| `CLAUDE.md` | rewritten — thin adapter with @AGENTS.md import + completion gate |
| `AI_RULES.md` | rewritten — CodeJudgeX-specific quick reference |
| `STARTUP_PROTOCOL.md` | rewritten — matches CodeJudgeX stack |
| `PROJECT_CONTEXT.md` | updated — accurate stack |

**Cursor adapter:**

| File | Change |
|---|---|
| `.cursor/AGENTS.md` | created — Cursor-specific startup/shutdown |
| `.cursor/MEMORY-WORKFLOW.md` | created — 10-step shutdown ritual |
| `.cursor/cursor-settings.json` | created — alwaysInclude + indexing |
| `.cursor/rules/project-rules.mdc` | rewritten — CodeJudgeX identity |
| `.cursor/rules/memory-session.mdc` | created — memory loop reminder |
| `.cursor/rules/backend.mdc` | rewritten — Java/Spring Boot rules |
| `.cursor/rules/frontend.mdc` | rewritten — React/TS rules + submission polling |
| `.cursor/rules/security.mdc` | rewritten — JWT, RBAC, hidden test cases |
| `.cursor/rules/testing.mdc` | rewritten — JUnit 5 + Vitest rules |
| `.cursor/agents/debug.md` | created — debugger runbook |
| `.cursor/agents/backend-feature.md` | created — backend feature runbook |
| `.cursor/agents/frontend-feature.md` | created — frontend feature runbook |
| `.cursor/agents/database-migration.md` | created — migration runbook |
| `.cursor/context/backend-context.md` | created — full backend context pack |
| `.cursor/context/frontend-context.md` | created — full frontend context pack |
| `.cursor/context/database-context.md` | created — schema + Redis + Flyway context |
| `.cursor/context/evaluation-context.md` | created — async pipeline + Judge0 context |

**Claude Code adapter:**

| File | Change |
|---|---|
| `.claude/settings.json` | updated — Stop hook + PreToolUse hook |
| `.claude/settings.local.json` | updated — Maven/npm/Docker allowlist + deny destructive |

**Memory vault:**

| File | Change |
|---|---|
| `.obsidian-ai-memory/02-PROJECTS/project-context.md` | populated — full stack, constraints, do-not-repeat |
| `.obsidian-ai-memory/02-PROJECTS/active-goals.md` | populated — 5-week roadmap with checkboxes |
| `.obsidian-ai-memory/02-PROJECTS/session-continuity.md` | created — first handoff |
| `.obsidian-ai-memory/MEMORY-READ-PROTOCOL.md` | created — retrieval modes + read order + red flags |
| `.obsidian-ai-memory/MEMORY-WRITE-PROTOCOL.md` | created — write rules + two-commit rule |
| `.obsidian-ai-memory/04-DECISIONS/decisions.md` | created — D-001 through D-006 |
| `.obsidian-ai-memory/05-ARCHITECTURE/system-overview.md` | created — component map + data flows |
| `.obsidian-ai-memory/07-LESSONS/debugging-lessons.md` | created — L-001 through L-003 |
| `.obsidian-ai-memory/templates/session-digest.md` | updated — CodeJudgeX fields |
| `.obsidian-ai-memory/templates/session-continuity.md` | created — overwrite template |

## Commands run

```bash
mkdir -p backend/src/...   # package tree creation
mkdir -p frontend/src/...  # frontend tree creation
mkdir -p infra/prometheus  # infra dirs
mkdir -p .cursor/agents .cursor/context  # cursor dirs
mkdir -p .obsidian-ai-memory/04-DECISIONS .obsidian-ai-memory/05-ARCHITECTURE ...
mv docs/codejudgex_*.md docs/  # moved + renamed 12 docs
```

## Decisions made

- D-001: Modular monolith (not microservices)
- D-002: Async evaluation via RabbitMQ (never synchronous)
- D-003: PostgreSQL source of truth, Redis speed layer only
- D-004: MapStruct for entity↔DTO mapping, entities never in responses
- D-005: Judge0 CE for sandboxed execution, Spring Boot never execs user code
- D-006: Flyway for migrations, Hibernate ddl-auto=validate

## Errors encountered

None — setup session only.

## Assumptions made

- Java 21 is available on the dev machine (no version check run)
- Docker with privileged mode is available (required for Judge0 CE on Windows)
- The user intends to start implementation with backend auth + database schema next

## Tests / verification

- Not run — no source code exists yet (scaffold only)
- `backend/pom.xml` structure is valid Maven (visually confirmed)
- `frontend/package.json` structure is valid npm (visually confirmed)
- `infra/docker-compose.yml` structure is valid Compose v3.9 (visually confirmed)

## Docs updated

- README.md — enterprise README written (13 sections)
- All 12 design docs moved to docs/ and renamed (stripped codejudgex_ prefix)

## Memory written after session

- [x] session-continuity.md overwritten
- [x] active-goals.md created with 5-week roadmap
- [x] error-memory.md — no bugs to append
- [x] decisions.md — D-001 through D-006 created
- [x] architecture docs — system-overview.md created

## Open risks

- Judge0 CE requires Docker privileged mode — may need special Windows Docker Desktop settings
- JPlag memory usage under large contest load — test before enabling in production
- No CI/CD pipeline yet — all verification is manual until Week 5

## Next recommended step

Write Flyway migrations V1–V9 (the database schema foundation). Start with:
`backend/src/main/resources/db/migration/V1__create_users_and_roles.sql`
