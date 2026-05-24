# Backend Context — CodeJudgeX

> @-include this file when working on backend/ code.

## Stack

- Java 21 (virtual threads available via `@EnableVirtualThreads`)
- Spring Boot 3.3
- Spring Security 6 (method-level `@PreAuthorize`, filter chain config)
- Spring Data JPA + Hibernate 6
- PostgreSQL 16 (via HikariCP connection pool)
- Redis 7 (Spring Data Redis — `RedisTemplate` + `@Cacheable`)
- RabbitMQ 3 (Spring AMQP — `RabbitTemplate` + `@RabbitListener`)
- MapStruct 1.5 (annotation processor — runs at compile time)
- Lombok (compile-time only — excluded from final JAR)
- Jakarta Validation (`@Valid` on controller params, `@NotBlank` etc. on DTOs)
- Flyway (migrations in `backend/src/main/resources/db/migration/`)
- springdoc-openapi (Swagger UI at `/swagger-ui`, OpenAPI JSON at `/v3/api-docs`)
- Micrometer + Prometheus registry (metrics at `/actuator/prometheus`)

## Module map

```
com.codejudgex
├── CodeJudgeXApplication.java
├── auth/            JWT auth: register, login, refresh, logout, /me
├── user/            User profile: STUDENT, FACULTY, ADMIN, SUPER_ADMIN
├── role/            Role assignment, permission mapping
├── problem/         Problem CRUD, difficulty (EASY/MEDIUM/HARD), tags
├── testcase/        Sample + hidden test cases, weights, visibility
├── contest/         Contest lifecycle: DRAFT→UPCOMING→LIVE→ENDED→ARCHIVED
├── submission/      Code submission, validates, publishes to RabbitMQ, returns 202
├── evaluation/      RabbitMQ consumer: fetch→Judge0→compare→score→leaderboard
├── leaderboard/     Redis sorted sets (live) + PostgreSQL snapshots (history)
├── plagiarism/      JPlag, triggered post-contest, similarity flags, faculty review
├── notification/    In-app + email (MailHog dev), RabbitMQ notification queue consumer
├── audit/           Append-only audit events: login, create, evaluate, admin actions
├── admin/           Admin-only: user mgmt, platform analytics, system health
├── analytics/       Aggregated stats for admin dashboard
├── common/          ApiResponse<T>, ApiErrorResponse, BaseEntity, pagination utils
└── infrastructure/  RabbitMQ config, Redis config, SecurityConfig, JPA config
```

## Standard module structure

```
module/
├── controller/       @RestController, thin — validate → service → return DTO
├── dto/
│   ├── request/      Inbound DTOs with Jakarta Validation annotations
│   └── response/     Outbound DTOs — entities never exposed directly
├── entity/           @Entity JPA classes — never serialized to JSON
├── repository/       @Repository extends JpaRepository<T, UUID>
├── service/          @Service — all business logic, @Transactional where needed
├── mapper/           @Mapper (MapStruct) — entity ↔ DTO conversions
└── exception/        Module-specific exception types
```

## Common patterns

### Standard API response wrapper

```java
// com.codejudgex.common.ApiResponse<T>
{
  "success": true,
  "message": "...",
  "data": { },
  "timestamp": "2026-05-23T10:30:00Z"
}
```

### Submission flow (the critical async path)

```
POST /api/v1/submissions
  → SubmissionController.submit()
  → SubmissionService.submit()
     → validate contest access + rate limit
     → save Submission{status: QUEUED} to PostgreSQL
     → rabbitTemplate.convertAndSend("evaluation.queue", evaluationJobDto)
     → return SubmissionResponse{submissionId, status: QUEUED}
  → HTTP 202 Accepted

EvaluationWorker (RabbitMQ consumer)
  → fetch Submission + Problem + hidden TestCases
  → update status: RUNNING
  → call Judge0 CE REST API
  → compare actual output vs expected output per test case
  → calculate score
  → update Submission{status: ACCEPTED|WRONG_ANSWER|..., score}
  → update Redis leaderboard (ZADD contest:{contestId}:leaderboard score studentId)
  → publish to notification.queue
```

### JWT configuration

- Access token: 15 min (configurable via `app.jwt.expiration-ms`)
- Refresh token: 7 days, stored hashed in `refresh_tokens` table
- Signing: HMAC-SHA256 with `app.jwt.secret` from env
- Filter: `JwtAuthenticationFilter extends OncePerRequestFilter`

### RabbitMQ queues

| Queue | Purpose |
|---|---|
| `evaluation.queue` | Submission evaluation jobs |
| `evaluation.retry` | Retry queue (with TTL → DLQ) |
| `evaluation.dlq` | Dead-letter queue for investigation |
| `notification.queue` | Notification delivery jobs |
| `plagiarism.queue` | Post-contest similarity check jobs |

## Key env vars (from `infra/.env`)

```
DB_URL, DB_USER, DB_PASS
REDIS_HOST, REDIS_PORT
RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS
JUDGE0_URL, JUDGE0_TOKEN
JWT_SECRET
```

## Current implementation status

No source files exist yet — scaffold only. Start from module design, then entity → migration → repository → service → controller.
