# CodeJudgeX

**CodeJudgeX** is a zero-cost, self-hosted coding assessment and online judge platform built with Java, Spring Boot, React, PostgreSQL, Redis, RabbitMQ, Judge0 CE, Docker, and open-source DevOps tooling.

The platform is designed for colleges, training institutes, coding clubs, and placement preparation ecosystems to create coding contests, manage problems, evaluate submissions asynchronously, generate leaderboards, detect code similarity, and monitor platform health without relying on paid services.

---

## Project Status

CodeJudgeX is planned as a long-term engineering project.

Current phase:

```text
Planning and architecture documentation
```

Target first milestone:

```text
Student submits code → submission is queued → Judge0 evaluates code → result is saved → leaderboard updates
```

---

## Why CodeJudgeX Exists

Most colleges and training institutes conduct coding assessments using fragmented tools such as forms, spreadsheets, manual evaluation, WhatsApp groups, or paid coding platforms.

CodeJudgeX solves this by providing a complete self-hosted system for:

- Coding contests
- Programming assessments
- Hidden test case evaluation
- Student submissions
- Automated scoring
- Leaderboards
- Faculty dashboards
- Plagiarism/similarity detection
- Audit logging
- Observability and monitoring

---

## Core Features

### Student Features

- Register and login
- Join contests
- View contest problems
- Write code in browser using Monaco Editor
- Submit code
- View verdicts and scores
- Track submission history
- View leaderboard rank

### Faculty Features

- Create coding problems
- Add sample and hidden test cases
- Create contests
- Add problems to contests
- View student submissions
- Review contest analytics
- Trigger plagiarism/similarity checks
- Export reports later

### Admin Features

- Manage users and roles
- View audit logs
- Monitor submissions
- Review suspicious plagiarism flags
- View system metrics
- Manage platform settings later

---

## Final Tech Stack

### Backend

```text
Java 21
Spring Boot 3
Spring Web
Spring Security
Spring Data JPA
Hibernate
Maven
Lombok
MapStruct
Jakarta Validation
```

### Frontend

```text
React
Vite
TypeScript
Tailwind CSS
shadcn/ui
React Router
TanStack Query
Axios
React Hook Form
Zod
Recharts
Monaco Editor
```

### Database and Cache

```text
PostgreSQL
Flyway
Redis
```

### Messaging and Async Processing

```text
RabbitMQ
Spring AMQP
```

### Code Execution

```text
Judge0 CE self-hosted
Docker
```

### Plagiarism / Similarity Detection

```text
JPlag
```

### DevOps and Infrastructure

```text
Docker
Docker Compose
Nginx optional
GitHub Actions
GitHub Container Registry
MailHog
```

### Observability

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
Loki optional
Promtail optional
```

### API Documentation

```text
springdoc-openapi
Swagger UI
Bruno
```

### Testing

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

## High-Level Architecture

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

## Core Workflow

### Submission Evaluation Flow

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
Worker sends code and test cases to Judge0 CE
    ↓
Judge0 executes code
    ↓
Worker compares actual output with expected output
    ↓
Score is calculated
    ↓
Submission result is saved in PostgreSQL
    ↓
Leaderboard is updated in Redis
```

---

## Main System Modules

```text
backend/
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

## API Documentation

Swagger UI will be available locally at:

```text
http://localhost:8080/swagger-ui
```

API base path:

```text
/api/v1
```

Main API groups:

```text
/api/v1/auth
/api/v1/users
/api/v1/problems
/api/v1/test-cases
/api/v1/contests
/api/v1/submissions
/api/v1/leaderboards
/api/v1/plagiarism
/api/v1/admin
/api/v1/audit-logs
```

---

## Local Development Setup

### Prerequisites

Install:

```text
Java 21
Node.js 20+
Docker
Docker Compose
Git
```

### Start Infrastructure

```bash
docker compose up --build
```

Expected local services:

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

## Security Design

CodeJudgeX follows CIA principles.

### Confidentiality

- BCrypt password hashing
- JWT authentication
- Refresh tokens
- Hidden test case protection
- Role-based access control
- Environment-based secrets
- CORS restrictions

### Integrity

- Database constraints
- Submission immutability
- Audit logs
- Source code hashing
- Input validation
- Transactional updates
- Idempotent queue consumers

### Availability

- RabbitMQ async processing
- Redis caching
- Retry queues
- Dead-letter queues
- Health checks
- Rate limiting
- Monitoring dashboards

---

## Observability

The system will expose metrics through Spring Boot Actuator and Micrometer.

Metrics include:

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
```

Grafana dashboards will visualize:

- API health
- Submission evaluation performance
- RabbitMQ queues
- JVM metrics
- Contest activity
- Error rates

---

## Testing Strategy

CodeJudgeX will include:

- Unit tests
- Service tests
- Repository tests
- Controller tests
- Integration tests
- Queue tests
- Redis tests
- RabbitMQ tests
- Frontend component tests
- Optional end-to-end tests

Testcontainers will be used for PostgreSQL, Redis, and RabbitMQ integration tests.

---

## Project Structure

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
│   ├── system-design.md
│   ├── database-design.md
│   ├── api-design.md
│   ├── security.md
│   ├── auth-and-roles.md
│   ├── backend-structure.md
│   ├── frontend-design.md
│   ├── submission-evaluation.md
│   ├── judge0-integration.md
│   ├── rabbitmq-workflows.md
│   ├── redis-usage.md
│   ├── plagiarism-detection.md
│   ├── audit-logging.md
│   ├── observability.md
│   ├── devops.md
│   ├── ci-cd.md
│   ├── testing.md
│   ├── deployment-local.md
│   ├── tradeoffs.md
│   ├── roadmap.md
│   └── demo-script.md
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

## Development Roadmap

### Phase 1: Core Online Judge

- Authentication
- Roles
- Problem creation
- Test case management
- Contest creation
- Code submission
- RabbitMQ evaluation queue
- Judge0 CE integration
- Submission result
- Basic leaderboard

### Phase 2: Strong Platform Version

- Redis leaderboard
- Hidden test cases
- Weighted scoring
- Audit logs
- Faculty dashboard
- Admin dashboard
- MailHog notifications
- Swagger documentation
- Testcontainers

### Phase 3: Production-Inspired Version

- JPlag similarity checks
- Prometheus and Grafana
- CI/CD with GitHub Actions
- Retry queues
- Dead-letter queues
- Rate limiting
- Structured logging
- Correlation IDs

### Phase 4: Advanced Version

- Keycloak integration
- Multi-language support
- Rejudge submissions
- Leaderboard freeze
- Editorials
- Problem tags
- Team contests
- Classroom analytics
- Report exports

---

## Engineering Tradeoffs

### Modular Monolith First

CodeJudgeX starts as a modular monolith to avoid unnecessary distributed-system complexity while maintaining clean boundaries.

### RabbitMQ Instead of Kafka

RabbitMQ is chosen because submission evaluation is a job-processing workflow, not high-volume event streaming.

### Judge0 CE Instead of Custom Sandbox

Judge0 CE is used to avoid unsafe manual code execution and to support multiple languages through a self-hosted execution engine.

### Redis + PostgreSQL for Leaderboard

Redis provides fast ranking. PostgreSQL remains the source of truth.

### Spring Security JWT First

Spring Security JWT is used first for learning and control. Keycloak can be added later for enterprise identity management.

---

## Success Criteria

CodeJudgeX is successful when:

- A student can join a contest.
- A faculty user can create problems and hidden test cases.
- A student can submit code.
- The submission is evaluated asynchronously.
- Judge0 CE returns verdicts correctly.
- Results are persisted in PostgreSQL.
- Leaderboard updates through Redis.
- Admin/faculty can review submissions.
- Important actions are audited.
- The full system runs with Docker Compose.
- APIs are documented with Swagger.
- Core workflows are covered by tests.

---

## Final Positioning

CodeJudgeX is a serious full-stack engineering project, not a CRUD application.

It demonstrates:

- Backend engineering
- Frontend development
- Async processing
- Secure code execution
- Database design
- Redis caching
- Queue-based architecture
- API design
- DevOps
- CI/CD
- Testing
- Observability
- Security
- Accountability
- System design maturity

