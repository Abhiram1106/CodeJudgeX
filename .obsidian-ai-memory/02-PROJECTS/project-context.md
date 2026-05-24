---
type: project-context
updated: 2026-05-24
tags: [context, stack, constraints]
---

# Project Context — CodeJudgeX

## Identity

**Name:** CodeJudgeX
**Type:** Enterprise-grade competitive programming judge for academic institutions
**Phase:** Implementation — Week 1 starting. Scaffold + AI infra + ROADMAP complete. No source code yet.
**Positioning:** Zero-cost, self-hosted, production-inspired coding assessment platform.

## Problem it solves

Academic institutions (colleges, training institutes, coding clubs) run assessments using Google Forms,
spreadsheets, WhatsApp, manual evaluation, or paid platforms. CodeJudgeX replaces all of that with
an owned, self-hosted platform providing: automated code evaluation, hidden test cases, leaderboards,
plagiarism detection, audit logs, and institutional analytics.

## Target users

| Role | Can do |
|---|---|
| STUDENT | Register, join contests, submit code, view own results, see leaderboard |
| FACULTY | All student actions + create problems/test cases/contests, view all submissions, trigger plagiarism checks |
| ADMIN | All faculty actions + manage users, view audit logs, platform analytics |
| SUPER_ADMIN | All admin actions + manage admins, global config |

## Stack

| Layer | Technology | Path |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.3 | `backend/` |
| Backend package root | `com.codejudgex` | `backend/src/main/java/com/codejudgex/` |
| Frontend | React 18 + Vite 5 + TypeScript 5.4 | `frontend/` |
| Styling | Tailwind CSS 3 + shadcn/ui | `frontend/src/` |
| Code editor | Monaco Editor | `frontend/src/components/editor/` |
| State management | TanStack Query v5 + Zustand v4 | `frontend/src/` |
| Forms | React Hook Form + Zod | `frontend/src/` |
| Primary DB | PostgreSQL 16 | `infra/docker-compose.yml` |
| Cache / Leaderboard | Redis 7 | `infra/docker-compose.yml` |
| Message queue | RabbitMQ 3 (management UI: 15672) | `infra/docker-compose.yml` |
| Code execution | Judge0 CE | `infra/docker-compose.yml` |
| Similarity detection | JPlag | triggered post-contest via plagiarism.queue |
| Monitoring | Prometheus + Grafana | `infra/prometheus/` + `infra/grafana/` |
| Email (dev) | MailHog | `infra/docker-compose.yml` |
| Migrations | Flyway | `backend/src/main/resources/db/migration/` |
| Build | Maven (mvnw) | `backend/pom.xml` |
| API docs | springdoc-openapi + Swagger UI | `/swagger-ui.html` |

## Architecture decisions (see decisions.md for full rationale)

| # | Decision | Rule |
|---|---|---|
| D-001 | Modular monolith | One Spring Boot app. Never split unless explicitly decided. |
| D-002 | Async evaluation | POST /submissions → 202 immediately. Never synchronous. |
| D-003 | PostgreSQL = source of truth | Redis = speed layer only. All data reconstructible from PG. |
| D-004 | MapStruct DTOs | JPA entities NEVER in API responses. Always map via MapStruct. |
| D-005 | Judge0 CE for execution | Spring Boot never calls Runtime.exec() or ProcessBuilder on user input. |
| D-006 | Flyway migrations | ddl-auto=validate always. Hibernate never creates/drops tables. |

## The 16 backend modules

| Module | Package | Responsibility |
|---|---|---|
| auth | com.codejudgex.auth | Register, login, JWT, refresh tokens, logout, /me |
| user | com.codejudgex.user | Profile, department/year, status |
| role | com.codejudgex.role | Role entity, assignment, STUDENT/FACULTY/ADMIN/SUPER_ADMIN |
| problem | com.codejudgex.problem | Problem CRUD, difficulty, time/memory limits |
| testcase | com.codejudgex.testcase | Sample + hidden test cases, weights, visibility |
| contest | com.codejudgex.contest | Contest lifecycle DRAFT→UPCOMING→LIVE→ENDED, registration |
| submission | com.codejudgex.submission | Accept code, persist QUEUED, publish to RabbitMQ, return 202 |
| evaluation | com.codejudgex.evaluation | RabbitMQ consumer, Judge0 CE client, output compare, score |
| leaderboard | com.codejudgex.leaderboard | Redis sorted sets, PG snapshots, rank calculation |
| plagiarism | com.codejudgex.plagiarism | JPlag integration, similarity flags, admin review |
| notification | com.codejudgex.notification | In-app + MailHog email, notification queue consumer |
| audit | com.codejudgex.audit | Append-only event log for all critical actions |
| admin | com.codejudgex.admin | User management, platform analytics, system settings |
| analytics | com.codejudgex.analytics | Contest stats, submission trends, faculty reports |
| common | com.codejudgex.common | ApiResponse wrapper, exceptions, base entities, constants |
| infrastructure | com.codejudgex.infrastructure | RabbitMQ config, Redis config, Judge0 client bean, security config |

## Critical constraints (never violate)

1. Hidden test cases (`is_sample=false`) MUST NEVER appear in any student-facing API response — enforce at SERVICE layer, not just controller
2. Code evaluation is ALWAYS asynchronous — POST /submissions returns 202, never evaluates inline
3. Spring Boot NEVER calls Runtime.exec() or ProcessBuilder on user input — Judge0 CE handles all execution
4. JPA entities NEVER in API responses — always MapStruct → response DTO
5. Secrets ALWAYS from environment variables — never hardcoded in any file
6. Controllers are THIN — validate → delegate to service → return DTO

## Submission status lifecycle

QUEUED → RUNNING → ACCEPTED | WRONG_ANSWER | PARTIALLY_ACCEPTED |
COMPILATION_ERROR | RUNTIME_ERROR | TIME_LIMIT_EXCEEDED | MEMORY_LIMIT_EXCEEDED | INTERNAL_ERROR

## RabbitMQ queues

| Queue | Routing key | Purpose |
|---|---|---|
| evaluation.queue | submission.created | Evaluation worker input |
| evaluation.retry | — | TTL 30s, dead-letters back to evaluation.queue |
| evaluation.dlq | — | Terminal failure destination |
| notification.queue | notification.requested | Notification worker input |
| plagiarism.queue | plagiarism.requested | JPlag worker input |

## Redis key design

| Key | Type | TTL | Purpose |
|---|---|---|---|
| leaderboard:contest:{contestId} | ZSET | none | Live leaderboard (score = totalScore) |
| submission:status:{submissionId} | STRING | 3600s | Status cache during evaluation |
| rate_limit:submission:{userId}:{contestId} | STRING | 60s | Rate limit counter |
| contest:stats:{contestId} | HASH | 300s | Contest stats cache |

## Key ports (local)

| Service | Port |
|---|---|
| Backend API | 8080 |
| Frontend dev (Vite) | 5173 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management UI | 15672 |
| Judge0 CE | 2358 |
| Prometheus | 9090 |
| Grafana | 3001 |
| MailHog UI | 8025 |

## Judge0 CE language IDs

| Language | ID |
|---|---|
| Java (JDK 17) | 62 |
| C++ (GCC 9.2) | 54 |
| Python 3.8 | 71 |
| JavaScript | 63 |
| C (GCC 9.2) | 50 |

## Do not repeat

- See `03-ERRORS/error-memory.md` for known bugs
- See `03-ERRORS/anti-patterns.md` for patterns to avoid

## Known risks

- Judge0 CE requires Docker privileged mode on Windows — test early in Week 2
- JPlag memory usage for large contests — only trigger post-contest, never live
- Redis eviction can corrupt leaderboard — set maxmemory-policy noeviction
- No CI/CD until Week 5 — manual verification per module until then

## Key documents

| Document | Path | Purpose |
|---|---|---|
| Build roadmap | `docs/ROADMAP.md` | Week-by-week plan, module file order, acceptance criteria |
| Enterprise docs | `docs/enterprise_documentation.md` | Full feature spec, all 33 sections |
| API design | `docs/api_design.md` | Endpoint reference |
| Database design | `docs/database_design.md` | Schema reference |
| Security | `docs/security.md` | CIA model, auth design |
| Architecture | `docs/architecture.md` | System overview |
