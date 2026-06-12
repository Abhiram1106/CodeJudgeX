---
type: session-continuity
updated: 2026-06-12
tool: claude-code
tags: [continuity, handoff]
---

# Session Continuity — CodeJudgeX

**Last updated:** 2026-06-12 by claude-sonnet-4-6
**Branch:** main
**Full suite:** 59/59 ✅ (unchanged this session — no code touched)

---

## Current State

### What's complete (backend)
- **Week 1:** Auth, Problem, TestCase, Contest, Submission modules — entities, repos, services, controllers
- **MockMvc tests:** AuthControllerTest (7), ContestControllerTest (4), ProblemControllerTest (4), SubmissionControllerTest (5) — all passing
- **Security fixes:** `authenticationEntryPoint` (401), `accessDeniedHandler` (403), `AuthorizationDeniedException` handler in GlobalExceptionHandler
- **JWT fix:** `userId` UUID stored as principal name; email stored as credentials on UsernamePasswordAuthenticationToken
- **Week 2 evaluation pipeline:**
  - `Judge0Client` → `EvaluationWorker` → `OutputComparator` → `ScoreCalculator`
  - `EvaluationWorker` updates Redis leaderboard after evaluation
  - Retry (3x) → DLQ + INTERNAL_ERROR on failure
- **Leaderboard module:** Redis ZSET live + PG snapshot fallback
- **Notification module:** in-app + MailHog email via RabbitMQ consumer
- **Flyway migrations:** V1–V10 (V10 adds verdict/weight/memoryUsedKb to submission_results)
- **Frontend foundation (from prior session, untouched here):** types, axios+JWT interceptor,
  TanStack Query client, Zustand auth store, 6 services, route guards, auth pages, routing —
  present as untracked files (`frontend/src/components/`, `frontend/src/lib/queryClient.ts`,
  `frontend/src/lib/utils.ts`, `frontend/src/pages/`, `frontend/src/services/`,
  `frontend/src/stores/`, `frontend/src/types/`)

### Infra/docs cleanup completed THIS session (2026-06-11)

- **`.omnix/` removed entirely** — Omnix dropped as an AI tool adapter (now: Claude Code, Cursor,
  Windsurf, Cline). All references stripped from AGENTS.md, CLAUDE.md, .claude/settings.json,
  .cursor/AGENTS.md, .cursor/MEMORY-WORKFLOW.md, STARTUP_PROTOCOL.md, .gitignore.
- **Docker removed entirely.** PostgreSQL/Redis/RabbitMQ → native local services. Judge0 CE →
  remote/hosted instance (`JUDGE0_URL` + `JUDGE0_TOKEN`, e.g. Judge0 RapidAPI). Deleted
  `infra/docker-compose.yml` + `infra/prometheus/`. Updated Makefile, README.md,
  `infra/.env.example`, `docs/ROADMAP.md` (Dockerfiles/Nginx/Grafana/Prometheus sections replaced
  with Spring Actuator `/actuator/health` + `/actuator/prometheus`).
- `./mvnw compile -q` → exit 0. `application.yml` already env-var driven — **zero backend code
  changes required** for this cleanup.
- Decision recorded as D-009 in `04-DECISIONS/decisions.md`.

### Rules engine update + repo cleanup completed (2026-06-12, earlier session)

- **AGENTS.md rule #12 added**: commit + `git push origin HEAD` are one atomic step,
  no separate push-confirmation once commit consent is given. Cascaded to CLAUDE.md,
  `.cursor/AGENTS.md`, `.cursor/MEMORY-WORKFLOW.md`. Persistent feedback memory saved.
- **D-009 .omnix leftovers committed**: `.gitignore`, `STARTUP_PROTOCOL.md`, and all
  remaining `.omnix/*` deletions — pushed.
- **Repo decluttered**: removed root JVM crash dumps (`hs_err_pid*.log`,
  `replay_pid*.log`), empty `infra/grafana/` + `infra/judge0/` (orphaned by Docker
  removal), empty `.tours/`, unrelated `.github/java-upgrade/` + `.github/modernize/`
  appmod scaffolding, and ~180 stale `.remember/logs/*.log` files. All were
  untracked/gitignored — no code commit needed.
- `./mvnw compile -q` → exit 0, `npm run typecheck` → exit 0 after cleanup.

### D-010: Docker reinstated — full docker-compose stack (THIS session, 2026-06-12)

D-009 (Docker removal, <24h old) was reversed at user request. The project now runs
fully via `infra/docker-compose.yml` (`make up`):

- **New files**: `infra/docker-compose.yml` (postgres, redis, rabbitmq, judge0-db,
  judge0-redis, judge0-server `1.13.1` w/ `privileged: true`, judge0-workers,
  mailhog, prometheus, grafana, backend, frontend — all with healthchecks),
  `infra/prometheus/prometheus.yml` (scrapes `backend:8080/api/v1/actuator/prometheus`),
  `backend/Dockerfile` + `.dockerignore` (multi-stage JDK→JRE alpine),
  `frontend/Dockerfile` + `.dockerignore` + `frontend/nginx/default.conf`
  (multi-stage Node→Nginx, proxies `/api/`, `/api/v1/swagger-ui/`, `/api/v1/api-docs`
  to `backend:8080`).
- **Updated**: `infra/.env.example` (Docker hostnames primary, native localhost
  fallback documented), `Makefile` (`up`/`down`/`build-images`/`logs`/`infra-up`/
  `infra-down` added, native targets kept), `README.md` (Infrastructure table,
  Quick Start rewritten for `docker compose up` + "Native Development" subsection,
  Repository Structure tree updated), `docs/ROADMAP.md` (Week 5 Dockerfiles/Nginx/CI
  sections restored with our actual multi-stage builds, stack header, risk register
  rows for Judge0 `privileged: true` and CI), `AGENTS.md` (Infrastructure section
  renamed "Docker Compose (D-010)", stack map rows for Postgres/Redis/RabbitMQ/Judge0
  now point at `infra/docker-compose.yml`).
- **No change needed**: `application.yml` (already fully env-var driven —
  hostnames come from compose env vars, not code), `.gitignore` (docker volumes
  already covered).
- D-010 recorded in `04-DECISIONS/decisions.md`, supersedes D-009.
- Verified: `./mvnw compile -q` → exit 0, `npm run typecheck` → exit 0,
  `docker compose -f infra/docker-compose.yml config -q` → exit 0 (valid syntax,
  Docker not run end-to-end — no images built/tested in this session).

### What's NOT done

- Frontend: foundation exists (untracked, uncommitted) but feature pages (Week 4 list) not built
- EvaluationWorker unit test (needs Judge0Client mock)
- Week 3: rate limiting, security headers, CORS hardening, audit module, plagiarism module, admin module
- Docker stack has NOT been run end-to-end (`make up`) — only `docker compose config` syntax-validated. First real run may surface issues (Judge0 privileged mode on Windows/WSL2, image pulls, healthcheck timing, Nginx proxy paths).

---

## Immediate Next Steps (in order)

1. Run `make up` (or `docker compose -f infra/docker-compose.yml up -d --build`) end-to-end at least once; fix any issues (image pulls, healthcheck timing, Judge0 privileged mode under Docker Desktop/WSL2, Nginx proxy paths)
2. Resume Week 3 (`active-goals.md`): Redis rate limiting → CORS/security headers → audit module → plagiarism → admin
3. Week 5: Actuator/Micrometer metrics + integration tests per `docs/ROADMAP.md` (metrics endpoint path now `/api/v1/actuator/prometheus`)

---

## Known Issues / Bugs to Avoid

- `@AuthenticationPrincipal Principal` returns null with `@WithMockUser` — always use `Authentication authentication` parameter directly
- `AuthorizationDeniedException` from `@PreAuthorize` bypasses security filter — must be handled in `GlobalExceptionHandler`, not just `accessDeniedHandler`
- IDE (NetBeans/VSCode Java LS) shows false Lombok errors — Maven compile is the only ground truth
- Redis ZADD always overwrites — if student re-submits with lower score, leaderboard may decrease; add best-score guard before ZADD
- Docker build context constraint: a service's Dockerfile `COPY` source paths are relative to its `context:` dir in docker-compose and cannot reference `../` paths outside it (this is why `nginx/default.conf` lives in `frontend/nginx/`, not `infra/nginx/`)
- Actuator path under Docker/native is `/api/v1/actuator/...` (context-path `/api/v1` + management base-path `/actuator`) — Prometheus scrape config and Nginx proxy rules must include the `/api/v1` prefix

---

## Key File Paths

| What | Path |
|------|------|
| Security config | `backend/src/main/java/com/codejudgex/infrastructure/config/SecurityConfig.java` |
| Global exception handler | `backend/src/main/java/com/codejudgex/common/exception/GlobalExceptionHandler.java` |
| JWT filter | `backend/src/main/java/com/codejudgex/auth/filter/JwtAuthenticationFilter.java` |
| Evaluation worker | `backend/src/main/java/com/codejudgex/evaluation/EvaluationWorker.java` |
| Judge0 client | `backend/src/main/java/com/codejudgex/evaluation/Judge0Client.java` |
| Leaderboard service | `backend/src/main/java/com/codejudgex/leaderboard/service/LeaderboardService.java` |
| Notification worker | `backend/src/main/java/com/codejudgex/notification/service/NotificationWorker.java` |
| Flyway migrations | `backend/src/main/resources/db/migration/` (V1–V10) |
| Build plan | `docs/ROADMAP.md` (full Docker Compose as of 2026-06-12, D-010) |
| Compose stack | `infra/docker-compose.yml` (D-010) |
| Env template | `infra/.env.example` (Docker hostnames + native localhost fallback) |
