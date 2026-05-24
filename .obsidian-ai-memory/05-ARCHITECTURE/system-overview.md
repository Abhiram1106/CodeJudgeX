---
type: architecture
updated: 2026-05-23
tags: [architecture, system-design]
---

# System Architecture — CodeJudgeX

## Pattern

**Modular Monolith + Async Workers** (see D-001, D-002)

## Component map

```
React Frontend (Vite + TypeScript)
  │  HTTP REST /api/v1
  ▼
Spring Boot REST API (Java 21, port 8080)
  │  JPA + HikariCP
  ├──► PostgreSQL 16 (source of truth, port 5432)
  │  Spring Data Redis
  ├──► Redis 7 (leaderboard, cache, rate limiting, port 6379)
  │  Spring AMQP publish
  └──► RabbitMQ 3 (port 5672)
         │  @RabbitListener consume
         ▼
      Evaluation Worker (same Spring Boot process, separate @Component)
         │  HTTP REST
         ├──► Judge0 CE (sandboxed execution, port 2358)
         │  write results
         ├──► PostgreSQL (submission results, leaderboard snapshots)
         │  ZADD
         ├──► Redis (live leaderboard)
         │  publish
         └──► RabbitMQ notification.queue

Prometheus (port 9090) ◄── scrapes /actuator/prometheus from Spring Boot
Grafana (port 3001)   ◄── queries Prometheus
```

## Module boundaries

```
com.codejudgex/
├── auth/          JWT authentication — login, register, refresh, logout
├── user/          User profiles and status
├── role/          Role assignment (STUDENT, FACULTY, ADMIN, SUPER_ADMIN)
├── problem/       Problem CRUD, difficulty, tags, constraints
├── testcase/      Sample + hidden test cases (hidden never returned to STUDENT)
├── contest/       Contest lifecycle (DRAFT→UPCOMING→LIVE→ENDED→ARCHIVED)
├── submission/    Accept code → validate → save QUEUED → publish to RabbitMQ → 202
├── evaluation/    RabbitMQ consumer → Judge0 → compare → score → leaderboard
├── leaderboard/   Redis sorted sets (live) + PostgreSQL snapshots (persistent)
├── plagiarism/    JPlag post-contest → flags → faculty review workflow
├── notification/  In-app + MailHog email via notification queue
├── audit/         Append-only audit events (login, create, evaluate, admin actions)
├── admin/         User management, platform analytics, system health
├── analytics/     Aggregated statistics for admin dashboard
├── common/        ApiResponse<T>, BaseEntity, pagination, global exception handler
└── infrastructure/ RabbitMQ queue config, Redis config, SecurityFilterChain, Swagger
```

## Critical data flows

### Submission (async — the most important flow)

```
POST /api/v1/submissions
  → validate JWT + contest access
  → save Submission{QUEUED} → PostgreSQL
  → publish to evaluation.queue → RabbitMQ
  → return {submissionId, QUEUED} HTTP 202

EvaluationWorker (RabbitMQ consumer)
  → fetch submission + problem + ALL hidden test cases
  → update RUNNING
  → for each test case: POST to Judge0 → poll → compare output
  → calculate score (weighted sum of passing test cases)
  → determine verdict (ACCEPTED only if ALL pass)
  → update submission + save results → PostgreSQL
  → ZADD leaderboard → Redis
  → publish to notification.queue
```

### Plagiarism check (post-contest only)

```
POST /api/v1/contests/{id}/plagiarism/check
  → validate FACULTY+ role
  → create PlagiarismJob{QUEUED}
  → publish to plagiarism.queue → 202

PlagiarismWorker
  → collect all submissions for contest
  → run JPlag similarity analysis
  → store similarity reports + flags
  → faculty reviews flags via API
```

## Consistency model

| Data | Consistency | Mechanism |
|---|---|---|
| Users, problems, contests, submissions (record) | Strong | Synchronous PostgreSQL write |
| Evaluation results, verdicts | Eventual | Async worker pipeline |
| Live leaderboard | Eventual | Redis update after evaluation |
| Notifications | Eventual | Notification queue |
| Plagiarism reports | Eventual | Post-contest background |

## Fault tolerance

| Failure | Response |
|---|---|
| Evaluation worker crash | RabbitMQ re-delivers unacked message |
| Judge0 unavailable | Nack → retry.queue (TTL) → DLQ after 3 attempts |
| Redis unavailable | Leaderboard falls back to PostgreSQL read |
| Duplicate message delivery | Idempotency: check if submission already has terminal status |
| Persistent evaluation failure | Message in DLQ — manual investigation |
