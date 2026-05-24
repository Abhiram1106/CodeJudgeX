# CodeJudgeX Remaining Documentation Pack

This document contains the remaining CodeJudgeX documentation files:

```text
rabbitmq-workflows.md
redis-usage.md
plagiarism-detection.md
audit-logging.md
observability.md
devops.md
ci-cd.md
testing.md
deployment-local.md
tradeoffs.md
roadmap.md
demo-script.md
```

---

# rabbitmq-workflows.md

## 1. Purpose

This document defines the RabbitMQ messaging architecture for CodeJudgeX.

RabbitMQ is used to decouple long-running and background tasks from HTTP request handling.

Primary use cases:

```text
submission evaluation
result processing
notification delivery
plagiarism/similarity checks
retry handling
dead-letter handling
```

---

## 2. Why RabbitMQ

RabbitMQ is chosen because CodeJudgeX needs job-based asynchronous processing.

Reasons:

```text
fits producer-consumer workflows
simple local setup with Docker
supports durable queues
supports manual acknowledgements
supports retries
supports dead-letter queues
less complex than Kafka for this use case
```

CodeJudgeX does not need high-volume event streaming initially. It needs reliable background job processing.

---

## 3. Messaging Architecture

```text
Spring Boot API
    ↓ publishes job
RabbitMQ Exchange
    ↓ routes message
RabbitMQ Queue
    ↓ consumed by
Worker Consumer
    ↓ processes job
PostgreSQL / Redis / Judge0 / JPlag
```

---

## 4. Core Queues

```text
submission.evaluation.queue
submission.result.queue
notification.queue
plagiarism.check.queue
retry.queue
dead.letter.queue
```

---

## 5. Core Exchanges

Recommended exchanges:

```text
codejudgex.exchange
codejudgex.retry.exchange
codejudgex.dlx.exchange
```

Exchange type:

```text
topic
```

Reason:

```text
Topic exchanges allow flexible routing keys.
```

---

## 6. Routing Keys

```text
submission.created
submission.evaluated
notification.requested
plagiarism.check.requested
job.retry
job.dead
```

---

## 7. Submission Evaluation Flow

```text
Student submits code
    ↓
Backend saves submission as QUEUED
    ↓
Backend publishes message with routing key submission.created
    ↓
RabbitMQ routes to submission.evaluation.queue
    ↓
Evaluation worker consumes message
    ↓
Worker evaluates using Judge0
    ↓
Worker updates database
    ↓
Worker updates Redis leaderboard
```

---

## 8. Submission Evaluation Message

Recommended payload:

```json
{
  "submissionId": "uuid",
  "contestId": "uuid",
  "problemId": "uuid",
  "studentId": "uuid",
  "attempt": 1,
  "createdAt": "2026-05-13T10:30:00Z"
}
```

Rule:

```text
Do not put full source code in the message.
Worker should fetch source code from PostgreSQL.
```

---

## 9. Notification Flow

```text
Submission evaluated
    ↓
Notification event published
    ↓
notification.queue receives message
    ↓
Notification worker creates in-app notification
    ↓
MailHog email sent optionally
```

Notification payload:

```json
{
  "userId": "uuid",
  "type": "SUBMISSION_EVALUATED",
  "title": "Submission Evaluated",
  "message": "Your submission has been evaluated.",
  "resourceId": "submission-uuid"
}
```

---

## 10. Plagiarism Check Flow

```text
Faculty/Admin triggers similarity check
    ↓
Backend publishes plagiarism.check.requested
    ↓
plagiarism.check.queue receives message
    ↓
Worker collects contest submissions
    ↓
JPlag runs similarity detection
    ↓
Results stored as plagiarism flags
```

---

## 11. Manual Acknowledgement

Consumers should use manual acknowledgement.

Reason:

```text
Message should be acknowledged only after job succeeds.
```

If worker crashes before acknowledgement:

```text
RabbitMQ can redeliver the message.
```

---

## 12. Retry Strategy

Retry only system-level failures.

Retry examples:

```text
Judge0 unavailable
transient database issue
network timeout
temporary RabbitMQ issue
```

Do not retry:

```text
wrong answer
compilation error
runtime error caused by user code
```

---

## 13. Dead-Letter Queue

If message processing fails after max retries:

```text
move message to dead.letter.queue
mark related job as INTERNAL_ERROR if needed
log failure
alert admin through metrics/logs
```

DLQ message should include:

```text
original payload
error reason
failed queue
failed timestamp
attempt count
```

---

## 14. Idempotency

RabbitMQ can redeliver messages. Consumers must be idempotent.

Rules:

```text
check current submission status before processing
ignore already evaluated submissions unless rejudge requested
avoid duplicate leaderboard updates
store evaluation attempts if needed
```

---

## 15. RabbitMQ Config Classes

Recommended backend package:

```text
infrastructure/rabbitmq
```

Classes:

```text
RabbitMQConfig
RabbitMQProperties
SubmissionQueueProducer
SubmissionEvaluationConsumer
NotificationProducer
PlagiarismQueueProducer
DeadLetterConfig
```

---

## 16. Monitoring RabbitMQ

Track:

```text
queue depth
consumer count
message publish rate
message consume rate
retry count
DLQ count
unacknowledged messages
```

RabbitMQ UI:

```text
http://localhost:15672
```

---

## 17. Success Criteria

RabbitMQ design is successful if:

```text
submission API returns quickly
evaluation happens asynchronously
failed jobs are retried safely
unrecoverable jobs move to DLQ
duplicate messages do not corrupt data
queue depth is observable
workers can scale independently
```

---

# redis-usage.md

## 1. Purpose

This document defines how Redis is used in CodeJudgeX.

Redis is used for fast, temporary, high-read data. PostgreSQL remains the durable source of truth.

---

## 2. Redis Responsibilities

Redis is used for:

```text
live leaderboards
rate limiting
submission status cache
contest statistics cache
dashboard cache
JWT blacklist optional
temporary OTP/password reset token optional
```

Redis should not be used for permanent critical data.

---

## 3. Core Rule

```text
Redis improves speed. PostgreSQL preserves truth.
```

If Redis data is lost, the system should be able to rebuild important state from PostgreSQL.

---

## 4. Leaderboard Design

Use Redis sorted sets.

Key:

```text
leaderboard:contest:{contestId}
```

Member:

```text
studentId
```

Score:

```text
totalScore
```

Example operations:

```text
ZADD leaderboard:contest:123 300 student-1
ZREVRANGE leaderboard:contest:123 0 9 WITHSCORES
ZRANK leaderboard:contest:123 student-1
```

---

## 5. Leaderboard Metadata

Redis sorted set stores score only.

Extra metadata should come from PostgreSQL:

```text
student name
solved count
last submission time
rank snapshot
```

Reason:

```text
Redis should remain simple and fast.
```

---

## 6. Submission Status Cache

Key:

```text
submission_status:{submissionId}
```

Value example:

```json
{
  "status": "RUNNING",
  "score": 0,
  "updatedAt": "2026-05-13T10:30:00Z"
}
```

TTL:

```text
1 hour after final status
```

Used for fast polling from frontend.

---

## 7. Rate Limiting

Use Redis counters.

Submission rate limit key:

```text
rate_limit:submission:user:{userId}:contest:{contestId}
```

Login rate limit keys:

```text
rate_limit:login:ip:{ipAddress}
rate_limit:login:email:{email}
```

Example policy:

```text
5 login failures per 10 minutes
10 submissions per minute per contest
```

---

## 8. Contest Stats Cache

Key:

```text
contest_stats:{contestId}
```

Cached values:

```text
total participants
total submissions
accepted submissions
failed submissions
average score
active users
```

TTL:

```text
30 seconds to 2 minutes
```

---

## 9. Dashboard Cache

Admin/faculty dashboard data can be cached.

Key examples:

```text
dashboard:admin:summary
dashboard:faculty:{facultyId}:summary
```

TTL:

```text
30 seconds
```

---

## 10. JWT Blacklist Optional

For logout or forced token invalidation:

```text
jwt_blacklist:{tokenId}
```

TTL:

```text
remaining token expiry time
```

---

## 11. Key Naming Convention

Use predictable names:

```text
feature:entity:id:subkey
```

Examples:

```text
leaderboard:contest:{contestId}
submission_status:{submissionId}
rate_limit:login:ip:{ipAddress}
contest_stats:{contestId}
```

---

## 12. Redis Failure Handling

If Redis is down:

```text
submission results should still save in PostgreSQL
leaderboard can be rebuilt later
rate limiting may temporarily fail open or fail closed based on security need
dashboard cache can be skipped
```

Critical writes must not depend only on Redis.

---

## 13. Redis Backend Classes

Recommended package:

```text
infrastructure/redis
```

Classes:

```text
RedisConfig
LeaderboardCacheService
RateLimiterService
SubmissionStatusCacheService
ContestStatsCacheService
JwtBlacklistService optional
```

---

## 14. Testing Redis

Test:

```text
leaderboard score update
rank fetching
rate limit increment
TTL expiration
submission status cache
fallback behavior when cache miss occurs
```

Use Testcontainers for Redis integration tests.

---

## 15. Success Criteria

Redis usage is successful if:

```text
leaderboards are fast
rate limiting works
submission polling is efficient
cache misses fall back to PostgreSQL
Redis loss does not destroy permanent data
key names are consistent
TTLs are applied properly
```

---

# plagiarism-detection.md

## 1. Purpose

This document defines the plagiarism and code similarity detection design for CodeJudgeX.

The system uses JPlag to detect suspicious code similarity after contests.

Important wording:

```text
This is similarity detection, not absolute plagiarism proof.
```

---

## 2. Why Similarity Detection Matters

Coding assessments can be manipulated through:

```text
copy-pasted code
minor variable renaming
shared solutions
same wrong logic
last-minute coordinated submissions
```

CodeJudgeX helps faculty/admins identify suspicious pairs for manual review.

---

## 3. Tool Choice

Use:

```text
JPlag
```

Why:

```text
open-source
runs locally
supports multiple languages
used for source-code similarity detection
fits zero-cost requirement
```

---

## 4. Workflow

```text
Contest ends
    ↓
Faculty/Admin triggers similarity check
    ↓
Backend publishes plagiarism check job
    ↓
Worker collects contest submissions
    ↓
Submissions are written to temporary files
    ↓
JPlag runs comparison
    ↓
Similarity report is parsed
    ↓
Suspicious pairs are stored
    ↓
Faculty/Admin reviews flags
```

---

## 5. When To Run

Run similarity checks:

```text
after contest ends
on faculty/admin trigger
optionally scheduled nightly
```

Do not run on every submission in MVP.

Reason:

```text
batch processing is simpler, faster, and more realistic for contests.
```

---

## 6. Data Used

Use submissions from:

```text
same contest
same problem
same language
```

Recommended filter:

```text
accepted submissions
partially accepted submissions
optionally all submissions
```

---

## 7. Plagiarism Job Payload

Queue:

```text
plagiarism.check.queue
```

Payload:

```json
{
  "contestId": "uuid",
  "requestedBy": "user-uuid",
  "createdAt": "2026-05-13T10:30:00Z"
}
```

---

## 8. Similarity Flag Table

Store:

```text
contest_id
submission_id
matched_submission_id
similarity_score
reason
status
reviewed_by
review_note
created_at
```

Statuses:

```text
OPEN
UNDER_REVIEW
CONFIRMED
DISMISSED
```

---

## 9. Similarity Thresholds

Suggested thresholds:

```text
70%+ → suspicious
85%+ → highly suspicious
95%+ → near duplicate
```

These are not final proof. They are review signals.

---

## 10. Admin Review Flow

```text
Admin opens plagiarism flags
    ↓
Views matched submissions
    ↓
Compares code side-by-side
    ↓
Reviews similarity score
    ↓
Marks flag as CONFIRMED or DISMISSED
    ↓
Audit log is created
```

---

## 11. Frontend Features

Admin/faculty page should show:

```text
contest
problem
student A
student B
similarity score
status
review action
```

Optional advanced UI:

```text
side-by-side code viewer
highlighted matching sections
filter by score
filter by status
```

---

## 12. Audit Events

Track:

```text
PLAGIARISM_CHECK_STARTED
PLAGIARISM_CHECK_COMPLETED
PLAGIARISM_FLAGGED
PLAGIARISM_FLAG_REVIEWED
```

---

## 13. Security Rules

Rules:

```text
students cannot view plagiarism flags
only faculty/admin can trigger checks
only authorized users can view source code
temporary files must be cleaned up
reports should not expose unnecessary private data
```

---

## 14. Failure Handling

Possible failures:

```text
JPlag execution fails
not enough submissions
unsupported language
temporary file write failure
parser failure
```

Handling:

```text
store job failure
log error
notify admin
move message to DLQ if repeated
```

---

## 15. Success Criteria

Similarity detection is successful if:

```text
faculty/admin can trigger a check
JPlag runs on contest submissions
similarity flags are stored
flags can be reviewed
students cannot access reports
review decisions are audited
```

---

# audit-logging.md

## 1. Purpose

This document defines the audit logging design for CodeJudgeX.

Audit logs provide accountability, traceability, and integrity for important system actions.

---

## 2. Why Audit Logging Matters

CodeJudgeX handles sensitive workflows:

```text
role changes
hidden test case edits
contest creation
code submissions
submission evaluation
plagiarism review
admin actions
```

Audit logs help answer:

```text
who did what
when it happened
which resource was affected
from which IP/device
what changed
```

---

## 3. Audit Table

Recommended table:

```text
audit_logs
```

Fields:

```text
id
actor_id
action
resource_type
resource_id
ip_address
user_agent
request_id
metadata
created_at
```

---

## 4. Audit Actions

Track:

```text
USER_REGISTERED
USER_LOGIN
USER_LOGIN_FAILED
USER_LOGOUT
ROLE_CHANGED
PROBLEM_CREATED
PROBLEM_UPDATED
TEST_CASE_CREATED
TEST_CASE_UPDATED
CONTEST_CREATED
CONTEST_STARTED
CONTEST_ENDED
SUBMISSION_CREATED
SUBMISSION_EVALUATION_STARTED
SUBMISSION_EVALUATED
SUBMISSION_EVALUATION_FAILED
LEADERBOARD_UPDATED
PLAGIARISM_CHECK_STARTED
PLAGIARISM_FLAGGED
PLAGIARISM_FLAG_REVIEWED
ADMIN_REJUDGED_SUBMISSION
```

---

## 5. Metadata Examples

Problem created:

```json
{
  "problemId": "uuid",
  "title": "Two Sum",
  "difficulty": "EASY"
}
```

Role changed:

```json
{
  "targetUserId": "uuid",
  "oldRole": "STUDENT",
  "newRole": "FACULTY"
}
```

Submission evaluated:

```json
{
  "submissionId": "uuid",
  "status": "ACCEPTED",
  "score": 100
}
```

---

## 6. Request ID

Every request should have a request ID.

Use it in:

```text
API response headers
logs
audit logs
error responses
```

Header:

```text
X-Request-Id
```

---

## 7. Audit Service

Recommended class:

```text
AuditLogService
```

Responsibilities:

```text
create audit event
attach actor
attach request metadata
store metadata JSON
avoid breaking main flow if non-critical audit fails
```

---

## 8. Security Rules

Rules:

```text
students cannot view audit logs
faculty can view limited contest-related audit logs later
admins can view audit logs
super admins can view all logs
audit logs should not expose secrets
```

Never store:

```text
passwords
JWT tokens
refresh tokens
hidden test case full data unless necessary
secrets
```

---

## 9. Audit Log API

Admin endpoint:

```http
GET /api/v1/admin/audit-logs
```

Filters:

```text
actorId
action
resourceType
resourceId
from
to
page
size
```

---

## 10. Retention

MVP:

```text
keep all audit logs
```

Future:

```text
archive old logs
export logs
compress metadata
partition by month
```

---

## 11. Success Criteria

Audit logging is successful if:

```text
important actions are traceable
audit logs include actor/resource/time
audit logs are searchable
audit logs do not expose secrets
admin can inspect audit history
audit events are created consistently
```

---

# observability.md

## 1. Purpose

This document defines observability for CodeJudgeX.

Observability helps understand whether the system is healthy, slow, failing, overloaded, or behaving unexpectedly.

---

## 2. Observability Stack

Use:

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
SLF4J
Logback
Loki optional
Promtail optional
```

---

## 3. What To Observe

Track:

```text
API health
submission volume
evaluation latency
Judge0 failures
RabbitMQ queue depth
Redis availability
PostgreSQL health
login failures
error rate
active contests
```

---

## 4. Actuator Endpoints

Expose locally:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

Avoid exposing sensitive actuator endpoints publicly.

---

## 5. Metrics

Core metrics:

```text
submissions_created_total
submissions_queued_total
submissions_running_total
submissions_accepted_total
submissions_failed_total
evaluation_duration_ms
judge0_request_duration_ms
judge0_error_total
rabbitmq_queue_depth
api_request_duration_ms
api_error_total
login_failure_total
active_contests_total
```

---

## 6. Grafana Dashboards

Create dashboards for:

```text
API health
submission evaluation
RabbitMQ queues
Judge0 performance
JVM memory
JVM threads
error rate
contest activity
```

---

## 7. Logging

Use structured logs.

Fields:

```text
requestId
userId
role
contestId
problemId
submissionId
action
status
errorCode
latencyMs
timestamp
```

---

## 8. Correlation IDs

Every request should have:

```text
requestId
```

Used across:

```text
API logs
audit logs
error responses
worker logs
```

---

## 9. Alerts Optional

Future alerts:

```text
RabbitMQ queue depth too high
Judge0 error rate high
API error rate high
DB unavailable
worker not consuming messages
```

---

## 10. Success Criteria

Observability is successful if:

```text
health endpoints work
Prometheus scrapes metrics
Grafana shows dashboards
logs include request IDs
submission failures are visible
queue backlog is visible
Judge0 issues are visible
```

---

# devops.md

## 1. Purpose

This document defines the DevOps setup for CodeJudgeX.

The project should run with zero paid infrastructure using Docker Compose and open-source tools.

---

## 2. DevOps Goals

```text
one-command local setup
reproducible infrastructure
containerized services
environment-based config
CI validation
local observability
zero-cost tooling
```

---

## 3. Core Tools

```text
Docker
Docker Compose
GitHub Actions
GitHub Container Registry optional
Nginx optional
Makefile optional
.env files
```

---

## 4. Docker Services

Required services:

```text
frontend
backend
postgres
redis
rabbitmq
judge0
prometheus
grafana
mailhog
```

Optional:

```text
nginx
keycloak
loki
promtail
```

---

## 5. Local URLs

```text
Frontend: http://localhost:5173
Backend: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui
RabbitMQ UI: http://localhost:15672
Prometheus: http://localhost:9090
Grafana: http://localhost:3000
MailHog: http://localhost:8025
Judge0: http://localhost:2358
```

---

## 6. Environment Files

Use:

```text
.env
.env.example
```

Commit:

```text
.env.example
```

Do not commit:

```text
.env with real secrets
```

---

## 7. Backend Dockerfile

Should:

```text
use Java 21 base image
build jar or copy built jar
set environment variables
expose 8080
run Spring Boot app
```

---

## 8. Frontend Dockerfile

Should:

```text
use Node image for build
install dependencies
build Vite app
serve using dev server for local or Nginx later
```

---

## 9. Docker Compose Requirements

Compose should:

```text
start all required services
create shared network
mount database volumes
configure health checks where possible
load env variables
restart services if needed
```

---

## 10. Makefile Optional

Helpful commands:

```text
make up
make down
make logs
make backend-test
make frontend-test
make clean
```

---

## 11. Success Criteria

DevOps setup is successful if:

```text
docker compose up --build starts the system
frontend connects to backend
backend connects to DB/Redis/RabbitMQ/Judge0
Swagger opens
RabbitMQ UI opens
Grafana opens
MailHog opens
```

---

# ci-cd.md

## 1. Purpose

This document defines the CI/CD pipeline for CodeJudgeX.

CI/CD ensures every code change is tested, built, and validated automatically.

---

## 2. Tool

Use:

```text
GitHub Actions
```

---

## 3. Pipelines

Recommended workflows:

```text
backend-ci.yml
frontend-ci.yml
docker-ci.yml
```

---

## 4. Backend CI

Steps:

```text
checkout code
setup Java 21
cache Maven dependencies
run unit tests
run integration tests
validate Flyway migrations
build jar
```

Command:

```bash
mvn clean test
```

---

## 5. Frontend CI

Steps:

```text
checkout code
setup Node
install dependencies
run lint
run type check
run build
```

Commands:

```bash
npm install
npm run lint
npm run build
```

---

## 6. Docker CI

Steps:

```text
build backend Docker image
build frontend Docker image
validate docker-compose config
optional push to GitHub Container Registry
```

---

## 7. Integration Tests

Use Testcontainers for:

```text
PostgreSQL
Redis
RabbitMQ
```

Run during backend CI if execution time is acceptable.

---

## 8. Security Scan Optional

Optional free tools:

```text
OWASP Dependency Check
Trivy
npm audit
```

---

## 9. Branch Rules

Recommended:

```text
main branch should stay stable
pull requests must pass CI
no direct pushes to main later
```

---

## 10. Success Criteria

CI/CD is successful if:

```text
backend tests run automatically
frontend build runs automatically
Docker images build successfully
broken code is caught before merge
pipeline is visible in GitHub Actions
```

---

# testing.md

## 1. Purpose

This document defines the testing strategy for CodeJudgeX.

Testing is critical because CodeJudgeX handles submissions, scoring, roles, hidden tests, async queues, and leaderboards.

---

## 2. Testing Stack

```text
JUnit 5
Mockito
AssertJ
Spring Boot Test
MockMvc
Testcontainers
React Testing Library
Playwright optional
```

---

## 3. Backend Unit Tests

Test:

```text
AuthService
JwtService
ProblemService
ContestService
SubmissionService
EvaluationService
LeaderboardService
RateLimiterService
```

Focus:

```text
business logic
validation
status transitions
score calculation
permission rules
```

---

## 4. Repository Tests

Use Testcontainers PostgreSQL.

Test:

```text
custom queries
indexes indirectly through query behavior
constraints
relationships
pagination
```

---

## 5. Controller Tests

Use MockMvc.

Test:

```text
auth APIs
problem APIs
contest APIs
submission APIs
admin APIs
error responses
role access
```

---

## 6. Integration Tests

Use Testcontainers for:

```text
PostgreSQL
Redis
RabbitMQ
```

Test:

```text
submission creation
message publishing
message consuming
Redis leaderboard update
rate limiting
```

---

## 7. Evaluation Tests

Test:

```text
output comparison
score calculation
status mapping
compilation error handling
runtime error handling
TLE handling
Judge0 failure handling
```

Judge0 can be mocked for most tests.

---

## 8. Security Tests

Test:

```text
student cannot create problem
student cannot view hidden tests
faculty cannot manage admins
invalid JWT rejected
expired JWT rejected
hidden fields not returned
```

---

## 9. Frontend Tests

Test:

```text
login form
protected routes
contest list
problem-solving page
submission result page
leaderboard page
role-based navigation
```

---

## 10. E2E Tests Optional

Use Playwright.

Flow:

```text
faculty creates problem
faculty creates contest
student joins contest
student submits code
result appears
leaderboard updates
```

---

## 11. Success Criteria

Testing is successful if:

```text
core business logic has unit tests
DB/Redis/RabbitMQ flows have integration tests
security rules are tested
submission evaluation is tested
CI runs tests automatically
```

---

# deployment-local.md

## 1. Purpose

This document explains how to run CodeJudgeX locally using Docker Compose.

The project is designed to be zero-cost and self-hosted.

---

## 2. Prerequisites

Install:

```text
Docker
Docker Compose
Git
Java 21 optional for local backend dev
Node.js 20+ optional for local frontend dev
```

---

## 3. Clone Repository

```bash
git clone <repo-url>
cd codejudgex
```

---

## 4. Environment Setup

Copy env file:

```bash
cp .env.example .env
```

Update values if required.

---

## 5. Start Full System

```bash
docker compose up --build
```

---

## 6. Local URLs

```text
Frontend: http://localhost:5173
Backend API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui
RabbitMQ UI: http://localhost:15672
Prometheus: http://localhost:9090
Grafana: http://localhost:3000
MailHog: http://localhost:8025
Judge0: http://localhost:2358
```

---

## 7. Default Credentials

RabbitMQ local default example:

```text
username: codejudgex
password: codejudgex
```

Grafana local example:

```text
username: admin
password: admin
```

Use `.env.example` as source of truth.

---

## 8. Stop System

```bash
docker compose down
```

Remove volumes if needed:

```bash
docker compose down -v
```

---

## 9. Troubleshooting

Common issues:

```text
port already in use
Docker not running
Judge0 not healthy yet
backend starts before DB ready
RabbitMQ credentials mismatch
```

Check logs:

```bash
docker compose logs backend
```

---

## 10. Success Criteria

Local deployment works if:

```text
frontend opens
backend health endpoint works
Swagger opens
PostgreSQL connects
Redis connects
RabbitMQ UI opens
Judge0 responds
Prometheus scrapes metrics
Grafana opens
```

---

# tradeoffs.md

## 1. Purpose

This document records major engineering tradeoffs in CodeJudgeX.

Tradeoffs show that decisions were made intentionally, not randomly.

---

## 2. Modular Monolith vs Microservices

Chosen:

```text
Modular monolith
```

Why:

```text
easier to build
easier to debug
easier to test
easier to deploy
lower operational complexity
```

Microservices may be added later only after the monolith is stable.

---

## 3. RabbitMQ vs Kafka

Chosen:

```text
RabbitMQ
```

Why:

```text
submission evaluation is job-based
RabbitMQ fits task queues better
simpler local setup
supports retries and DLQ
```

Kafka is better for high-volume event streaming, which is not the first need.

---

## 4. Judge0 CE vs Custom Sandbox

Chosen:

```text
Judge0 CE
```

Why:

```text
safer than direct execution
self-hostable
multi-language support
built for online judges
```

Custom sandboxing is too risky and time-consuming initially.

---

## 5. Redis + PostgreSQL vs PostgreSQL Only

Chosen:

```text
Redis + PostgreSQL
```

Why:

```text
Redis gives fast leaderboard reads
PostgreSQL remains source of truth
```

---

## 6. Spring Security JWT vs Keycloak First

Chosen:

```text
Spring Security JWT first
```

Why:

```text
better learning value
less setup complexity
more direct backend control
```

Keycloak can be added later.

---

## 7. Local Docker Compose vs Cloud

Chosen:

```text
Docker Compose local-first
```

Why:

```text
zero cost
fully reproducible
no dependency on paid platforms
easy for reviewers to run
```

---

## 8. Polling vs WebSockets

Chosen initially:

```text
polling submission status
```

Why:

```text
simpler
reliable enough for MVP
easier to debug
```

WebSockets/SSE can be added later.

---

## 9. Exact Output Matching vs Custom Checker

Chosen initially:

```text
normalized exact output matching
```

Why:

```text
simple
clear
works for most beginner/intermediate problems
```

Custom checkers can be added later.

---

## 10. Success Criteria

Tradeoffs are successful if:

```text
each major tool has a clear reason
complexity is controlled
system remains buildable
future growth is possible
```

---

# roadmap.md

## 1. Purpose

This document defines the roadmap for CodeJudgeX.

The project should be built in phases, not all at once.

---

## 2. Phase 1: Core Online Judge

Goal:

```text
Make code submission and evaluation work end-to-end.
```

Features:

```text
Spring Boot backend
React frontend
JWT auth
student/faculty/admin roles
problem creation
test case creation
contest creation
student joins contest
code submission
RabbitMQ queue
Judge0 evaluation
submission result
basic leaderboard
Docker Compose
Swagger
```

---

## 3. Phase 2: Strong Platform Version

Goal:

```text
Make the platform feel real and usable.
```

Features:

```text
hidden test cases
weighted scoring
Redis leaderboard
submission history
faculty dashboard
admin dashboard
audit logs
MailHog notifications
rate limiting
better error handling
```

---

## 4. Phase 3: Production-Inspired Version

Goal:

```text
Add serious engineering practices.
```

Features:

```text
Testcontainers
Prometheus
Grafana
structured logging
correlation IDs
GitHub Actions CI
RabbitMQ retry queue
Dead-letter queue
JPlag similarity checks
security hardening
```

---

## 5. Phase 4: Advanced Version

Goal:

```text
Make it a serious full platform.
```

Features:

```text
Python support
C++ support
rejudge submissions
leaderboard freeze
problem tags
editorials
team contests
classroom analytics
CSV export
Keycloak integration
```

---

## 6. Phase 5: Massive Version

Goal:

```text
Enterprise-style expansion.
```

Features:

```text
multi-tenant organizations
advanced permissions
custom checkers
SSE/WebSocket live updates
advanced plagiarism reports
Kubernetes optional
centralized logs with Loki
advanced dashboards
problem recommendation engine optional
```

---

## 7. What Not To Build Early

Avoid early:

```text
microservices
Kubernetes
too many languages
advanced AI features
perfect UI polish
complex proctoring
payment/billing
```

---

## 8. First Critical Milestone

The first true milestone:

```text
Student submits Java code
    ↓
RabbitMQ queues job
    ↓
Worker calls Judge0
    ↓
Result saved
    ↓
Leaderboard updates
```

If this works, the project has a real foundation.

---

# demo-script.md

## 1. Purpose

This document provides a demo script for presenting CodeJudgeX.

Use this during interviews, portfolio walkthroughs, or GitHub demo videos.

---

## 2. Demo Goal

Show the complete core workflow:

```text
faculty creates contest
faculty creates problem
student submits code
system evaluates asynchronously
leaderboard updates
admin/faculty reviews results
```

---

## 3. Start System

Run:

```bash
docker compose up --build
```

Open:

```text
Frontend: http://localhost:5173
Swagger: http://localhost:8080/swagger-ui
RabbitMQ UI: http://localhost:15672
Grafana: http://localhost:3000
MailHog: http://localhost:8025
```

---

## 4. Faculty Flow

Login as faculty.

Steps:

```text
open faculty dashboard
create a problem
add sample test case
add hidden test cases
create a contest
add problem to contest
start or schedule contest
```

Explain:

```text
Hidden test cases are protected from students.
```

---

## 5. Student Flow

Login as student.

Steps:

```text
open contests page
join contest
open problem
write code in Monaco editor
submit solution
see submission queued
open submission result page
watch status change from QUEUED to RUNNING to final verdict
```

Explain:

```text
The API does not evaluate code directly. It queues the submission through RabbitMQ.
```

---

## 6. Backend Flow Explanation

Say:

```text
When the student submits code, Spring Boot saves the submission as QUEUED and publishes a job to RabbitMQ. A worker consumes the job, sends code to Judge0 CE, evaluates hidden test cases, stores the result, and updates Redis leaderboard.
```

---

## 7. Leaderboard Demo

Open leaderboard page.

Show:

```text
rank
student name
score
solved count
last submission time
```

Explain:

```text
Redis powers live leaderboard reads, while PostgreSQL stores persistent results.
```

---

## 8. Admin/Faculty Review

Show:

```text
submission list
submission details
contest analytics
audit logs
```

Optional:

```text
trigger JPlag similarity check after contest ends
show plagiarism flags
```

---

## 9. Observability Demo

Open Grafana.

Show:

```text
API metrics
submission count
evaluation latency
RabbitMQ queue depth
error rate
```

Explain:

```text
The system exposes metrics through Actuator and Prometheus.
```

---

## 10. RabbitMQ Demo

Open RabbitMQ UI.

Show:

```text
submission.evaluation.queue
message rates
consumers
DLQ optional
```

Explain:

```text
RabbitMQ decouples user requests from long-running code execution.
```

---

## 11. Final Demo Summary

End with:

```text
CodeJudgeX is a zero-cost, self-hosted coding assessment platform built with Spring Boot, React, PostgreSQL, Redis, RabbitMQ, Judge0 CE, JPlag, Docker, Prometheus, and Grafana.
```

Then highlight:

```text
async processing
secure code execution
hidden test cases
Redis leaderboard
audit logs
observability
CI/CD
```

