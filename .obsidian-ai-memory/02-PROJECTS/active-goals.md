---
type: active-goals
updated: 2026-05-24
tags: [goals, roadmap]
---

# Active Goals — CodeJudgeX

> Source of truth for what to build next.
> Check boxes off as tasks complete. Never delete completed items — move to ## Completed.
> Full week-by-week plan with file order + acceptance criteria → `docs/ROADMAP.md`

---

## Current phase: Week 1 — Database Foundation + Backend Core
**Period:** 2026-05-25 → 2026-05-31
**Goal:** Flyway schema complete + auth + problem + contest + submission modules working

### Flyway Migrations (do first — everything depends on schema)

- [ ] V1__create_users_roles.sql — users, roles, user_roles
- [ ] V2__create_problems.sql — problems, problem_tags, problem_tag_map
- [ ] V3__create_test_cases.sql — test_cases (with is_sample, weight)
- [ ] V4__create_contests.sql — contests, contest_problems, contest_participants
- [ ] V5__create_submissions.sql — submissions, submission_results
- [ ] V6__create_leaderboard.sql — leaderboard_snapshots
- [ ] V7__create_plagiarism.sql — plagiarism_jobs, plagiarism_flags
- [ ] V8__create_supporting.sql — notifications, audit_logs, refresh_tokens
- [ ] V9__add_indexes.sql — composite indexes for common query patterns

### Auth Module

- [ ] User + Role + UserRole entities
- [ ] UserRepository + RoleRepository
- [ ] RegisterRequest + LoginRequest + AuthResponse DTOs
- [ ] UserMapper (MapStruct)
- [ ] JwtService — generate/validate access tokens
- [ ] RefreshTokenService — create, rotate, revoke
- [ ] AuthService — register (BCrypt), login, refresh, logout
- [ ] JwtAuthenticationFilter (OncePerRequestFilter)
- [ ] SecurityConfig — filter chain, role hierarchy
- [ ] AuthController — /register /login /refresh /logout /me
- [ ] AuthServiceTest (unit) + AuthIntegrationTest (Testcontainers)

### Problem Module

- [ ] Problem + TestCase + ProblemTag entities
- [ ] ProblemRepository + TestCaseRepository
- [ ] CreateProblemRequest + ProblemResponse + ProblemSummaryResponse DTOs
- [ ] TestCaseResponse DTO (excludes hidden test case data for students)
- [ ] ProblemMapper (MapStruct)
- [ ] ProblemService — CRUD, hidden test case protection at service layer
- [ ] ProblemController — FACULTY+ to create/edit, authenticated to read
- [ ] ProblemServiceTest + ProblemControllerTest

### Contest Module

- [ ] Contest + ContestProblem + ContestParticipant entities
- [ ] ContestRepository
- [ ] CreateContestRequest + ContestResponse + ContestSummaryResponse DTOs
- [ ] ContestMapper
- [ ] ContestService — lifecycle, registration, problem assignment
- [ ] ContestController
- [ ] ContestServiceTest + ContestControllerTest

### Submission Module (accept + queue only — not evaluation)

- [ ] Submission + SubmissionResult entities
- [ ] SubmissionRepository
- [ ] CreateSubmissionRequest + SubmissionResponse + SubmissionStatusResponse DTOs
- [ ] EvaluationMessage POJO (RabbitMQ message)
- [ ] SubmissionMapper
- [ ] SubmissionService — validate, persist QUEUED, publish to RabbitMQ
- [ ] SubmissionController — POST /submissions (202), GET /submissions/{id}/status
- [ ] SubmissionServiceTest + SubmissionControllerTest

### Week 1 done when
- [ ] `cd backend && ./mvnw clean package` succeeds with no errors
- [ ] All module unit tests pass
- [ ] Swagger UI shows all auth/problems/contests/submissions endpoints
- [ ] Postman/Bruno can hit POST /auth/register → POST /auth/login → GET /problems

---

## Week 2 — Async Evaluation Pipeline + Leaderboard
**Period:** 2026-06-01 → 2026-06-07

- [ ] RabbitMQ exchange + queue config (evaluation, retry, dlq, notification, plagiarism)
- [ ] Redis key setup (leaderboard ZSET, status cache, rate limit)
- [ ] Judge0Client — REST wrapper for Judge0 CE API
- [ ] EvaluationWorker — RabbitMQ consumer, calls Judge0, compares output, scores
- [ ] OutputComparator — trim/normalize/compare
- [ ] ScoreCalculator — sum of weights of passing test cases
- [ ] Retry logic (3 attempts) + DLQ handling + INTERNAL_ERROR on terminal failure
- [ ] LeaderboardService — Redis ZADD/ZREVRANGE/ZREVRANK + PG snapshot
- [ ] LeaderboardController — GET /leaderboards/contests/{id}
- [ ] NotificationWorker — consume notification.queue, persist in-app + MailHog email
- [ ] NotificationController — GET /notifications, PATCH /{id}/read
- [ ] EvaluationIntegrationTest — full async flow with Testcontainers

### Week 2 done when
- [ ] Submit Java code → QUEUED → RUNNING → ACCEPTED/WRONG_ANSWER (end-to-end)
- [ ] Leaderboard updates within 1s of evaluation completing
- [ ] Failed evaluation × 3 → INTERNAL_ERROR + message in DLQ

---

## Week 3 — Security Hardening + Admin + Plagiarism
**Period:** 2026-06-08 → 2026-06-14

- [ ] Rate limiting — Redis-based, 5 submissions/student/contest/minute
- [ ] CORS — allow localhost:5173 only, never wildcard
- [ ] Security headers (X-Content-Type-Options, X-Frame-Options, CSP)
- [ ] Role enforcement audit — every controller has @PreAuthorize
- [ ] Hidden test case audit — grep all endpoints, verify student can't reach is_sample=false
- [ ] AuditLog entity + AuditService + AuditAspect (@Around AOP)
- [ ] All 17 audit events wired
- [ ] AuditController — GET /audit-logs (ADMIN+, paginated, filterable)
- [ ] JPlagService — collect submissions, run JPlag, parse results
- [ ] PlagiarismWorker — consume plagiarism.queue, delegate, persist flags
- [ ] PlagiarismController — POST /plagiarism/contests/{id}/check, GET flags, PATCH review
- [ ] AdminController — user management, role change, activate/deactivate
- [ ] AnalyticsService — platform stats + per-contest stats
- [ ] Security audit checklist (`.cursor/agents/security-review.md`) passed

### Week 3 done when
- [ ] Security audit passes — every endpoint protected
- [ ] All 17 audit events create audit_log rows
- [ ] Plagiarism check → JPlag runs → flags stored → admin sees results

---

## Week 4 — Frontend
**Period:** 2026-06-15 → 2026-06-21

- [ ] Shared TypeScript types matching all backend DTOs
- [ ] axios instance + JWT interceptor + 401 → /login redirect
- [ ] TanStack Query client (staleTime 30s default)
- [ ] Zustand auth store (user, accessToken, setAuth, clearAuth)
- [ ] Service layer — one file per backend module
- [ ] LoginPage + RegisterPage (React Hook Form + Zod)
- [ ] RequireAuth + RequireRole route guards
- [ ] Silent refresh token interceptor
- [ ] ContestListPage + ContestDetailPage
- [ ] ProblemPage + Monaco editor (CodeEditor wrapper) + language selector
- [ ] useSubmissionStatus polling hook (stops on TERMINAL_STATUSES)
- [ ] SubmissionResultPage + SubmissionHistoryPage
- [ ] LeaderboardPage (live-updating)
- [ ] CreateProblemPage + CreateContestPage (FACULTY)
- [ ] ContestSubmissionsPage + PlagiarismReviewPage (FACULTY)
- [ ] UserManagementPage + AuditLogPage + AdminDashboardPage (ADMIN)
- [ ] Route wiring (all pages + guards)
- [ ] `npm run typecheck` passes
- [ ] `npm run build` succeeds

---

## Week 5 — Observability + CI/CD + Polish
**Period:** 2026-06-22 → 2026-06-28

- [ ] Custom Prometheus metrics (submission counters, eval duration histogram, queue gauge)
- [ ] Grafana dashboard provisioning (6 panels)
- [ ] SubmissionEvaluationIntegrationTest (full flow)
- [ ] ContestLifecycleIntegrationTest
- [ ] backend/Dockerfile + frontend/Dockerfile
- [ ] infra/nginx/default.conf
- [ ] .github/workflows/backend-ci.yml
- [ ] .github/workflows/frontend-ci.yml
- [ ] .github/workflows/docker-ci.yml
- [ ] Final polish: no TODOs, all env vars in .env.example, Swagger complete
- [ ] `docker compose up --build` — all services start, full flow works
- [ ] README quick-start verified from fresh clone

---

## Completed

- [x] Monorepo scaffold — backend/, frontend/, infra/, docs/
- [x] pom.xml with all backend dependencies (Java 21, Spring Boot 3.3, all modules)
- [x] application.yml — env-var-based config
- [x] frontend package.json + vite.config.ts + tsconfig.json + tailwind.config.ts
- [x] Docker Compose — all 8 infrastructure services
- [x] Prometheus scrape config
- [x] infra/.env.example
- [x] Makefile (10 targets)
- [x] Enterprise README.md
- [x] AI agent infrastructure — AGENTS.md, CLAUDE.md, AI_RULES.md, STARTUP_PROTOCOL.md, PROJECT_CONTEXT.md
- [x] Cursor adapter — 6 .mdc rules, 4 agent recipes, 4 context packs
- [x] Claude Code adapter — settings.json (hooks), settings.local.json (allowlist)
- [x] Memory vault — project-context, active-goals, session-continuity, protocols, templates, decisions, architecture
- [x] .claude/rules/code-style.md — polyglot style guide
- [x] .claude/rules/frontend/react.md — React/TS rules
- [x] .claude/agents/README.md — subagent patterns
- [x] .claude/skills/README.md — skill invocation guide
- [x] .claude/.mcp.json — MCP placeholder
- [x] .omnix/ agents/commands/workflows/memory READMEs — populated
- [x] .cursor/agents/security-review.md — 8-section security audit runbook
- [x] docs/ROADMAP.md — full 5-week build spec with file order, SQL blueprints, acceptance criteria
- [x] AGENTS.md updated — ROADMAP ref, test discipline, shutdown git push mandate
- [x] CLAUDE.md updated — build verification commands, full completion gate
- [x] .claude/settings.local.json — expanded allowlist for build phase
- [x] .claude/settings.json — hooks cleaned up (PreToolUse on Write/Edit only)
- [x] Vault project-context.md — fully populated with all docs/ content
