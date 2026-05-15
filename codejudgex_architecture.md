# CodeJudgeX Architecture

## 1. Architecture Overview

CodeJudgeX uses a **modular monolith + asynchronous worker architecture**.

The system is designed to be simple enough to build and run locally, but strong enough to demonstrate enterprise-level engineering concepts such as async processing, queue-based workflows, caching, secure execution, observability, and auditability.

---

## 2. High-Level Architecture

```text
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
Result Processor
    ↓
Redis Leaderboard + PostgreSQL Results
    ↓
JPlag Similarity Detection
    ↓
Prometheus + Grafana Monitoring
```

---

## 3. Main Architecture Decision

### Chosen Architecture

```text
Modular Monolith + Async Workers
```

### Why This Is Chosen

CodeJudgeX should not start as microservices.

A modular monolith is better because:

- Easier to develop
- Easier to debug
- Easier to test
- Faster to ship
- Lower DevOps complexity
- Still supports clean separation of modules
- Can later evolve into services if needed

The system still uses async architecture through RabbitMQ, so it is not a simple CRUD monolith.

---

## 4. Core Components

## 4.1 Frontend Application

Technology:

```text
React
Vite
TypeScript
Tailwind CSS
shadcn/ui
Monaco Editor
TanStack Query
```

Responsibilities:

- Student dashboard
- Faculty dashboard
- Admin dashboard
- Contest pages
- Problem-solving interface
- Code editor
- Leaderboard display
- Submission results
- Analytics charts

---

## 4.2 Spring Boot API

Technology:

```text
Java 21
Spring Boot 3
Spring Security
Spring Data JPA
Hibernate
Spring AMQP
Spring Data Redis
```

Responsibilities:

- REST APIs
- Authentication
- Authorization
- Problem management
- Contest management
- Submission management
- Admin operations
- Audit logging
- Publishing messages to RabbitMQ
- Reading/writing PostgreSQL and Redis

---

## 4.3 PostgreSQL

Used as the primary source of truth.

Stores:

- Users
- Roles
- Problems
- Test cases
- Contests
- Contest participants
- Submissions
- Submission results
- Leaderboard snapshots
- Plagiarism flags
- Notifications
- Audit logs

---

## 4.4 Redis

Used for fast, temporary, high-read data.

Responsibilities:

- Live leaderboard
- Submission status cache
- Rate limiting
- Contest stats cache
- Optional JWT blacklist

PostgreSQL remains the permanent source of truth.

---

## 4.5 RabbitMQ

Used for asynchronous job processing.

Responsibilities:

- Submission evaluation queue
- Result processing queue
- Notification queue
- Plagiarism check queue
- Retry queue
- Dead-letter queue

RabbitMQ prevents long-running code evaluation from blocking API requests.

---

## 4.6 Evaluation Worker

The evaluation worker is responsible for processing queued submissions.

Responsibilities:

- Consume submission jobs from RabbitMQ
- Fetch submission, problem, and test cases
- Send code execution request to Judge0 CE
- Process Judge0 response
- Compare outputs
- Calculate score
- Update submission status
- Update leaderboard
- Publish notification/result events

---

## 4.7 Judge0 CE

Judge0 CE is the self-hosted code execution engine.

Responsibilities:

- Compile submitted code
- Execute code safely inside isolated environment
- Enforce time limits
- Enforce memory limits
- Return output, errors, and execution status

Spring Boot does not directly execute user code.

---

## 4.8 JPlag

JPlag is used for code similarity detection.

Responsibilities:

- Run post-contest similarity checks
- Compare student submissions
- Generate similarity reports
- Help faculty/admin review suspicious submissions

This should be called **similarity detection**, not absolute plagiarism proof.

---

## 4.9 Observability Stack

Technology:

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
```

Responsibilities:

- Track API health
- Track submission metrics
- Track RabbitMQ queue depth
- Track JVM metrics
- Track error rates
- Visualize platform health

---

## 5. Backend Module Architecture

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

---

## 6. Module Responsibilities

## 6.1 Auth Module

Handles:

- Registration
- Login
- JWT access tokens
- Refresh tokens
- Password hashing
- Logout
- Token validation

---

## 6.2 User Module

Handles:

- User profile
- Student/faculty/admin data
- User status
- Department/year metadata

---

## 6.3 Role Module

Handles:

- Role assignment
- Permission mapping
- Role-based authorization

Roles:

```text
STUDENT
FACULTY
ADMIN
SUPER_ADMIN
```

---

## 6.4 Problem Module

Handles:

- Problem creation
- Problem update
- Problem listing
- Problem difficulty
- Tags
- Problem constraints

---

## 6.5 Test Case Module

Handles:

- Sample test cases
- Hidden test cases
- Weighted test cases
- Test case visibility

---

## 6.6 Contest Module

Handles:

- Contest creation
- Contest schedule
- Contest status
- Contest participants
- Contest-problem mapping

Contest statuses:

```text
DRAFT
UPCOMING
LIVE
ENDED
ARCHIVED
```

---

## 6.7 Submission Module

Handles:

- Code submission
- Submission validation
- Submission status
- Submission history
- Publishing evaluation jobs

---

## 6.8 Evaluation Module

Handles:

- RabbitMQ consumption
- Judge0 integration
- Verdict generation
- Score calculation
- Result persistence

---

## 6.9 Leaderboard Module

Handles:

- Redis leaderboard updates
- Rank calculation
- Score aggregation
- PostgreSQL leaderboard snapshots

---

## 6.10 Plagiarism Module

Handles:

- JPlag execution
- Similarity reports
- Suspicious submission flags
- Faculty review workflow

---

## 6.11 Notification Module

Handles:

- In-app notifications
- Email testing through MailHog
- Notification queue consumption

---

## 6.12 Audit Module

Handles:

- Important action tracking
- Admin activity logs
- Submission evaluation logs
- User login history

---

## 7. Request Flow: User Login

```text
User submits email/password
    ↓
Spring Security validates credentials
    ↓
Password checked with BCrypt
    ↓
JWT access token generated
    ↓
Refresh token generated
    ↓
Login audit event stored
    ↓
Token returned to frontend
```

---

## 8. Request Flow: Faculty Creates Problem

```text
Faculty sends create problem request
    ↓
JWT is validated
    ↓
Role permission checked
    ↓
Problem payload validated
    ↓
Problem stored in PostgreSQL
    ↓
Audit log created
    ↓
Response returned
```

---

## 9. Request Flow: Student Submits Code

```text
Student submits code
    ↓
JWT is validated
    ↓
Contest access is checked
    ↓
Submission payload is validated
    ↓
Submission saved as QUEUED
    ↓
Submission job published to RabbitMQ
    ↓
API returns immediately
```

Important:

```text
The API does not evaluate code synchronously.
```

---

## 10. Async Flow: Submission Evaluation

```text
RabbitMQ contains submission job
    ↓
Evaluation worker consumes message
    ↓
Submission status updated to RUNNING
    ↓
Worker loads hidden test cases
    ↓
Worker sends execution request to Judge0 CE
    ↓
Judge0 executes code
    ↓
Worker receives output/status
    ↓
Output is compared with expected output
    ↓
Score is calculated
    ↓
Submission status updated
    ↓
Submission result saved
    ↓
Redis leaderboard updated
    ↓
Notification event published
```

---

## 11. Async Flow: Plagiarism Check

```text
Contest ends
    ↓
Faculty/Admin triggers similarity check
    ↓
Plagiarism job published to RabbitMQ
    ↓
Worker collects submissions
    ↓
JPlag runs similarity analysis
    ↓
Similarity reports stored
    ↓
Suspicious submissions flagged
    ↓
Admin/faculty reviews flags
```

---

## 12. Data Flow Summary

```text
Frontend → Backend API → PostgreSQL
Frontend → Backend API → RabbitMQ → Worker → Judge0
Worker → PostgreSQL
Worker → Redis
Worker → RabbitMQ Notification Queue
Prometheus → Backend Metrics
Grafana → Prometheus Data
```

---

## 13. Security Architecture

Security is applied at multiple levels:

```text
Frontend route protection
JWT authentication
Role-based backend authorization
Permission checks
Hidden test case protection
Input validation
Rate limiting
Audit logging
Judge0 sandboxing
Environment variables
CORS policy
```

---

## 14. Availability Architecture

Availability is improved using:

```text
RabbitMQ async processing
Retry queues
Dead-letter queues
Redis caching
Health checks
Docker Compose restart policies
Graceful error handling
Monitoring dashboards
```

---

## 15. Consistency Model

CodeJudgeX uses a mix of strong and eventual consistency.

### Strong Consistency

Used for:

- User creation
- Problem creation
- Contest creation
- Test case storage
- Submission record creation

### Eventual Consistency

Used for:

- Submission evaluation
- Leaderboard update
- Notification delivery
- Plagiarism checks

Example:

```text
A submission may be saved immediately, but its final verdict appears after async evaluation completes.
```

---

## 16. Fault Tolerance

Failure cases to handle:

```text
RabbitMQ message processing failure
Judge0 timeout
Judge0 unavailable
Database error
Redis unavailable
Worker crash
Duplicate message delivery
Invalid submission payload
```

Handling strategies:

```text
Retry queue
Dead-letter queue
Idempotent consumers
Status transition checks
Fallback to PostgreSQL if Redis unavailable
Health checks
Structured error logs
```

---

## 17. Scalability Strategy

### API Scaling

The API can scale horizontally because long-running evaluation work is offloaded to RabbitMQ workers.

### Worker Scaling

Multiple workers can consume from the evaluation queue.

### Database Scaling

Use:

- Indexes
- Pagination
- Query optimization
- Archival strategy later

### Redis Scaling

Used for fast leaderboard and cache access.

### RabbitMQ Scaling

Use:

- Durable queues
- Manual acknowledgement
- Retry queues
- Dead-letter queues

---

## 18. Deployment Architecture

Local-first deployment:

```text
Docker Compose
```

Services:

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

Optional later:

```text
nginx
keycloak
loki
promtail
```

---

## 19. Architecture Success Criteria

The architecture is successful if:

- API requests remain fast.
- Code evaluation happens asynchronously.
- Hidden test cases are protected.
- Leaderboard updates correctly.
- System can recover from worker failure.
- Failed jobs move to DLQ.
- Redis improves read-heavy operations.
- PostgreSQL remains source of truth.
- Metrics are visible in Grafana.
- Audit logs track important actions.

---

## 20. Final Architecture Summary