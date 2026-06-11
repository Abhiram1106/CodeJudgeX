# CodeJudgeX — Build Roadmap & Project Specification

> **Audience:** Solo developer + AI agents (Claude Code, Cursor).
> This document is the authoritative build plan. Agents read it at session start to understand what to build next, in what order, and why.
>
> **Timeline:** 5 weeks · 2026-05-25 → 2026-06-28
> **Stack:** Java 21 + Spring Boot 3.3 · React 18 + Vite · PostgreSQL · Redis · RabbitMQ · Judge0 CE (remote/hosted) — local services, no Docker

---

## Problem This Solves

Academic institutions (colleges, training institutes, coding clubs) run programming assessments using Google Forms, spreadsheets, WhatsApp, manual evaluation, or paid external platforms. These approaches have no automated evaluation, no hidden test cases, no plagiarism detection, no audit trail, and no institutional ownership of data.

CodeJudgeX replaces all of that with a **zero-cost, self-hosted, production-grade** platform that an institution owns and controls.

---

## Success Criteria (the definition of "done" for the whole project)

The project is complete when ALL of these are true:

- [ ] A student can register, join a live contest, write code in Monaco, and submit
- [ ] The submission is evaluated asynchronously via RabbitMQ → EvaluationWorker → Judge0 CE
- [ ] Hidden test cases are used and never returned in any student-facing response
- [ ] Score and verdict are stored in PostgreSQL and leaderboard updates in Redis
- [ ] A faculty member can create problems, add hidden test cases, create a contest, and view submissions
- [ ] An admin can manage users, view audit logs, and trigger plagiarism checks
- [ ] The full stack runs locally with PostgreSQL, Redis, and RabbitMQ as native services and a Judge0 CE remote/hosted instance
- [ ] Swagger UI documents every API endpoint
- [ ] `./mvnw clean package` and `npm run build` both succeed with no errors
- [ ] Unit tests cover auth, submission, evaluation, and leaderboard logic
- [ ] Integration tests cover the full submission → evaluation → result flow
- [ ] Spring Boot Actuator exposes queue health, verdict metrics, and API latency

---

## Architectural Decisions (non-negotiable — do not revisit without appending to decisions.md)

| # | Decision | Rule |
|---|---|---|
| D-001 | Modular monolith | One Spring Boot app. No microservices. |
| D-002 | Async evaluation | POST /submissions returns 202 immediately. Never synchronous. |
| D-003 | PostgreSQL = source of truth | Redis is speed layer only. All data reconstructible from PG. |
| D-004 | MapStruct DTOs | JPA entities never in API responses. Period. |
| D-005 | Judge0 CE for execution | Spring Boot never calls Runtime.exec() or ProcessBuilder on user input. |
| D-006 | Flyway migrations | `ddl-auto=validate` always. Hibernate never creates/drops tables. |

---

## Module Inventory

Every module in `com.codejudgex.*` and its responsibility:

| Module | Package | Responsibility |
|---|---|---|
| `auth` | `com.codejudgex.auth` | Register, login, JWT access + refresh tokens, logout, /me |
| `user` | `com.codejudgex.user` | Profile, department/year, status management |
| `role` | `com.codejudgex.role` | Role entity, role assignment — STUDENT/FACULTY/ADMIN/SUPER_ADMIN |
| `problem` | `com.codejudgex.problem` | Problem CRUD, difficulty, constraints, time/memory limits |
| `testcase` | `com.codejudgex.testcase` | Sample + hidden test cases, weights, visibility protection |
| `contest` | `com.codejudgex.contest` | Contest lifecycle: DRAFT→UPCOMING→LIVE→ENDED, registration |
| `submission` | `com.codejudgex.submission` | Accept code, persist as QUEUED, publish to RabbitMQ, return 202 |
| `evaluation` | `com.codejudgex.evaluation` | RabbitMQ consumer, Judge0 CE client, output compare, score |
| `leaderboard` | `com.codejudgex.leaderboard` | Redis sorted sets, PG snapshots, rank calculation |
| `plagiarism` | `com.codejudgex.plagiarism` | JPlag integration, similarity flagging, admin review workflow |
| `notification` | `com.codejudgex.notification` | In-app + MailHog email, notification queue consumer |
| `audit` | `com.codejudgex.audit` | Append-only event log for all critical actions |
| `admin` | `com.codejudgex.admin` | User management, platform analytics, system settings |
| `analytics` | `com.codejudgex.analytics` | Contest stats, submission trends, faculty reports |
| `common` | `com.codejudgex.common` | ApiResponse wrapper, exceptions, base entities, constants |
| `infrastructure` | `com.codejudgex.infrastructure` | RabbitMQ config, Redis config, Judge0 client bean, security config |

---

## Critical Path (build in this order — each layer unblocks the next)

```
Layer 0: Infrastructure config (RabbitMQ queues, Redis, Security filter chain)
    ↓
Layer 1: Database schema (Flyway V1–V9)
    ↓
Layer 2: Auth module (JWT, BCrypt, refresh tokens) — unblocks all protected endpoints
    ↓
Layer 3: User + Role modules — unblocks faculty/admin role-gated features
    ↓
Layer 4: Problem + TestCase modules — unblocks contest problem assignment
    ↓
Layer 5: Contest module — unblocks student participation + submission acceptance
    ↓
Layer 6: Submission module (accept + queue) — unblocks evaluation pipeline
    ↓
Layer 7: Evaluation worker (RabbitMQ consumer + Judge0 CE) — unblocks leaderboard
    ↓
Layer 8: Leaderboard module (Redis + PG) — unblocks contest completion
    ↓
Layer 9: Notification + Audit modules — unblocks admin visibility
    ↓
Layer 10: Plagiarism + Admin modules — completes backend
    ↓
Layer 11: Frontend (all features) — depends on all backend APIs being stable
    ↓
Layer 12: Observability + CI/CD + Polish
```

---

## Week-by-Week Plan

---

### WEEK 1 — Database Foundation + Backend Core Modules
**Target: 2026-05-25 → 2026-05-31**
**Goal: Infra compiles, DB schema exists, auth + problem + contest + submission APIs return correct responses**

#### Flyway Migrations (do these first — everything else depends on schema)

**V1 — users, roles, user_roles**
```sql
-- Tables: users, roles, user_roles
-- users: id UUID PK, name, email UNIQUE, password_hash, department, year, status, created_at
-- roles: id UUID PK, name UNIQUE (STUDENT/FACULTY/ADMIN/SUPER_ADMIN)
-- user_roles: user_id FK, role_id FK, assigned_at
-- Indexes: idx_users_email, idx_user_roles_user_id
```

**V2 — problems, problem_tags, problem_tag_map**
```sql
-- problems: id, title, description, difficulty, input_format, output_format,
--           constraints_text, time_limit_ms, memory_limit_mb, created_by FK, created_at, updated_at
-- problem_tags: id, name UNIQUE
-- problem_tag_map: problem_id FK, tag_id FK
-- Indexes: idx_problems_created_by, idx_problems_difficulty
```

**V3 — test_cases**
```sql
-- test_cases: id, problem_id FK, input_data TEXT, expected_output TEXT,
--             is_sample BOOLEAN, weight INT DEFAULT 1, created_at
-- Indexes: idx_test_cases_problem_id, idx_test_cases_is_sample
-- NOTE: is_sample=false rows NEVER appear in student-facing responses (enforced in service)
```

**V4 — contests, contest_problems, contest_participants**
```sql
-- contests: id, title, description, start_time TIMESTAMPTZ, end_time TIMESTAMPTZ,
--           status (DRAFT/UPCOMING/LIVE/ENDED), created_by FK, created_at, updated_at
-- contest_problems: contest_id FK, problem_id FK, problem_order INT
-- contest_participants: contest_id FK, user_id FK, registered_at
-- Indexes: idx_contests_status, idx_contests_start_time, idx_contest_problems_contest_id
```

**V5 — submissions, submission_results**
```sql
-- submissions: id, student_id FK, contest_id FK, problem_id FK, language_id INT,
--              source_code TEXT, source_code_hash VARCHAR(64),
--              status (QUEUED/RUNNING/ACCEPTED/WRONG_ANSWER/PARTIALLY_ACCEPTED/
--                      COMPILATION_ERROR/RUNTIME_ERROR/TIME_LIMIT_EXCEEDED/
--                      MEMORY_LIMIT_EXCEEDED/INTERNAL_ERROR),
--              score INT DEFAULT 0, execution_time_ms INT, memory_used_mb INT,
--              submitted_at, evaluated_at
-- submission_results: id, submission_id FK, test_case_id FK, status, actual_output TEXT,
--                     execution_time_ms INT, error_message TEXT
-- Indexes: idx_submissions_student_id, idx_submissions_contest_id,
--          idx_submissions_problem_id, idx_submissions_status, idx_submissions_submitted_at
```

**V6 — leaderboard_snapshots**
```sql
-- leaderboard_snapshots: id, contest_id FK, student_id FK, total_score INT,
--                        solved_count INT, last_submission_at TIMESTAMPTZ,
--                        rank_position INT, snapshot_at TIMESTAMPTZ
-- Indexes: idx_leaderboard_contest_id, idx_leaderboard_student_id
```

**V7 — plagiarism_jobs, plagiarism_flags**
```sql
-- plagiarism_jobs: id, contest_id FK, triggered_by FK, status, started_at, completed_at
-- plagiarism_flags: id, job_id FK, submission_id FK, matched_submission_id FK,
--                   similarity_score DECIMAL(5,2), status (PENDING/REVIEWED/DISMISSED), created_at
```

**V8 — notifications, audit_logs, refresh_tokens**
```sql
-- notifications: id, user_id FK, title, message TEXT, is_read BOOLEAN, created_at
-- audit_logs: id, actor_id FK, action VARCHAR(64), resource_type, resource_id,
--             ip_address, user_agent, metadata JSONB, created_at
-- refresh_tokens: id, user_id FK, token_hash VARCHAR(64) UNIQUE,
--                 expires_at TIMESTAMPTZ, revoked BOOLEAN DEFAULT FALSE, created_at
-- Indexes: idx_notifications_user_id, idx_audit_logs_actor_id, idx_audit_logs_action,
--          idx_refresh_tokens_token_hash, idx_refresh_tokens_user_id
```

**V9 — indexes and constraints review**
```sql
-- Final pass: add any composite indexes needed for common query patterns
-- idx_submissions_contest_student: (contest_id, student_id) for leaderboard queries
-- idx_submissions_contest_problem: (contest_id, problem_id) for problem stats
-- idx_audit_logs_resource: (resource_type, resource_id) for resource history
```

#### Auth Module

**Files to create (in order):**
1. `User.java` entity + `Role.java` entity + `UserRole.java` join entity
2. `UserRepository.java` + `RoleRepository.java`
3. `RegisterRequest.java` + `LoginRequest.java` + `AuthResponse.java` (DTOs)
4. `UserMapper.java` (MapStruct)
5. `JwtService.java` — generate/validate access tokens, extract claims
6. `RefreshTokenService.java` — create, rotate, revoke refresh tokens
7. `AuthService.java` — register (BCrypt hash), login (verify + issue tokens), refresh, logout
8. `JwtAuthenticationFilter.java` — OncePerRequestFilter, reads Bearer token
9. `SecurityConfig.java` — filter chain, permitAll for /auth/**, ROLE_ hierarchy
10. `AuthController.java` — POST /register, POST /login, POST /refresh, POST /logout, GET /me

**Acceptance criteria:**
- POST /api/v1/auth/register → 201, returns `AuthResponse` with accessToken
- POST /api/v1/auth/login → 200, returns `AuthResponse` with accessToken + sets refresh cookie
- POST /api/v1/auth/refresh → 200, rotates refresh token, returns new accessToken
- GET /api/v1/auth/me (authenticated) → 200, returns user profile
- Expired token → 401
- Wrong password → 401
- Duplicate email → 409

#### Problem Module

**Files to create (in order):**
1. `Problem.java` entity + `TestCase.java` entity + `ProblemTag.java` entity
2. `ProblemRepository.java` + `TestCaseRepository.java`
3. Request/response DTOs: `CreateProblemRequest`, `ProblemResponse`, `ProblemSummaryResponse`
4. `TestCaseResponse.java` — **excludes `input_data` and `expected_output` for students**
5. `ProblemMapper.java` (MapStruct)
6. `ProblemService.java` — CRUD, enforce hidden test case protection at service layer
7. `ProblemController.java` — FACULTY+ to create/edit, all authenticated to read

**Acceptance criteria:**
- POST /api/v1/problems (FACULTY) → 201
- GET /api/v1/problems → 200, list (student-safe response — no hidden test case data)
- GET /api/v1/problems/{id} → 200 (student gets sample test cases only)
- GET /api/v1/problems/{id}/test-cases (FACULTY+) → 200, all test cases
- Hidden test cases (`is_sample=false`) never in student-accessible responses

#### Contest Module

**Files to create (in order):**
1. `Contest.java` entity + `ContestProblem.java` + `ContestParticipant.java`
2. `ContestRepository.java`
3. Request/response DTOs: `CreateContestRequest`, `ContestResponse`, `ContestSummaryResponse`
4. `ContestMapper.java`
5. `ContestService.java` — lifecycle management, registration, problem assignment
6. `ContestController.java`

**Acceptance criteria:**
- POST /api/v1/contests (FACULTY) → 201, status DRAFT
- POST /api/v1/contests/{id}/problems (FACULTY) → add problem to contest
- POST /api/v1/contests/{id}/register (STUDENT) → join contest
- GET /api/v1/contests → list with status filter
- GET /api/v1/contests/{id} → detail with problem list
- Status transitions: DRAFT → UPCOMING (when start_time set) → LIVE (at start) → ENDED (at end)

#### Submission Module (accept + queue only — not evaluation)

**Files to create (in order):**
1. `Submission.java` entity + `SubmissionResult.java` entity
2. `SubmissionRepository.java`
3. `CreateSubmissionRequest.java` + `SubmissionResponse.java` + `SubmissionStatusResponse.java`
4. `SubmissionMapper.java`
5. `EvaluationMessage.java` (the RabbitMQ message POJO)
6. `SubmissionService.java` — validate contest live + student registered, persist QUEUED, publish message
7. `SubmissionController.java`

**Acceptance criteria:**
- POST /api/v1/submissions (STUDENT) → 202, returns `{submissionId, status: "QUEUED"}`
- GET /api/v1/submissions/{id}/status → 200, returns current status
- GET /api/v1/submissions/{id} (STUDENT, own submission) → full submission detail
- Submitting to a non-LIVE contest → 422
- Submitting when not registered → 403
- Source code > 65536 chars → 400

**Week 1 done when:** `./mvnw clean package` succeeds, all 4 module tests pass, Swagger shows auth/problems/contests/submissions endpoints

---

### WEEK 2 — Async Evaluation Pipeline + Leaderboard
**Target: 2026-06-01 → 2026-06-07**
**Goal: End-to-end submission → evaluation → result → leaderboard works**

#### Infrastructure Configuration

**RabbitMQ queue setup (`infrastructure` module):**
```
Exchange: codejudgex.topic (topic exchange, durable)
Queues:
  evaluation.queue         → routing key: submission.created
  evaluation.retry         → x-message-ttl: 30000, x-dead-letter-exchange: codejudgex.topic
  evaluation.dlq           → terminal dead-letter destination
  notification.queue       → routing key: notification.requested
  plagiarism.queue         → routing key: plagiarism.requested
```

**Redis configuration:**
```
Keys:
  leaderboard:contest:{contestId}         → ZSET, member=userId, score=totalScore
  submission:status:{submissionId}        → STRING, TTL 3600s
  rate_limit:submission:{userId}:{contestId} → STRING counter, TTL 60s
  contest:stats:{contestId}              → HASH, TTL 300s
```

#### Evaluation Worker

**Files to create (in order):**
1. `Judge0Client.java` — RestTemplate/WebClient wrapper for Judge0 CE REST API
   - POST `/submissions?base64_encoded=true&wait=false` → returns `{token}`
   - GET `/submissions/{token}?base64_encoded=true` → polls for result
   - Map Judge0 status IDs to internal `SubmissionStatus` enum
2. `EvaluationWorker.java` — `@RabbitListener(queues = "evaluation.queue")`
   - Consume `EvaluationMessage`
   - Set submission status → RUNNING
   - For each test case: send to Judge0, poll result, compare output
   - Calculate score (sum of weights of passing test cases)
   - Persist all `SubmissionResult` rows
   - Update `Submission` with final status + score
   - Publish leaderboard update event
   - On exception: retry up to 3 times, then send to DLQ + mark INTERNAL_ERROR
3. `OutputComparator.java` — trim whitespace, normalize line endings, compare
4. `ScoreCalculator.java` — sum weights of ACCEPTED test case results

**Judge0 CE API reference (use these exact endpoints):**
```
POST   /submissions         body: {source_code (base64), language_id, stdin (base64), cpu_time_limit, memory_limit}
GET    /submissions/{token} → {status: {id, description}, stdout, stderr, time, memory, compile_output}

Status IDs that matter:
  1 = In Queue, 2 = Processing, 3 = Accepted, 4 = Wrong Answer,
  5 = Time Limit Exceeded, 6 = Compilation Error, 7-12 = Runtime Errors,
  13 = Internal Error, 14 = Exec Format Error

Language IDs:
  Java (JDK 17)  = 62
  C++ (GCC 9.2)  = 54
  Python 3.8     = 71
  JavaScript     = 63
  C (GCC 9.2)   = 50
```

**Acceptance criteria:**
- Submit Java code → status goes QUEUED → RUNNING → ACCEPTED/WRONG_ANSWER/etc.
- All per-test-case results stored in `submission_results`
- Correct verdict for: correct solution, wrong answer, TLE, CE
- Failed evaluation after 3 retries → INTERNAL_ERROR in DB + message in DLQ

#### Leaderboard Module

**Files to create:**
1. `LeaderboardEntry.java` entity + `LeaderboardSnapshot.java`
2. `LeaderboardRepository.java`
3. `LeaderboardService.java`
   - `updateScore(contestId, studentId, score, solvedCount)` — ZADD to Redis sorted set
   - `getTopN(contestId, n)` — ZREVRANGE with scores
   - `getStudentRank(contestId, studentId)` — ZREVRANK
   - `snapshotToPostgres(contestId)` — write current Redis state to leaderboard_snapshots
4. `LeaderboardController.java`

**Acceptance criteria:**
- GET /api/v1/leaderboards/contests/{id} → ranked list, updates within 1s of evaluation completing
- GET /api/v1/leaderboards/contests/{id}/me → authenticated student's own rank
- Redis miss falls back to latest PG snapshot

#### Notification Module

**Files to create:**
1. `Notification.java` entity + `NotificationRepository.java`
2. `NotificationMessage.java` (RabbitMQ POJO)
3. `NotificationWorker.java` — consume `notification.queue`, persist in-app notification
4. `EmailService.java` — JavaMailSender → MailHog SMTP (port 1025)
5. `NotificationController.java` — GET /notifications (authenticated), PATCH /{id}/read

**Acceptance criteria:**
- In-app notification created when: submission evaluated, contest starting in 30min
- Email sent via MailHog for: registration welcome, evaluation complete
- GET /api/v1/notifications → user's unread notifications

**Week 2 done when:** Submit code → get result → leaderboard updates → notification arrives. Full async flow working end-to-end.

---

### WEEK 3 — Security Hardening + Admin + Plagiarism
**Target: 2026-06-08 → 2026-06-14**
**Goal: Production-grade security posture + admin tooling + plagiarism pipeline**

#### Security Hardening

**Rate limiting (Redis-based):**
- Submission endpoint: max 5 submissions per student per contest per minute
- Auth endpoints: max 10 login attempts per IP per minute
- Implementation: `RateLimitInterceptor.java` using `RedisTemplate.opsForValue().increment()`

**CORS configuration:**
- Allow: `http://localhost:5173` (Vite dev), production frontend origin (configured via env var)
- Disallow: `*` in any environment

**Security headers (via Spring Security):**
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000 (prod only)
Content-Security-Policy: default-src 'self'
```

**Role enforcement audit — verify every controller has `@PreAuthorize`:**
- `STUDENT`: submit code, view own submissions, view contests, view problems (sample test cases only)
- `FACULTY`: all STUDENT permissions + create/edit problems, test cases, contests, view all submissions
- `ADMIN`: all FACULTY permissions + manage users, view audit logs, trigger plagiarism checks
- `SUPER_ADMIN`: all ADMIN permissions + manage admins, platform config

**Hidden test case audit:**
- Grep all `@GetMapping` and `@PostMapping` handlers that return anything related to `TestCase`
- Verify none return `is_sample=false` test cases to STUDENT role
- Service layer — not just controller — must filter

#### Audit Module

**Files to create:**
1. `AuditLog.java` entity + `AuditRepository.java`
2. `AuditEvent.java` enum — all events from section 18.1 of enterprise docs
3. `AuditService.java` — `record(actorId, action, resourceType, resourceId, metadata)`
4. `AuditAspect.java` — `@Around` AOP advice on `@Audited` annotated service methods
5. `AuditController.java` (ADMIN+) — GET /api/v1/audit-logs with filters

**Events to audit:** USER_REGISTERED, USER_LOGIN, ROLE_CHANGED, PROBLEM_CREATED, PROBLEM_UPDATED, TEST_CASE_CREATED, CONTEST_CREATED, CONTEST_STARTED, CONTEST_ENDED, SUBMISSION_CREATED, SUBMISSION_EVALUATED, LEADERBOARD_UPDATED, PLAGIARISM_CHECK_STARTED, PLAGIARISM_FLAGGED, ADMIN_REJUDGED_SUBMISSION

**Acceptance criteria:**
- Every listed event creates an audit_log row
- GET /api/v1/audit-logs (ADMIN) → paginated, filterable by action/actor/date

#### Plagiarism Module

**Files to create:**
1. `PlagiarismJob.java` + `PlagiarismFlag.java` entities + repositories
2. `JPlagService.java` — collect submission source files for a contest, run JPlag programmatically, parse results
3. `PlagiarismWorker.java` — consume `plagiarism.queue`, delegate to JPlagService, persist flags
4. `PlagiarismController.java`
   - POST /api/v1/plagiarism/contests/{id}/check (ADMIN+) → trigger check
   - GET /api/v1/plagiarism/contests/{id}/flags (ADMIN+) → list flags
   - PATCH /api/v1/plagiarism/flags/{id}/review (ADMIN+) → mark reviewed/dismissed

**Acceptance criteria:**
- Plagiarism check triggered → JPlag runs against all ACCEPTED submissions for contest
- Similarity pairs above threshold (default 70%) flagged in DB
- Admin sees flag list with similarity score + link to both submissions

#### Admin Module

**Files to create:**
1. `AdminController.java` — user management endpoints
2. `AnalyticsService.java` — contest stats, submission counts by verdict, active users
3. `AdminDashboardResponse.java` DTO

**Endpoints:**
- GET /api/v1/admin/users → paginated user list
- PATCH /api/v1/admin/users/{id}/role → change role
- PATCH /api/v1/admin/users/{id}/status → activate/deactivate
- GET /api/v1/admin/analytics → platform-wide stats
- GET /api/v1/admin/analytics/contests/{id} → per-contest stats

**Week 3 done when:** Security audit passes (every endpoint protected), audit log records all events, plagiarism check produces flags, admin can manage users.

---

### WEEK 4 — Frontend
**Target: 2026-06-15 → 2026-06-21**
**Goal: Complete React application covering all user roles and flows**

#### Build order (each step unblocks the next)

**Step 1 — Foundation**
- `src/types/` — all shared TypeScript types matching backend DTOs
- `src/lib/axios.ts` — single axios instance, JWT Bearer interceptor, 401 → redirect to /login
- `src/lib/queryClient.ts` — TanStack Query client, default staleTime 30s
- `src/stores/auth.store.ts` — Zustand: user, accessToken, setAuth, clearAuth
- `src/services/` — one service file per backend module (auth, problems, contests, submissions, leaderboard, admin, notifications)

**Step 2 — Auth**
- `RegisterPage.tsx` + `LoginPage.tsx` — React Hook Form + Zod + mutation
- `RequireAuth.tsx` + `RequireRole.tsx` — route guards
- Axios refresh token interceptor — silent token rotation on 401

**Step 3 — Student flows**
- `ContestListPage.tsx` — active/upcoming contests, join button
- `ContestDetailPage.tsx` — problem list, countdown timer, participant count
- `ProblemPage.tsx` — problem statement + Monaco editor + language selector + submit button
- `useSubmissionStatus.ts` — polling hook with TERMINAL_STATUSES stop condition
- `SubmissionResultPage.tsx` — verdict badge, per-test-case results (sample only), score
- `LeaderboardPage.tsx` — live-updating ranked table
- `SubmissionHistoryPage.tsx` — student's own past submissions

**Step 4 — Faculty flows**
- `CreateProblemPage.tsx` — form for problem + add test cases (mark hidden/sample)
- `CreateContestPage.tsx` — form, add problems, set start/end time
- `ContestSubmissionsPage.tsx` — all submissions for a contest, filter by student/problem/verdict
- `PlagiarismReviewPage.tsx` — flag list, similarity score, side-by-side code diff

**Step 5 — Admin flows**
- `UserManagementPage.tsx` — table, role change, activate/deactivate
- `AuditLogPage.tsx` — paginated, filterable
- `AdminDashboardPage.tsx` — platform metrics (submission throughput, active contests, error rate)

**Step 6 — Route wiring**
```typescript
// Route structure
/                     → redirect to /contests or /login
/login                → LoginPage (public)
/register             → RegisterPage (public)
/contests             → ContestListPage (RequireAuth)
/contests/:id         → ContestDetailPage (RequireAuth)
/contests/:id/problems/:problemId → ProblemPage (RequireAuth + STUDENT)
/submissions/:id      → SubmissionResultPage (RequireAuth)
/leaderboard/:contestId → LeaderboardPage (RequireAuth)
/faculty/problems/new → CreateProblemPage (RequireRole FACULTY)
/faculty/contests/new → CreateContestPage (RequireRole FACULTY)
/faculty/contests/:id/submissions → ContestSubmissionsPage (RequireRole FACULTY)
/faculty/plagiarism/:contestId → PlagiarismReviewPage (RequireRole FACULTY)
/admin/users          → UserManagementPage (RequireRole ADMIN)
/admin/audit-logs     → AuditLogPage (RequireRole ADMIN)
/admin/dashboard      → AdminDashboardPage (RequireRole ADMIN)
```

**Week 4 done when:** `npm run typecheck` passes, all pages render without runtime errors, student can complete full submit-and-see-result flow in the browser.

---

### WEEK 5 — Observability, CI/CD, Polish
**Target: 2026-06-22 → 2026-06-28**
**Goal: Production-ready deployment, monitoring, and end-to-end test coverage**

#### Observability

**Custom metrics via Spring Boot Actuator + Micrometer (add to `infrastructure` module), exposed at `/actuator/prometheus` for optional external scraping:**
```java
// Counters
submissions_total{status="ACCEPTED|WRONG_ANSWER|..."}
evaluation_errors_total

// Gauges
submissions_queued_current
active_contests_current

// Histograms
evaluation_duration_seconds
api_request_duration_seconds{endpoint, method}
```

**Health and metrics available without any extra setup:**
- `/actuator/health` — overall app, DB, Redis, RabbitMQ health indicators
- `/actuator/metrics` — JVM heap, request latency, custom counters/gauges above
- `/actuator/prometheus` — Prometheus-format scrape endpoint (optional external Prometheus/Grafana can point here)

#### Integration Tests

**Backend integration tests (Testcontainers):**
```java
// Required test coverage:
SubmissionEvaluationIntegrationTest:
  - submit valid Java solution → ACCEPTED after evaluation
  - submit wrong solution → WRONG_ANSWER
  - submit TLE solution → TIME_LIMIT_EXCEEDED
  - failed evaluation → DLQ after 3 retries + INTERNAL_ERROR status

AuthIntegrationTest:
  - register → login → access protected endpoint → refresh → logout

ContestLifecycleIntegrationTest:
  - create contest → add problems → register student → submit → leaderboard updates
```

#### GitHub Actions CI (`/github/workflows/`)

**`backend-ci.yml`:** checkout → setup Java 21 → `./mvnw test` → `./mvnw clean package -DskipTests`
**`frontend-ci.yml`:** checkout → setup Node 20 → `npm ci` → `npm run typecheck` → `npm run lint` → `npm run build`

#### Final polish checklist
- [ ] All `TODO` comments removed or linked to a GitHub issue
- [ ] No hardcoded values — all via env vars (verify with `grep -r "localhost" src/`)
- [ ] `infra/.env.example` has every required variable with description
- [ ] Swagger UI accessible at `/swagger-ui.html` with all endpoints documented
- [ ] README quick-start works from a fresh clone using local services (PostgreSQL, Redis, RabbitMQ) and a Judge0 CE remote/hosted instance
- [ ] `./mvnw clean package` succeeds (no test failures)
- [ ] `npm run build` succeeds (no type errors, no lint errors)

**Week 5 done when:** backend and frontend run against local PostgreSQL, Redis, and RabbitMQ services plus a Judge0 CE remote/hosted instance, full contest lifecycle works end-to-end, `/actuator/health` and `/actuator/prometheus` report correctly, CI pipeline goes green.

---

## Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Judge0 CE has no practical non-Docker local install | High | High | Use a remote/hosted Judge0 instance (e.g. Judge0 RapidAPI) configured via `JUDGE0_URL` + `JUDGE0_TOKEN`; test early in Week 2 |
| JPlag memory usage on large contest (>500 submissions) | Medium | Medium | Run JPlag async via queue; set JVM heap limit on worker; defer to Week 3 |
| RabbitMQ message loss on worker crash | Medium | High | durable queues + manual ack + idempotent consumer (check submission status before processing) |
| Redis eviction during contest causing leaderboard loss | Low | High | Set `maxmemory-policy noeviction` for leaderboard Redis; PG snapshot every 5 min |
| Frontend polling overwhelming API under load | Medium | Medium | `/submissions/{id}/status` endpoint cached in Redis (TTL 2s); stop polling on terminal status |
| No CI/CD until Week 5 | Certain | Low | Manual test each module at end of its week against locally running services |
| Monaco editor SSR/Vite chunk size | Low | Low | Lazy-load Monaco with `React.lazy` + `Suspense`; specify `height` always |

---

## Module Acceptance Checklist

Before marking any module "done", ALL must be true:

- [ ] `./mvnw compile` succeeds with this module included
- [ ] Unit tests written and passing for the service class
- [ ] At least one integration test with Testcontainers
- [ ] Controller tested with MockMvc (happy path + one error case)
- [ ] All endpoints documented in Swagger (via `@Operation` / `@ApiResponse`)
- [ ] No `@Entity` class returned from any `@RestController` method
- [ ] Hidden test case data (`is_sample=false`) not reachable by STUDENT role
- [ ] No hardcoded values — all config via `application.yml` env vars
- [ ] `error-memory.md` checked — not repeating a known bug pattern

---

## Session Startup Quick Reference (for AI agents)

Every session, before touching code:

1. Read `session-continuity.md` — what module was last touched
2. Read `active-goals.md` — what week are we in, what's checked off
3. Read `error-memory.md` — what bugs to avoid
4. Find the current module in this ROADMAP — read its full section
5. Follow the "Files to create (in order)" list for that module
6. Verify against "Acceptance criteria" before marking done
7. Run `./mvnw compile` after each module completes
8. Update `active-goals.md` checkbox + write session digest

---

*Last updated: 2026-05-24 | Tool: claude-code*
