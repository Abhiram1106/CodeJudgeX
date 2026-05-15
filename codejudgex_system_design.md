# CodeJudgeX System Design

## 1. Purpose

This document explains the system design behind **CodeJudgeX**.

It focuses on major engineering decisions, system behavior, scalability, consistency, reliability, failure handling, and tradeoffs.

CodeJudgeX is not designed as a simple CRUD application. It is designed as a production-inspired coding assessment platform with asynchronous evaluation, secure code execution, caching, auditability, and observability.

---

## 2. System Design Goals

CodeJudgeX is designed to satisfy the following goals:

```text
Fast API responses
Asynchronous code evaluation
Secure handling of submitted code
Reliable submission processing
Accurate scoring
Fast leaderboard reads
Hidden test case protection
Role-based access control
Clear audit trail
Local-first zero-cost deployment
Observability through metrics and logs
Failure recovery through retries and DLQs
```

---

## 3. Core Design Principle

The core design principle is:

```text
Never evaluate submitted code inside the API request lifecycle.
```

Instead:

```text
API receives submission
    ↓
Submission is saved as QUEUED
    ↓
Job is published to RabbitMQ
    ↓
Worker evaluates submission asynchronously
```

This keeps the API fast, resilient, and scalable.

---

## 4. High-Level System Design

```text
Student / Faculty / Admin
        ↓
React Frontend
        ↓
Spring Boot REST API
        ↓
PostgreSQL + Redis
        ↓
RabbitMQ
        ↓
Evaluation Worker
        ↓
Judge0 CE
        ↓
PostgreSQL + Redis Leaderboard
        ↓
Prometheus + Grafana
```

---

## 5. Main Subsystems

## 5.1 Identity and Access Subsystem

Responsible for:

```text
Authentication
Authorization
JWT handling
Refresh tokens
Password hashing
Role-based access
Permission checks
```

Main roles:

```text
STUDENT
FACULTY
ADMIN
SUPER_ADMIN
```

---

## 5.2 Contest Management Subsystem

Responsible for:

```text
Contest creation
Contest scheduling
Contest status lifecycle
Contest registration
Contest-problem mapping
Contest analytics
```

Contest lifecycle:

```text
DRAFT → UPCOMING → LIVE → ENDED → ARCHIVED
```

---

## 5.3 Problem and Test Case Subsystem

Responsible for:

```text
Problem statements
Difficulty levels
Tags
Input/output format
Constraints
Sample test cases
Hidden test cases
Weighted scoring
```

Important rule:

```text
Students can access only sample test cases.
Hidden test cases are backend-only.
```

---

## 5.4 Submission Subsystem

Responsible for:

```text
Accepting code submissions
Validating contest eligibility
Storing submission metadata
Publishing evaluation jobs
Tracking submission status
Showing submission history
```

Submission status flow:

```text
QUEUED → RUNNING → FINAL_STATUS
```

Final statuses:

```text
ACCEPTED
WRONG_ANSWER
PARTIALLY_ACCEPTED
COMPILATION_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
INTERNAL_ERROR
```

---

## 5.5 Evaluation Subsystem

Responsible for:

```text
Consuming RabbitMQ jobs
Fetching submissions and test cases
Calling Judge0 CE
Processing execution results
Comparing actual and expected output
Calculating score
Updating submission status
Updating leaderboard
```

---

## 5.6 Leaderboard Subsystem

Responsible for:

```text
Fast ranking
Score aggregation
Solved count tracking
Last submission time tracking
Leaderboard snapshots
```

Design:

```text
Redis = fast live leaderboard
PostgreSQL = permanent source of truth
```

---

## 5.7 Plagiarism / Similarity Subsystem

Responsible for:

```text
Post-contest similarity checks
JPlag integration
Suspicious pair detection
Similarity reports
Faculty/admin review
```

Important terminology:

```text
Use “similarity detection” instead of claiming absolute plagiarism detection.
```

---

## 5.8 Observability Subsystem

Responsible for:

```text
Metrics
Health checks
Structured logs
Correlation IDs
Grafana dashboards
Queue monitoring
Error visibility
```

---

## 6. Key System Design Decisions

## 6.1 Modular Monolith First

Chosen approach:

```text
Modular monolith + async workers
```

Why:

```text
Simpler deployment
Faster development
Easier debugging
Clean module boundaries
Lower operational overhead
Can be split later if needed
```

Rejected approach:

```text
Microservices from day one
```

Reason rejected:

```text
Too much unnecessary distributed-system complexity early.
```

---

## 6.2 RabbitMQ for Async Evaluation

Chosen tool:

```text
RabbitMQ
```

Why:

```text
Submission evaluation is job-based
Supports queues naturally
Supports acknowledgements
Supports retries
Supports dead-letter queues
Simpler than Kafka for this use case
```

---

## 6.3 Judge0 CE for Code Execution

Chosen tool:

```text
Judge0 CE self-hosted
```

Why:

```text
Avoids unsafe direct code execution
Supports multiple languages
Self-hostable
Docker-friendly
Designed for online judges
```

---

## 6.4 Redis for Leaderboard and Fast State

Chosen tool:

```text
Redis
```

Use cases:

```text
Leaderboard
Rate limiting
Submission status cache
Contest stats cache
JWT blacklist optional
```

Important rule:

```text
Redis is not the source of truth.
PostgreSQL is the source of truth.
```

---

## 6.5 PostgreSQL as Source of Truth

Chosen tool:

```text
PostgreSQL
```

Why:

```text
Strong relational consistency
Good indexing
Reliable transactions
Excellent for contest/problem/submission data
Supports JSONB if needed later
```

---

## 7. Core Workflow: Code Submission

```text
1. Student submits code.
2. Backend validates JWT.
3. Backend checks contest status.
4. Backend checks student participation.
5. Backend validates language and payload size.
6. Backend saves submission as QUEUED.
7. Backend publishes evaluation job to RabbitMQ.
8. API returns submission ID immediately.
9. Worker consumes job.
10. Worker marks submission as RUNNING.
11. Worker sends code to Judge0 CE.
12. Judge0 executes code against test cases.
13. Worker processes result.
14. Worker updates PostgreSQL.
15. Worker updates Redis leaderboard.
16. Student sees final verdict.
```

---

## 8. Data Consistency Model

CodeJudgeX uses both strong consistency and eventual consistency.

## 8.1 Strong Consistency

Used for:

```text
User creation
Role assignment
Problem creation
Test case creation
Contest creation
Submission record creation
Audit log creation
```

These operations should complete inside database transactions.

---

## 8.2 Eventual Consistency

Used for:

```text
Submission evaluation result
Leaderboard update
Notifications
Plagiarism checks
Analytics cache
```

Example:

```text
A submission is immediately created as QUEUED, but the final verdict appears later.
```

---

## 9. Idempotency Design

RabbitMQ may deliver the same message more than once.

Therefore, consumers must be idempotent.

### Idempotency Rules

```text
Do not evaluate completed submissions again unless rejudge is requested.
Check current submission status before processing.
Use unique submission IDs.
Use status transition validation.
Avoid duplicate leaderboard updates.
Store evaluation attempt metadata.
```

Example:

```text
If submission status is ACCEPTED and duplicate message arrives, worker should ignore it.
```

---

## 10. Rate Limiting Design

Redis should be used for rate limiting.

Examples:

```text
Limit login attempts per IP/email
Limit submissions per student per contest
Limit plagiarism check triggers
Limit admin export requests
```

Example key:

```text
rate_limit:submission:user:{userId}:contest:{contestId}
```

---

## 11. Leaderboard Design

### 11.1 Redis Live Leaderboard

Use Redis sorted sets.

Example key:

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

### 11.2 PostgreSQL Leaderboard Snapshot

PostgreSQL stores persistent leaderboard entries.

Used for:

```text
Historical reports
Final contest result
Data recovery
Auditability
```

---

## 12. Failure Handling

## 12.1 Submission Evaluation Failure

Possible causes:

```text
Judge0 unavailable
Worker crash
RabbitMQ connection failure
Invalid test case
Database timeout
Unexpected runtime error
```

Handling:

```text
Retry limited times
Move to dead-letter queue
Mark submission as INTERNAL_ERROR if unrecoverable
Log error with correlation ID
Notify admin if repeated failures occur
```

---

## 12.2 RabbitMQ Failure

Handling:

```text
Durable queues
Persistent messages
Manual acknowledgement
Retry queue
Dead-letter queue
Health monitoring
```

---

## 12.3 Redis Failure

Handling:

```text
Fallback to PostgreSQL for critical reads
Rebuild leaderboard from PostgreSQL
Avoid storing permanent state only in Redis
Log Redis failures
Expose health checks
```

---

## 12.4 PostgreSQL Failure

Handling:

```text
Fail fast for write operations
Expose degraded health status
Do not acknowledge queue messages if DB update fails
Retry message processing
```

---

## 13. Security System Design

Security controls:

```text
JWT authentication
Refresh token rotation
BCrypt password hashing
Role-based authorization
Permission checks
CORS restrictions
Input validation
Request size limits
Rate limiting
Audit logs
Hidden test case access control
Judge0 sandboxed execution
Environment-based secrets
```

---

## 14. CIA Mapping

## 14.1 Confidentiality

Protects:

```text
Passwords
JWT secrets
Hidden test cases
Student submissions
Admin APIs
Contest data before start time
```

Controls:

```text
BCrypt
RBAC
Permission checks
Sensitive data masking
Environment variables
```

---

## 14.2 Integrity

Protects:

```text
Scores
Submission results
Test cases
Contest rules
User roles
Audit logs
```

Controls:

```text
Database constraints
Transactions
Submission immutability
Audit logging
Idempotent consumers
Source code hashing
```

---

## 14.3 Availability

Protects:

```text
Submission flow
Contest pages
Leaderboard
Evaluation worker
Admin monitoring
```

Controls:

```text
Async queues
Retry mechanism
DLQ
Redis cache
Health checks
Graceful degradation
Monitoring
```

---

## 15. Observability Design

Metrics to track:

```text
total_submissions
queued_submissions
running_submissions
accepted_submissions
failed_submissions
average_evaluation_time
rabbitmq_queue_depth
api_latency
api_error_rate
login_failures
active_contests
judge0_error_count
```

Logs should include:

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
```

---

## 16. Scalability Design

## 16.1 API Scaling

The API can scale horizontally because evaluation is offloaded to RabbitMQ.

## 16.2 Worker Scaling

Multiple workers can consume submission jobs in parallel.

## 16.3 Database Scaling

Use:

```text
Indexes
Pagination
Query optimization
Archiving old submissions later
Read-optimized leaderboard snapshots
```

## 16.4 Redis Scaling

Redis improves fast reads for leaderboard and stats.

## 16.5 Judge0 Scaling

Judge0 execution capacity can be scaled by increasing execution workers/containers later.

---

## 17. Performance Considerations

Important optimizations:

```text
Do not load all submissions without pagination
Index contest_id, student_id, problem_id
Cache leaderboard in Redis
Avoid synchronous Judge0 calls in API request
Use batch queries for dashboard stats
Compress large responses if needed
Limit code submission size
Limit test case output size
```

---

## 18. Major Tradeoffs

## 18.1 Simplicity vs Scalability

Chosen:

```text
Modular monolith first
```

Reason:

```text
Better for development speed and maintainability.
```

## 18.2 RabbitMQ vs Kafka

Chosen:

```text
RabbitMQ
```

Reason:

```text
Better fit for job processing.
```

## 18.3 Redis vs Database Leaderboard

Chosen:

```text
Redis + PostgreSQL
```

Reason:

```text
Redis gives speed. PostgreSQL gives persistence.
```

## 18.4 Judge0 vs Custom Execution

Chosen:

```text
Judge0 CE
```

Reason:

```text
Safer and more practical than building a custom sandbox initially.
```

---

## 19. System Limits

Initial system limits:

```text
Limited language support initially
Local-first deployment
Educational sandboxing, not commercial-grade hostile-code isolation
Moderate contest scale
Manual scaling through Docker Compose
```

These limits are acceptable for the first major version.

---

## 20. Future System Design Improvements

Future improvements:

```text
Separate evaluation worker service
Keycloak integration
Multi-tenant organizations
Kubernetes deployment
Advanced Judge0 scaling
Leaderboard freeze
Rejudge architecture
Event sourcing for audit-heavy flows
Centralized logging with Loki
Advanced role permissions
Read replicas later
```

---

## 21. Final System Design Summary

CodeJudgeX is designed as a production-inspired, zero-cost coding assessment platform.

Its strongest system design elements are:

```text
Async submission evaluation
RabbitMQ job queues
Judge0-based code execution
Redis leaderboard
PostgreSQL source of truth
Role-based security
Audit logging
Observability
Failure recovery
Dockerized local infrastructure
```

This gives the project serious engineering depth while keeping it realistic to build incrementally.

