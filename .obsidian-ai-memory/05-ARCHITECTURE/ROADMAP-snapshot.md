---
type: roadmap-snapshot
updated: 2026-05-24
source: docs/ROADMAP.md
tags: [roadmap, build-plan, reference]
---

# ROADMAP Snapshot — CodeJudgeX

> Mirror of `docs/ROADMAP.md` kept in vault for offline agent retrieval.
> If this diverges from docs/ROADMAP.md, the docs/ version is authoritative.
> Last synced: 2026-05-24

---

## Timeline

5 weeks · 2026-05-25 → 2026-06-28
Stack: Java 21 + Spring Boot 3.3 · React 18 + Vite · PostgreSQL · Redis · RabbitMQ · Judge0 CE · Docker Compose

---

## Architectural Decisions (non-negotiable)

| # | Decision | Rule |
|---|---|---|
| D-001 | Modular monolith | One Spring Boot app. No microservices. |
| D-002 | Async evaluation | POST /submissions returns 202 immediately. Never synchronous. |
| D-003 | PostgreSQL = source of truth | Redis is speed layer only. |
| D-004 | MapStruct DTOs | JPA entities never in API responses. Period. |
| D-005 | Judge0 CE for execution | Spring Boot never calls Runtime.exec() on user input. |
| D-006 | Flyway migrations | ddl-auto=validate always. Hibernate never creates/drops tables. |

---

## Critical Path

```
Layer 0: Infrastructure config (RabbitMQ, Redis, Security)
Layer 1: Flyway V1–V9 (database schema)
Layer 2: Auth module
Layer 3: User + Role modules
Layer 4: Problem + TestCase modules
Layer 5: Contest module
Layer 6: Submission module (accept + queue)
Layer 7: Evaluation worker (RabbitMQ → Judge0 CE)
Layer 8: Leaderboard (Redis + PG)
Layer 9: Notification + Audit
Layer 10: Plagiarism + Admin
Layer 11: Frontend
Layer 12: Observability + CI/CD + Polish
```

---

## Week 1 — Database Foundation + Backend Core

**Files: V1–V9 SQL → common → infrastructure → auth → problem → contest → submission**

### Flyway Migration Summary

| File | Tables |
|---|---|
| V1__create_users_roles.sql | users, roles, user_roles |
| V2__create_problems.sql | problems, problem_tags, problem_tag_map |
| V3__create_test_cases.sql | test_cases |
| V4__create_contests.sql | contests, contest_problems, contest_participants |
| V5__create_submissions.sql | submissions, submission_results |
| V6__create_leaderboard.sql | leaderboard_snapshots |
| V7__create_plagiarism.sql | plagiarism_jobs, plagiarism_flags |
| V8__create_supporting.sql | notifications, audit_logs, refresh_tokens |
| V9__add_indexes.sql | composite indexes |

### Auth Module File Order

1. User.java + Role.java + UserRole.java entities
2. UserRepository + RoleRepository
3. RegisterRequest + LoginRequest + AuthResponse DTOs
4. UserMapper (MapStruct)
5. JwtService — generate/validate access tokens
6. RefreshTokenService — create, rotate, revoke
7. AuthService — register, login, refresh, logout
8. JwtAuthenticationFilter (OncePerRequestFilter)
9. SecurityConfig — filter chain, role hierarchy
10. AuthController — /register /login /refresh /logout /me

### Problem Module File Order

1. Problem + TestCase + ProblemTag entities
2. ProblemRepository + TestCaseRepository
3. CreateProblemRequest + ProblemResponse + ProblemSummaryResponse + TestCaseResponse DTOs
4. ProblemMapper
5. ProblemService — CRUD + hidden test case protection at service layer
6. ProblemController

### Contest Module File Order

1. Contest + ContestProblem + ContestParticipant entities
2. ContestRepository
3. CreateContestRequest + ContestResponse + ContestSummaryResponse DTOs
4. ContestMapper
5. ContestService — lifecycle, registration, problem assignment
6. ContestController

### Submission Module File Order

1. Submission + SubmissionResult entities
2. SubmissionRepository
3. CreateSubmissionRequest + SubmissionResponse + SubmissionStatusResponse DTOs
4. EvaluationMessage POJO
5. SubmissionMapper
6. SubmissionService — validate, persist QUEUED, publish to RabbitMQ
7. SubmissionController

---

## Week 2 — Async Evaluation Pipeline + Leaderboard

Judge0 CE language IDs: Java=62, C++=54, Python=71, JS=63, C=50
Judge0 status IDs: 1=Queue, 2=Processing, 3=Accepted, 4=WA, 5=TLE, 6=CE, 7-12=RTE, 13=IE

RabbitMQ queues: evaluation.queue, evaluation.retry (TTL 30s), evaluation.dlq, notification.queue, plagiarism.queue
Redis keys: leaderboard:contest:{id} (ZSET), submission:status:{id} (STRING TTL 3600), rate_limit:submission:{uid}:{cid}

Key files: Judge0Client → EvaluationWorker → OutputComparator → ScoreCalculator → LeaderboardService → NotificationWorker

---

## Week 3 — Security + Audit + Plagiarism + Admin

Rate limit: 5 submissions/student/contest/minute via Redis
CORS: localhost:5173 only, never wildcard
Audit events: USER_REGISTERED, USER_LOGIN, ROLE_CHANGED, PROBLEM_CREATED, CONTEST_CREATED, SUBMISSION_CREATED, SUBMISSION_EVALUATED + 10 more

---

## Week 4 — Frontend

Build order: types → axios/queryClient → Zustand → services → auth pages → guards → student flows → faculty flows → admin flows → routes

---

## Week 5 — Observability + CI/CD + Polish

Prometheus metrics: submissions_total, evaluation_errors_total, submissions_queued_current, evaluation_duration_seconds
Grafana: 6 panels (throughput, queue depth, eval duration P50/P95/P99, error rate, JVM heap, active contests)
CI: backend-ci.yml + frontend-ci.yml + docker-ci.yml
Done when: docker compose up --build works end-to-end

---

## Module Acceptance Checklist

Before any module is "done":
- [ ] mvnw compile passes
- [ ] Unit tests written and passing for the service class
- [ ] Integration test with Testcontainers
- [ ] Controller tested with MockMvc (happy path + one error case)
- [ ] No @Entity in any @RestController response
- [ ] Hidden test case data not reachable by STUDENT role
- [ ] No hardcoded values — all config via application.yml env vars
