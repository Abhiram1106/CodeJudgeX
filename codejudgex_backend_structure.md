# CodeJudgeX Backend Structure

## 1. Purpose

This document defines the backend structure for **CodeJudgeX**.

The backend is built using Java 21 and Spring Boot 3. It follows a modular monolith architecture with clean package boundaries, layered design, DTO-based APIs, service-layer business logic, repository-based persistence, and asynchronous workers.

---

## 2. Backend Architecture Style

Chosen style:

```text
Modular Monolith
```

Reason:

```text
simple to build
simple to test
simple to deploy
clean module boundaries
avoids fake microservice complexity
can be split later if required
```

CodeJudgeX should not start as microservices.

---

## 3. Backend Root Package

Recommended root package:

```text
com.codejudgex
```

Main structure:

```text
com.codejudgex
├── CodeJudgeXApplication.java
├── auth
├── user
├── role
├── problem
├── testcase
├── contest
├── submission
├── evaluation
├── leaderboard
├── plagiarism
├── notification
├── audit
├── admin
├── analytics
├── common
└── infrastructure
```

---

## 4. Standard Module Structure

Each major module should follow this pattern:

```text
module-name/
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── repository
├── service
├── mapper
├── validator optional
└── exception optional
```

Example:

```text
problem/
├── controller
│   └── ProblemController.java
├── dto
│   ├── request
│   │   └── CreateProblemRequest.java
│   └── response
│       └── ProblemResponse.java
├── entity
│   └── Problem.java
├── repository
│   └── ProblemRepository.java
├── service
│   ├── ProblemService.java
│   └── ProblemServiceImpl.java
├── mapper
│   └── ProblemMapper.java
└── exception
    └── ProblemNotFoundException.java
```

---

## 5. Layered Design

Use this flow:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

For async workflows:

```text
Controller
    ↓
Service
    ↓
RabbitMQ Producer
    ↓
Worker Consumer
    ↓
Service
    ↓
Repository
```

---

## 6. Controller Layer

Responsibilities:

```text
accept HTTP requests
validate request DTOs
call service layer
return response DTOs
apply role annotations
avoid business logic
```

Controllers should not:

```text
access repositories directly
contain business rules
return JPA entities
perform code evaluation
handle complex transactions
```

Example annotations:

```java
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Validated
```

---

## 7. DTO Layer

Use DTOs for every API request and response.

Request DTO examples:

```text
RegisterRequest
LoginRequest
CreateProblemRequest
CreateContestRequest
SubmitCodeRequest
CreateTestCaseRequest
```

Response DTO examples:

```text
AuthResponse
UserResponse
ProblemResponse
ContestResponse
SubmissionResponse
LeaderboardResponse
ApiErrorResponse
```

Rules:

```text
Do not expose JPA entities directly.
Do not include password_hash in response DTOs.
Do not include hidden test cases in student-facing DTOs.
Do not include full source code in list DTOs.
```

---

## 8. Service Layer

Responsibilities:

```text
business logic
transaction handling
permission checks when needed
orchestration between repositories and infrastructure
publishing async jobs
calling Judge0 integration service
updating leaderboard
creating audit logs
```

Service naming:

```text
ProblemService
ContestService
SubmissionService
EvaluationService
LeaderboardService
AuditService
```

Implementation naming:

```text
ProblemServiceImpl
ContestServiceImpl
SubmissionServiceImpl
```

---

## 9. Repository Layer

Use Spring Data JPA repositories.

Example:

```java
public interface ProblemRepository extends JpaRepository<Problem, UUID> {
    Optional<Problem> findBySlug(String slug);
}
```

Rules:

```text
repositories should only handle database access
avoid business logic inside repositories
use pagination for list queries
use custom queries only when needed
avoid N+1 query problems
```

---

## 10. Mapper Layer

Use:

```text
MapStruct
```

Purpose:

```text
convert entity to response DTO
convert request DTO to entity
avoid manual repetitive mapping
keep controllers clean
```

Example:

```text
ProblemMapper
ContestMapper
SubmissionMapper
UserMapper
```

---

## 11. Entity Layer

Entities represent database tables.

Rules:

```text
use UUID primary keys
use created_at and updated_at fields
use enums for controlled states
avoid exposing entities in API
keep entity relationships clear
avoid unnecessary eager fetching
```

Recommended default fetch strategy:

```text
LAZY
```

---

## 12. Common Package

The `common` package contains shared backend utilities.

Structure:

```text
common/
├── config
├── exception
├── response
├── security
├── util
├── constants
└── validation
```

Use for:

```text
global exception handler
standard API response
pagination response
common constants
request ID filter
validation utilities
```

---

## 13. Infrastructure Package

The `infrastructure` package contains external integrations.

Structure:

```text
infrastructure/
├── rabbitmq
├── redis
├── judge0
├── jplag
├── mail
├── storage optional
└── monitoring
```

Responsibilities:

```text
RabbitMQ producers/consumers
Redis configuration
Judge0 API client
JPlag execution adapter
MailHog email sender
Prometheus metrics configuration
```

---

## 14. Auth Module

Structure:

```text
auth/
├── controller
├── dto
├── service
├── security
├── entity
├── repository
└── mapper
```

Responsibilities:

```text
register
login
refresh token
logout
JWT generation
JWT validation
password hashing
refresh token storage
```

Important classes:

```text
AuthController
AuthService
JwtService
RefreshTokenService
CustomUserDetailsService
JwtAuthenticationFilter
SecurityConfig
```

---

## 15. Submission Module

Structure:

```text
submission/
├── controller
├── dto
├── entity
├── repository
├── service
├── mapper
└── event
```

Responsibilities:

```text
accept submissions
validate contest rules
save submission as QUEUED
publish evaluation job
return submission ID
show submission history
```

Important classes:

```text
SubmissionController
SubmissionService
SubmissionRepository
SubmissionMapper
SubmissionCreatedEvent
```

---

## 16. Evaluation Module

Structure:

```text
evaluation/
├── worker
├── service
├── dto
├── verdict
└── strategy optional
```

Responsibilities:

```text
consume submission jobs
mark submission RUNNING
call Judge0
compare outputs
calculate score
update submission result
update leaderboard
publish notification event
```

Important classes:

```text
SubmissionEvaluationWorker
EvaluationService
Judge0EvaluationClient
VerdictCalculator
OutputComparator
ScoreCalculator
```

---

## 17. RabbitMQ Integration

Package:

```text
infrastructure/rabbitmq
```

Classes:

```text
RabbitMQConfig
SubmissionQueueProducer
SubmissionEvaluationConsumer
NotificationQueueProducer
DeadLetterQueueConfig
```

Queue names:

```text
submission.evaluation.queue
submission.result.queue
notification.queue
plagiarism.check.queue
dead.letter.queue
```

---

## 18. Redis Integration

Package:

```text
infrastructure/redis
```

Classes:

```text
RedisConfig
RateLimiterService
LeaderboardCacheService
SubmissionStatusCacheService
ContestStatsCacheService
```

Redis use cases:

```text
leaderboard
rate limiting
submission status cache
contest stats cache
JWT blacklist optional
```

---

## 19. Judge0 Integration

Package:

```text
infrastructure/judge0
```

Classes:

```text
Judge0Client
Judge0SubmissionRequest
Judge0SubmissionResponse
Judge0LanguageMapper
Judge0Config
```

Rules:

```text
Spring Boot must not run code directly.
All execution must go through Judge0 CE.
Handle Judge0 timeouts.
Handle Judge0 unavailable state.
Map Judge0 statuses to internal verdicts.
```

---

## 20. JPlag Integration

Package:

```text
infrastructure/jplag
```

Classes:

```text
JPlagRunner
JPlagReportParser
SimilarityResultMapper
```

Usage:

```text
run after contest ends
compare accepted/relevant submissions
store similarity flags
allow faculty/admin review
```

---

## 21. Global Exception Handling

Use:

```text
@RestControllerAdvice
```

Package:

```text
common/exception
```

Handle:

```text
validation errors
authentication errors
authorization errors
resource not found
business rule violations
rate limit errors
internal errors
```

Standard error format:

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request payload",
  "details": [],
  "timestamp": "2026-05-13T10:30:00Z"
}
```

---

## 22. Standard API Response

Package:

```text
common/response
```

Classes:

```text
ApiResponse<T>
PageResponse<T>
ApiErrorResponse
```

Use consistent API response formats across all controllers.

---

## 23. Transaction Management

Use `@Transactional` in service layer.

Examples:

```text
user registration
problem creation
contest creation
submission creation
submission result update
leaderboard snapshot update
audit log creation
```

Avoid transactions in controllers.

---

## 24. Audit Logging Integration

Audit logs should be created for major actions.

Package:

```text
audit
```

Classes:

```text
AuditLogService
AuditLogRepository
AuditLogEntity
AuditAction
```

Track:

```text
login
problem creation
contest creation
submission created
submission evaluated
plagiarism flagged
role changed
```

---

## 25. Backend Configuration Files

Recommended files:

```text
application.yml
application-dev.yml
application-test.yml
application-docker.yml
```

Use profiles:

```text
dev
test
docker
prod-like optional
```

---

## 26. Environment Variables

Use environment variables for secrets and external services.

Examples:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
RABBITMQ_HOST
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
JWT_SECRET
JUDGE0_BASE_URL
```

Never hardcode secrets.

---

## 27. Testing Structure

Test package:

```text
src/test/java/com/codejudgex
```

Recommended test structure:

```text
auth
problem
contest
submission
evaluation
leaderboard
integration
```

Test types:

```text
unit tests
service tests
repository tests
controller tests
integration tests
queue tests
Redis tests
RabbitMQ tests
```

---

## 28. Coding Standards

Rules:

```text
controllers should be thin
services should contain business logic
repositories should only access DB
DTOs should be used for APIs
entities should not be exposed
exceptions should be meaningful
enums should be used for statuses
all list APIs should be paginated
all important actions should be audited
```

---

## 29. Package Dependency Rules

Allowed dependencies:

```text
controller → service
service → repository
service → infrastructure
mapper → entity/dto
repository → entity
```

Avoid:

```text
controller → repository
controller → entity response
repository → service
module circular dependencies
business logic in mapper
```

---

## 30. Future Backend Evolution

Possible future split:

```text
api-service
evaluation-worker-service
notification-worker-service
plagiarism-worker-service
```

Do not split until:

```text
core modular monolith works
async flows are stable
deployment is repeatable
monitoring exists
```

---

## 31. Backend Success Criteria

Backend structure is successful if:

```text
modules are clearly separated
controllers are thin
services own business logic
repositories are clean
DTOs protect entities
auth is centralized
RabbitMQ integration is isolated
Judge0 integration is isolated
Redis usage is isolated
errors are standardized
audit logs are consistent
code is testable
```

---

## 32. Final Summary

The CodeJudgeX backend should be built as a clean modular Spring Boot application.

The most important backend rule is:

```text
Keep the core business logic clean and keep infrastructure integrations isolated.
```

This structure makes the project easier to build, test, debug, extend, and eventually scale.

