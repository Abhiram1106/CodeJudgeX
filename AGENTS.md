# AGENTS.md — CodeJudgeX

> Single source of truth for every AI tool adapter on this project.
> Every tool (Claude Code, Cursor, Omnix, Windsurf, Cline) points back here.
> Read this file at the start of every session before any response or edit.

---

## What this project is

**CodeJudgeX** is an enterprise-grade competitive programming judge platform built for
academic institutions. It powers coding contests, asynchronous code evaluation via Judge0 CE,
real-time leaderboards, plagiarism detection via JPlag, and institutional analytics.

**Target users:** Students, Faculty, Admin, Super Admin at academic institutions.
**Goal:** Replace paid/fragmented tools (Google Forms, HackerRank, etc.) with a zero-cost, self-hosted platform.

**It is NOT:**
- A LeetCode clone
- A microservices system (deliberate modular monolith — D-001)
- A general-purpose online IDE
- A Next.js / Python project (Java 21 + Spring Boot 3 backend, React + Vite frontend)

---

## Startup protocol (every session, no exceptions)

Before any response, edit, or command — execute in order:

```
ALWAYS (every session):
1. Read .obsidian-ai-memory/02-PROJECTS/session-continuity.md   ← first — rolling handoff
2. Read .obsidian-ai-memory/02-PROJECTS/active-goals.md         ← current week + checked tasks
3. Read .obsidian-ai-memory/03-ERRORS/error-memory.md           ← never repeat known bugs

TASK-TYPE OVERRIDES (read additionally based on what was requested):
  feature / build / implement  → also read docs/ROADMAP.md (find the module's section)
  debug / error / crash        → also read .obsidian-ai-memory/03-ERRORS/anti-patterns.md
  architecture / design        → also read .obsidian-ai-memory/04-DECISIONS/decisions.md
                                           .obsidian-ai-memory/05-ARCHITECTURE/system-overview.md
  refactor / clean             → also read .obsidian-ai-memory/03-ERRORS/anti-patterns.md
  first run / new session      → also read .obsidian-ai-memory/02-PROJECTS/project-context.md
                                           last 1-3 digests from .obsidian-ai-memory/01-SESSIONS/
```

**Emit this startup block at the top of every first response:**
```
[CodeJudgeX] Stack: Java 21 + Spring Boot 3 | React 18 + Vite | Mode: {balanced|deep|minimal}
[Memory] Read: {N files} | Last session: {YYYY-MM-DD} | Active goal: {one-line goal}
[Workflow] → {workflow name}
[Agents] → {roles}
[Starting] {one sentence describing what you are about to do}
```

---

## Retrieval modes

| Mode | Budget | When |
|---|---|---|
| `minimal` | ~400 tokens | Quick one-liner answers, trivial edits |
| `balanced` | ~1500 tokens | Most feature/fix/review work (default) |
| `deep` | ~3000 tokens | Architecture changes, major refactors, complex debugging |

**Red flags — stop and ask before proceeding if:**
- A known error in `error-memory.md` matches the current task
- The last digest says tests were failing and they haven't been fixed
- The request contradicts an active decision in `04-DECISIONS/decisions.md`
- The requested module is not in the current week's goals in `active-goals.md`

---

## Engineering rules

### Universal (all tools, all languages)

1. **Read memory before any edit.** `session-continuity.md` + `error-memory.md` before touching code.
2. **Never repeat known errors.** Check `error-memory.md` before diagnosing anything.
3. **No secrets in any file.** No API keys, tokens, passwords, connection strings with credentials.
4. **Verify before "done".** Run `./mvnw compile` or `npm run typecheck` — state the result explicitly.
5. **One concern per commit.** Don't bundle unrelated changes.
6. **Confirm before destructive operations:** `rm -rf`, `DROP TABLE`, force push, `git reset --hard`, production migrations.
7. **Update docs when behaviour changes.** Behaviour change without doc update = incomplete task.
8. **Write session digest** after every meaningful session. No exceptions.
9. **Record assumptions.** Unstated assumptions go in the digest under "Assumptions Made".
10. **Small changes over large rewrites.** Prefer safe, incremental changes.
11. **Consult ROADMAP first for any build task.** `docs/ROADMAP.md` has the exact file order, SQL blueprints, and acceptance criteria for every module. Do not invent your own order.

### Test discipline (enforced — not optional)

- Every `@Service` class must have a unit test class (`*ServiceTest.java`) — Mockito mocks for dependencies
- Every `@RestController` must have a MockMvc test (`*ControllerTest.java`) — happy path + one error case minimum
- Every module that touches the DB must have an integration test using Testcontainers (`*IntegrationTest.java`)
- Frontend: every hook and service function must have a Vitest unit test
- **No module is "done" without its tests passing**

### Java / Spring Boot (backend/)

- Validate all inputs at the controller boundary using Jakarta Validation (`@NotBlank`, `@Email`, `@Size`, etc.)
- Never expose JPA entities in API responses — always MapStruct → response DTO
- Business logic lives exclusively in `@Service` classes. Controllers are thin.
- Every async operation (evaluation, plagiarism, notifications) goes through RabbitMQ — never block the API thread
- Use `@Transactional` explicitly on service methods that touch multiple tables
- Never log passwords, tokens, source code content, or hidden test case data
- Config from `application.yml` via env vars — never hardcode values
- Structured logs with request correlation IDs on every error log entry
- Hidden test cases (`is_sample=false`) MUST NEVER appear in any student-facing API response — enforce at **service layer**, not just controller

### React / TypeScript (frontend/)

- TypeScript strict mode — no `any`. If you must escape, comment why.
- All API calls through typed service layer in `src/services/` — never raw fetch/axios in components
- Server state managed by TanStack Query — no manual loading/error/caching state
- Forms via React Hook Form + Zod — no manual form state
- shadcn/ui or Radix primitives — do not build custom interactive primitives from scratch
- Monaco editor is the code editor — do not substitute
- Every interactive element must be keyboard-reachable
- No layout shift — reserve space for async content and images
- The `@/` path alias maps to `src/` — always use it
- Submission polling: `refetchInterval` MUST stop when status is in `TERMINAL_STATUSES`

### Infrastructure / Docker

- All secrets via environment variables from `infra/.env` (never committed — only `.env.example`)
- Docker Compose services must have health checks and restart policies
- Never run database migrations against production without explicit user confirmation
- Judge0 CE handles all code execution — Spring Boot must never exec user code directly

---

## Routing table

| Request signal | Workflow | Activate roles | Also read |
|---|---|---|---|
| build / add / implement / create | feature-build | architect + fullstack + reviewer | `docs/ROADMAP.md` — find module section |
| error / broken / crash / failing / exception | debug → bug-fix | debugger + security | `error-memory.md` first |
| test failing / test broken | bug-fix + testing | debugger + qa | `error-memory.md` + `anti-patterns.md` |
| review / audit / check quality | code-review | reviewer + security | `.claude/agents/security-review.md` |
| refactor / clean / improve / simplify | refactor | architect + reviewer | `anti-patterns.md` |
| deploy / ship / release / publish | deployment | devops | `docs/ROADMAP.md` Week 5 section |
| slow / performance / optimize | debug + performance | debugger + performance | — |
| docs / readme / document | docs-update | docs | — |
| security / auth / vulnerability / CVE | code-review + security | security + reviewer | `.cursor/agents/security-review.md` |
| schema / migration / database / query | feature-build + database | architect + database | `docs/ROADMAP.md` Flyway section |
| first run / empty vault / setup | project-onboarding | fullstack | `project-context.md` + `decisions.md` |

---

## Stack map (canonical paths)

| Layer | Technology | Path |
|---|---|---|
| Frontend | React 18 + Vite + TypeScript + Tailwind + shadcn/ui | `frontend/` |
| Code editor | Monaco Editor | `frontend/src/components/editor/` |
| Frontend state | TanStack Query + Zustand | `frontend/src/features/` + `frontend/src/stores/` |
| API services | Axios typed service layer | `frontend/src/services/` |
| Backend | Java 21 + Spring Boot 3 | `backend/` |
| Backend root package | `com.codejudgex` | `backend/src/main/java/com/codejudgex/` |
| Auth module | JWT + Spring Security | `backend/src/main/java/com/codejudgex/auth/` |
| Evaluation worker | RabbitMQ consumer + Judge0 | `backend/src/main/java/com/codejudgex/evaluation/` |
| Leaderboard | Redis sorted sets | `backend/src/main/java/com/codejudgex/leaderboard/` |
| Plagiarism | JPlag | `backend/src/main/java/com/codejudgex/plagiarism/` |
| Primary DB | PostgreSQL 16 | `infra/docker-compose.yml` |
| Cache | Redis 7 | `infra/docker-compose.yml` |
| Queue | RabbitMQ 3 | `infra/docker-compose.yml` |
| Code execution | Judge0 CE | `infra/docker-compose.yml` |
| Monitoring | Prometheus + Grafana | `infra/prometheus/` + `infra/grafana/` |
| Migrations | Flyway | `backend/src/main/resources/db/migration/` |
| Infra config | Docker Compose | `infra/` |
| Build plan | ROADMAP | `docs/ROADMAP.md` |
| Design documents | Markdown | `docs/` |
| Memory vault | Obsidian-compatible | `.obsidian-ai-memory/` |

---

## Safety gates

Stop and confirm with the user before any of these:
- Deleting files or directories
- Dropping or truncating database tables
- Force-pushing to any branch
- Running `git reset --hard`
- Running database migrations against a live/production database
- Overwriting files with `--force`
- Publishing packages to any registry

---

## Shutdown protocol (MANDATORY — every session where files changed)

This is not optional. Every session that touches code, fixes bugs, or makes decisions MUST complete all steps.

```
1. Write session digest → .obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-<tool>.md
   Template: .obsidian-ai-memory/templates/session-digest.md
   Include: files changed, decisions made, errors encountered, assumptions, next 3 tasks

2. Overwrite session-continuity.md → .obsidian-ai-memory/02-PROJECTS/session-continuity.md
   This is the rolling handoff. The next session reads this first.

3. Update active-goals.md → check off completed tasks, uncheck anything reverted

4. If a bug was fixed → append entry to .obsidian-ai-memory/03-ERRORS/error-memory.md

5. If a non-trivial decision was made → append to .obsidian-ai-memory/04-DECISIONS/decisions.md

6. If architecture changed → update .obsidian-ai-memory/05-ARCHITECTURE/system-overview.md

7. CODE COMMIT — stage application files only:
   git add backend/ frontend/ infra/ docs/ (not .obsidian-ai-memory/)
   git commit -m "feat(module): description"   ← conventional commit, present tense

8. MEMORY COMMIT — stage vault files only:
   git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md .claude/ .cursor/
   git commit -m "memory: YYYY-MM-DD claude — summary of session"

9. PUSH — git push origin HEAD
   (confirm with user if pushing to a shared/main branch)

10. Final reply MUST include this block:
    ## Memory
    - Digest: .obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-claude.md
    - Code commit: <hash> — <subject>
    - Memory commit: <hash> — memory: ...
    - Push: ✓ pushed to origin/main  (or: ✗ not pushed — reason)
```

**Two-commit rule — enforced:**
- Code commit and memory commit are ALWAYS separate.
- `git log --grep="memory:"` reconstructs every session handoff cleanly.
- Application history must never be polluted with vault updates.
- If you forget and mix them: do NOT amend — create a follow-up memory commit.

---

## What "done" means

Do not say "done" until ALL of the following are true:
- [ ] Changed files are correct and match the stated intent
- [ ] `./mvnw compile` passes (backend) or `npm run typecheck` passes (frontend)
- [ ] Unit tests written and passing for the module's service class
- [ ] Docs updated if behaviour or API contract changed
- [ ] Session digest written to vault
- [ ] `session-continuity.md` overwritten
- [ ] `error-memory.md` appended if a bug was fixed
- [ ] `decisions.md` appended if a non-trivial choice was made
- [ ] Code commit made (application files)
- [ ] Memory commit made (vault files)
- [ ] `git push origin HEAD` executed
- [ ] No secrets in any written or modified file
- [ ] Open risks explicitly listed if any remain
