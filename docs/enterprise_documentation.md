# CodeJudgeX — Enterprise-Level Project Documentation

## 1. Project Overview

**CodeJudgeX** is a zero-cost, open-source, full-stack coding assessment and online judge platform designed for colleges, training institutes, coding clubs, and placement preparation ecosystems.

The platform allows faculty/admins to create coding contests, manage problems and hidden test cases, evaluate student submissions asynchronously, generate leaderboards, detect code similarity, monitor system health, and maintain complete accountability through audit logs.

The goal is to build a production-inspired platform that demonstrates backend engineering, frontend development, system design, security, DevOps, database design, observability, and real-world software architecture using only free and open-source tools.

---

## 2. Product Vision

### Vision Statement

To create a reliable, extensible, and self-hostable coding assessment platform that helps institutions conduct programming contests and technical assessments without depending on paid platforms.

### Core Philosophy

CodeJudgeX is built around five principles:

1. **Zero-cost infrastructure** — every tool should be free, open-source, or self-hostable.
2. **Production-inspired architecture** — the system should be designed like a real engineering platform, not a tutorial project.
3. **Security-first execution** — submitted code must be isolated and controlled.
4. **Async-first evaluation** — submission evaluation should not block API requests.
5. **Accountability by design** — important actions must be traceable through audit logs.

---

## 3. Problem Statement

Many colleges and training institutes conduct coding assessments using fragmented tools such as Google Forms, spreadsheets, WhatsApp groups, manual evaluation, or paid coding platforms.

These approaches suffer from:

- Lack of automated code evaluation
- No hidden test case support
- No reliable contest leaderboard
- No plagiarism/similarity detection
- No structured faculty/admin workflow
- No auditability
- No local/self-hosted ownership
- No system observability
- Limited customization

CodeJudgeX solves this by providing a complete self-hosted coding assessment platform.

---

## 4. Target Users

### 4.1 Student

Students use the platform to:

- Register/login
- Join contests
- View coding problems
- Write code in the browser
- Submit solutions
- View submission results
- Track leaderboard rank
- Review submission history

### 4.2 Faculty

Faculty users can:

- Create contests
- Create problems
- Add sample and hidden test cases
- View student submissions
- View contest analytics
- Trigger plagiarism checks
- Export reports

### 4.3 Admin

Admins can:

- Manage users
- Manage roles
- Monitor platform usage
- View audit logs
- Review flagged plagiarism cases
- Manage system-level settings

### 4.4 Super Admin

Super admins can:

- Manage global configuration
- Manage admin/faculty access
- Review platform-wide logs
- Configure advanced integrations

---

## 5. Final Zero-Cost Technology Stack

### 5.1 Backend Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- Lombok
- MapStruct
- Jakarta Validation

### 5.2 Database Stack

- PostgreSQL
- Flyway
- Hibernate/JPA
- pgAdmin optional

### 5.3 Cache and Fast Data Layer

- Redis
- Spring Data Redis

### 5.4 Messaging and Async Processing

- RabbitMQ
- Spring AMQP

### 5.5 Code Execution

- Judge0 CE self-hosted
- Docker

### 5.6 Plagiarism and Similarity Detection

- JPlag

### 5.7 Authentication and Authorization

Phase 1:

- Spring Security
- JWT
- Refresh tokens
- BCrypt
- Role-based access control
- Permission-based authorization

Phase 2:

- Keycloak self-hosted
- OAuth2
- OpenID Connect
- SSO

### 5.8 Frontend Stack

- React
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui
- React Router
- TanStack Query
- Axios
- React Hook Form
- Zod
- Recharts
- Monaco Editor

### 5.9 DevOps Stack

- Docker
- Docker Compose
- Nginx
- GitHub Actions
- GitHub Container Registry
- Makefile optional
- Environment variables

### 5.10 Testing Stack

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- MockMvc
- Testcontainers
- React Testing Library
- Playwright optional

### 5.11 Observability Stack

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Loki optional
- Promtail optional

### 5.12 API Documentation Stack

- springdoc-openapi
- Swagger UI
- Bruno
- Postman free optional

### 5.13 Notification Stack

- MailHog
- RabbitMQ notification queue
- In-app notifications

---

## 6. High-Level Architecture

```text
React + Monaco + shadcn/ui
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
Result Processor
        ↓
Redis Leaderboard + PostgreSQL Results
        ↓
JPlag Similarity Detection
        ↓
Prometheus + Grafana Monitoring
```

---

## 7. Architectural Style

### 7.1 Recommended Architecture

CodeJudgeX should start as a **modular monolith with asynchronous workers**.

This avoids unnecessary microservice complexity while still allowing strong separation of concerns.

### 7.2 Why Not Microservices Initially?

Microservices add operational complexity:

- Service discovery
- Distributed transactions
- Multiple deployments
- Network failures
- More CI/CD complexity
- More monitoring requirements

For CodeJudgeX, the better first approach is:

```text
One Spring Boot backend
One React frontend
One async evaluation worker module
Shared PostgreSQL
Shared Redis
RabbitMQ for async jobs
Judge0 CE for code execution
```

Later, the platform can evolve into separate services if needed.

---

## 8. Core Backend Modules

```text
com.codejudgex
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

### 8.1 Auth Module

Responsible for:

- Registration
- Login
- JWT generation
- Refresh token handling
- Password hashing
- Logout
- Token blacklist optional

### 8.2 User Module

Responsible for:

- User profile
- Department/year details
- Role assignment
- User status

### 8.3 Problem Module

Responsible for:

- Problem creation
- Problem update
- Problem listing
- Difficulty tags
- Problem statements
- Constraints

### 8.4 Test Case Module

Responsible for:

- Sample test cases
- Hidden test cases
- Weighted test cases
- Test case visibility

### 8.5 Contest Module

Responsible for:

- Contest creation
- Contest scheduling
- Contest registration
- Contest-problem mapping
- Contest status lifecycle

### 8.6 Submission Module

Responsible for:

- Accepting code submissions
- Saving submission metadata
- Publishing evaluation jobs
- Tracking submission status

### 8.7 Evaluation Module

Responsible for:

- Consuming submission jobs
- Calling Judge0 CE
- Comparing outputs
- Calculating score
- Updating submission results

### 8.8 Leaderboard Module

Responsible for:

- Redis leaderboard updates
- PostgreSQL leaderboard snapshots
- Rank calculation
- Contest statistics

### 8.9 Plagiarism Module

Responsible for:

- Triggering JPlag checks
- Storing similarity results
- Flagging suspicious submissions
- Admin review workflow

### 8.10 Notification Module

Responsible for:

- In-app notifications
- MailHog email testing
- Notification queue handling

### 8.11 Audit Module

Responsible for:

- Tracking major user/admin actions
- Login history
- Submission history
- Problem/test case modification history

---

## 9. System Design Concepts Covered

CodeJudgeX is designed to demonstrate:

- Modular monolith architecture
- Async processing
- Worker architecture
- Producer-consumer pattern
- Message queues
- Retry mechanism
- Dead-letter queues
- Caching
- Rate limiting
- Leaderboard design
- Sandboxed code execution
- Eventual consistency
- Idempotency
- Database indexing
- Fault tolerance
- Auditability
- Observability
- Secure API design
- Role-based access control
- Horizontal scaling readiness

---

## 10. Core Domain Entities

### 10.1 User

Represents a platform user.

Fields:

```text
id
name
email
password_hash
role
department
year
status
created_at
updated_at
```

### 10.2 Role

Represents access level.

Roles:

```text
STUDENT
FACULTY
ADMIN
SUPER_ADMIN
```

### 10.3 Problem

Represents a coding problem.

Fields:

```text
id
title
description
difficulty
input_format
output_format
constraints_text
time_limit_ms
memory_limit_mb
created_by
created_at
updated_at
```

### 10.4 Test Case

Represents sample or hidden input/output.

Fields:

```text
id
problem_id
input_data
expected_output
is_sample
weight
created_at
```

### 10.5 Contest

Represents a coding contest or assessment.

Fields:

```text
id
title
description
start_time
end_time
status
created_by
created_at
updated_at
```

### 10.6 Submission

Represents a student's code submission.

Fields:

```text
id
student_id
contest_id
problem_id
language
source_code
source_code_hash
status
score
execution_time_ms
memory_used_mb
submitted_at
evaluated_at
```

### 10.7 Submission Result

Represents per-test-case result.

Fields:

```text
id
submission_id
test_case_id
status
actual_output
expected_output
execution_time_ms
error_message
```

### 10.8 Leaderboard Entry

Represents contest ranking.

Fields:

```text
id
contest_id
student_id
total_score
solved_count
last_submission_at
rank_snapshot
```

### 10.9 Plagiarism Flag

Represents suspicious similarity.

Fields:

```text
id
contest_id
submission_id
matched_submission_id
similarity_score
reason
status
created_at
```

### 10.10 Audit Log

Represents accountability record.

Fields:

```text
id
actor_id
action
resource_type
resource_id
ip_address
user_agent
metadata
created_at
```

---

## 11. Submission Evaluation Workflow

```text
Student submits code
    ↓
Spring Boot API validates request
    ↓
Submission saved as QUEUED
    ↓
Evaluation job published to RabbitMQ
    ↓
Evaluation worker consumes job
    ↓
Submission status changed to RUNNING
    ↓
Worker sends source code and test cases to Judge0 CE
    ↓
Judge0 executes code in isolated environment
    ↓
Worker compares actual output with expected output
    ↓
Score is calculated
    ↓
Submission status updated
    ↓
Leaderboard updated in Redis
    ↓
Final result persisted in PostgreSQL
```

---

## 12. Submission Status Model

```text
QUEUED
RUNNING
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

## 13. RabbitMQ Design

### 13.1 Queues

```text
submission.evaluation.queue
submission.result.queue
plagiarism.check.queue
notification.queue
dead.letter.queue
retry.queue
```

### 13.2 Exchange Strategy

Use topic exchanges for flexible routing.

Example routing keys:

```text
submission.created
submission.evaluated
plagiarism.requested
notification.requested
```

### 13.3 Failure Handling

If evaluation fails:

1. Retry limited number of times.
2. If still failing, move message to DLQ.
3. Mark submission as INTERNAL_ERROR.
4. Store error reason.
5. Notify admin if repeated failures occur.

---

## 14. Redis Design

Use Redis for fast, temporary, high-read data.

### 14.1 Leaderboard

```text
leaderboard:contest:{contestId}
```

Use Redis sorted set:

```text
studentId -> score
```

### 14.2 Rate Limiting

```text
rate_limit:submission:user:{userId}:contest:{contestId}
```

### 14.3 Submission Status Cache

```text
submission_status:{submissionId}
```

### 14.4 Contest Stats Cache

```text
contest_stats:{contestId}
```

### 14.5 JWT Blacklist Optional

```text
jwt_blacklist:{tokenId}
```

---

## 15. API Design Standards

### 15.1 Base API Path

```text
/api/v1
```

### 15.2 Standard Response Format

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-05-13T10:30:00Z"
}
```

### 15.3 Standard Error Response

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request payload",
  "details": [],
  "timestamp": "2026-05-13T10:30:00Z"
}
```

### 15.4 API Categories

```text
/api/v1/auth
/api/v1/users
/api/v1/problems
/api/v1/test-cases
/api/v1/contests
/api/v1/submissions
/api/v1/leaderboards
/api/v1/plagiarism
/api/v1/notifications
/api/v1/admin
/api/v1/audit-logs
```

### 15.5 API Documentation

Use:

```text
springdoc-openapi
Swagger UI
Bruno collections
```

---

## 16. Authentication and Authorization Design

### 16.1 Phase 1 Auth

Use Spring Security with JWT.

Features:

- Register
- Login
- Refresh token
- Logout
- BCrypt password hashing
- JWT access token
- Refresh token rotation
- Role-based access control

### 16.2 Roles

```text
STUDENT
FACULTY
ADMIN
SUPER_ADMIN
```

### 16.3 Permission Examples

```text
PROBLEM_CREATE
PROBLEM_UPDATE
CONTEST_CREATE
SUBMISSION_CREATE
SUBMISSION_REVIEW
PLAGIARISM_REVIEW
USER_MANAGE
SYSTEM_AUDIT_VIEW
```

### 16.4 Phase 2 Auth

Add Keycloak for:

- OAuth2
- OpenID Connect
- SSO
- Realm management
- Advanced role management

---

## 17. CIA Security Design

### 17.1 Confidentiality

Controls:

- BCrypt password hashing
- JWT expiry
- Refresh token rotation
- Hidden test case protection
- Role-based API access
- Admin-only test case access
- Sensitive data masking
- Environment variables for secrets
- CORS restrictions

### 17.2 Integrity

Controls:

- Database constraints
- Foreign keys
- Submission immutability
- Source code hashing
- Audit logs
- Idempotent consumers
- Transactional updates
- Input validation
- Role-based modification controls

### 17.3 Availability

Controls:

- RabbitMQ async processing
- Retry queue
- Dead-letter queue
- Redis caching
- Rate limiting
- Health checks
- Graceful error handling
- Docker Compose recovery
- Monitoring dashboards

---

## 18. Accountability Design

Every critical action should be audited.

### 18.1 Audit Events

```text
USER_REGISTERED
USER_LOGIN
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
SUBMISSION_EVALUATED
LEADERBOARD_UPDATED
PLAGIARISM_CHECK_STARTED
PLAGIARISM_FLAGGED
ADMIN_REJUDGED_SUBMISSION
```

### 18.2 Audit Metadata

Store:

```text
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

## 19. Code Execution Security

### 19.1 Execution Strategy

Use Judge0 CE as the execution engine.

Spring Boot does not directly execute user code.

Instead:

```text
Spring Boot Worker → Judge0 CE API → Docker-isolated execution
```

### 19.2 Security Controls

- Time limit
- Memory limit
- Output size limit
- Network isolation where possible
- Temporary execution environment
- No direct host execution
- Clean up temporary files
- Store only required output

### 19.3 Important Limitation

This system is suitable for educational/self-hosted use. Commercial-grade hostile-code sandboxing requires deeper infrastructure hardening.

---

## 20. Plagiarism and Similarity Detection Design

### 20.1 Tool

Use JPlag.

### 20.2 Workflow

```text
Contest ends
    ↓
Admin triggers plagiarism check
    ↓
System collects accepted/relevant submissions
    ↓
JPlag runs similarity analysis
    ↓
Similarity reports are stored
    ↓
Suspicious pairs are flagged
    ↓
Admin/faculty reviews results
```

### 20.3 Important Naming

Do not call this absolute plagiarism detection.

Call it:

```text
Code similarity flagging
```

---

## 21. Frontend Architecture

### 21.1 Frontend Modules

```text
src/
├── app
├── routes
├── components
├── features
│   ├── auth
│   ├── contests
│   ├── problems
│   ├── submissions
│   ├── leaderboard
│   ├── admin
│   └── plagiarism
├── hooks
├── lib
├── services
├── schemas
└── types
```

### 21.2 Key Screens

Student:

- Login/Register
- Student dashboard
- Contest list
- Contest detail
- Problem-solving page
- Monaco editor
- Submission result
- Leaderboard
- Submission history

Faculty:

- Faculty dashboard
- Create problem
- Add test cases
- Create contest
- View submissions
- View analytics
- Review plagiarism flags

Admin:

- Admin dashboard
- User management
- Audit logs
- System metrics
- Platform settings

---

## 22. DevOps Architecture

### 22.1 Docker Services

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
keycloak optional
nginx optional
```

### 22.2 Local Run Command

```bash
docker compose up --build
```

### 22.3 Local URLs

```text
Frontend: http://localhost:5173
Backend API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui
RabbitMQ UI: http://localhost:15672
Prometheus: http://localhost:9090
Grafana: http://localhost:3000
MailHog: http://localhost:8025
```

---

## 23. CI/CD Design

### 23.1 GitHub Actions Pipeline

Stages:

```text
checkout
setup Java
backend unit tests
backend integration tests
setup Node
frontend lint
frontend build
Docker build validation
optional Docker image publish
```

### 23.2 Backend CI

- Compile Java code
- Run unit tests
- Run integration tests
- Validate Flyway migrations
- Build JAR

### 23.3 Frontend CI

- Install dependencies
- Type check
- Lint
- Build React app

### 23.4 Docker CI

- Build backend image
- Build frontend image
- Validate docker-compose configuration

---

## 24. Observability Design

### 24.1 Metrics

Track:

```text
total submissions
queued submissions
running submissions
accepted submissions
failed submissions
average evaluation time
RabbitMQ queue depth
API latency
API error rate
login failures
active contests
plagiarism checks triggered
```

### 24.2 Health Checks

Expose through Spring Boot Actuator:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

### 24.3 Dashboards

Grafana dashboards:

- API health
- Submission evaluation performance
- RabbitMQ queue depth
- JVM memory usage
- Error rate
- Contest activity

---

## 25. Logging Design

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

Important logs:

- Authentication attempts
- Submission received
- Evaluation started
- Evaluation completed
- Evaluation failed
- Admin action
- Plagiarism check completed

---

## 26. Testing Strategy

### 26.1 Unit Tests

Test:

- Auth service
- JWT service
- Problem service
- Contest service
- Submission service
- Leaderboard logic
- Plagiarism flag logic

### 26.2 Integration Tests

Use Testcontainers for:

- PostgreSQL
- Redis
- RabbitMQ

Test:

- Submission queue flow
- Repository queries
- Redis leaderboard updates
- RabbitMQ message consumption

### 26.3 API Tests

Use MockMvc or RestAssured.

Test:

- Auth endpoints
- Contest APIs
- Problem APIs
- Submission APIs
- Admin APIs

### 26.4 Frontend Tests

Use:

- React Testing Library
- Playwright optional

Test:

- Login flow
- Contest page
- Code submission flow
- Leaderboard rendering

---

## 27. Scaling Considerations

### 27.1 API Scaling

The API can scale horizontally because submission evaluation is async.

### 27.2 Worker Scaling

Multiple evaluation workers can consume from RabbitMQ.

### 27.3 Database Scaling

Use:

- Proper indexes
- Pagination
- Read-optimized queries
- Archive old submissions later

### 27.4 Redis Scaling

Redis is used for leaderboard and cache, not permanent source of truth.

### 27.5 RabbitMQ Scaling

Use:

- Durable queues
- Manual acknowledgements
- Dead-letter queues
- Retry queues

---

## 28. MVP Scope

The first complete version should include:

- Student/faculty/admin auth
- Problem creation
- Test case management
- Contest creation
- Student contest participation
- Java code submission
- RabbitMQ async evaluation
- Judge0 CE integration
- Hidden test cases
- Score calculation
- Redis leaderboard
- Basic admin/faculty dashboard
- Docker Compose setup
- Swagger docs
- Basic tests

---

## 29. Advanced Scope

Later versions can include:

- Python and C++ support
- JPlag similarity reports
- Keycloak integration
- Rejudge submissions
- Leaderboard freeze
- Editorials
- Problem tags
- User rating system
- Team contests
- Classroom/batch analytics
- Export reports
- Playwright E2E tests
- Loki logging
- Nginx reverse proxy

---

## 30. Project Repository Structure

```text
codejudgex/
├── backend/
│   ├── src/main/java/com/codejudgex/
│   ├── src/test/java/com/codejudgex/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── infra/
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   ├── grafana/
│   ├── nginx/
│   └── mailhog/
│
├── docs/
│   ├── architecture.md
│   ├── api-design.md
│   ├── database-design.md
│   ├── security.md
│   ├── devops.md
│   ├── observability.md
│   ├── testing.md
│   └── tradeoffs.md
│
├── .github/
│   └── workflows/
│       ├── backend-ci.yml
│       ├── frontend-ci.yml
│       └── docker-ci.yml
│
├── README.md
└── LICENSE
```

---

## 31. Engineering Tradeoffs

### 31.1 Modular Monolith vs Microservices

Chosen: Modular monolith.

Reason:

- Easier development
- Easier debugging
- Faster iteration
- Lower operational complexity
- Still allows clean module boundaries

### 31.2 RabbitMQ vs Kafka

Chosen: RabbitMQ.

Reason:

- Better for job queues
- Easier setup
- Suitable for submission evaluation tasks
- Lower complexity than Kafka

### 31.3 Judge0 CE vs Custom Sandbox

Chosen: Judge0 CE.

Reason:

- Avoids unsafe manual code execution
- Already designed for code evaluation
- Supports multiple languages
- Self-hostable

### 31.4 Redis vs PostgreSQL Leaderboard

Chosen: Both.

Reason:

- Redis gives fast ranking
- PostgreSQL remains source of truth

### 31.5 Spring Security JWT vs Keycloak First

Chosen: Spring Security JWT first.

Reason:

- Better learning value
- Simpler initial setup
- Keycloak can be added later

---

## 32. Success Criteria

CodeJudgeX is successful when:

- A student can join a contest and submit code.
- The submission is evaluated asynchronously.
- Hidden test cases are used correctly.
- Results are stored accurately.
- Leaderboard updates automatically.
- Faculty can create contests/problems/test cases.
- Admin can audit important actions.
- The system runs locally with Docker Compose.
- Swagger documents the APIs.
- Tests verify core workflows.
- Prometheus/Grafana show useful metrics.

---

## 33. Final Positioning

CodeJudgeX should be positioned as:

> A zero-cost, self-hosted, production-inspired coding assessment platform built with Java, Spring Boot, React, PostgreSQL, Redis, RabbitMQ, Judge0 CE, Docker, and open-source DevOps tooling.

It is not just a resume project. It is a full-stack engineering learning platform that touches backend development, frontend development, databases, system design, security, DevOps, observability, testing, and real product workflows.

