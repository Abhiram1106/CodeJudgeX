<div align="center">

# CodeJudgeX

**Enterprise-Grade Competitive Programming Judge Platform for Academic Institutions**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.4-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

<br/>

> A full-stack, production-inspired online judge built for universities and colleges — powering coding contests, automated evaluation, plagiarism detection, real-time leaderboards, and institutional analytics with enterprise-grade security and observability.

<br/>

[Overview](#overview) · [Architecture](#architecture) · [Features](#features) · [Tech Stack](#tech-stack) · [Quick Start](#quick-start) · [API Reference](#api-reference) · [Security](#security) · [Documentation](#documentation)

</div>

---

## Overview

CodeJudgeX is a battle-tested competitive programming judge designed to meet the operational demands of academic institutions at scale. It enables faculty to create and manage coding contests, students to submit solutions in multiple programming languages, and administrators to gain full visibility into platform activity — all within a secure, observable, and audit-compliant system.

The platform is architected as a **modular monolith with asynchronous workers**, meaning it is simple to build, debug, and deploy while retaining the performance characteristics of event-driven systems. Code evaluation is fully decoupled from the API layer via RabbitMQ, ensuring the API remains fast and responsive even under high contest load.

### What Makes This Different

| Capability | Description |
|---|---|
| **Async Evaluation Pipeline** | Submissions are queued via RabbitMQ and processed by dedicated workers — the API never blocks on code execution |
| **Sandboxed Code Execution** | All submitted code runs inside Judge0 CE, an isolated and resource-limited execution environment |
| **Role-Based Access Control** | Four-tier role system (Student, Faculty, Admin, Super Admin) with granular permission enforcement |
| **Similarity Detection** | Post-contest JPlag integration flags suspiciously similar submissions for faculty review |
| **Real-Time Leaderboard** | Redis-powered live rankings updated instantly after each evaluation |
| **Full Auditability** | Every significant action is logged with actor, timestamp, and metadata |
| **Production Observability** | Spring Actuator exposes health, metrics, and JVM internals |

---

## Architecture

CodeJudgeX follows a **Modular Monolith + Async Worker** pattern — purpose-built for academic deployment where simplicity and correctness matter more than premature distribution.

```
┌─────────────────────────────────────────────────────────────────────┐
│                          React Frontend                              │
│          (Vite · TypeScript · Tailwind CSS · shadcn/ui)             │
│         Monaco Editor · TanStack Query · React Router               │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP / REST
┌────────────────────────────▼────────────────────────────────────────┐
│                      Spring Boot REST API                            │
│     Java 21 · Spring Security · Spring Data JPA · Spring AMQP      │
│                                                                      │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│   │   Auth   │ │ Problem  │ │ Contest  │ │Submission│ │  Admin │  │
│   └──────────┘ └──────────┘ └──────────┘ └────┬─────┘ └────────┘  │
│                                                │ Publish            │
└──────────────────────┬─────────────────────────┼────────────────────┘
                       │                         │
         ┌─────────────▼──────┐    ┌─────────────▼─────────────────┐
         │     PostgreSQL      │    │           RabbitMQ             │
         │  (Source of Truth)  │    │  Evaluation · Notification     │
         └─────────────────────┘    │  Plagiarism · Retry · DLQ     │
                                    └─────────────┬─────────────────┘
         ┌───────────────────┐                    │ Consume
         │       Redis        │    ┌──────────────▼──────────────────┐
         │  Live Leaderboard  │    │        Evaluation Worker         │
         │  Status Cache      │◄───│  Fetch · Execute · Score · Rank │
         │  Rate Limiting     │    └──────────────┬──────────────────┘
         └───────────────────┘                    │
                                    ┌─────────────▼──────────────────┐
                                    │     Judge0 CE (remote/hosted)   │
                                    │  Compile · Execute · Sandbox    │
                                    │  Time Limit · Memory Limit      │
                                    └────────────────────────────────┘

         ┌────────────────────────────────────────────────────────┐
         │                   Observability Layer                   │
         │              Spring Actuator (health, metrics)          │
         └────────────────────────────────────────────────────────┘
```

### Submission Lifecycle

The most critical workflow in CodeJudgeX is asynchronous by design:

```
Student submits code via API
        │
        ▼
API validates JWT + contest access + payload
        │
        ▼
Submission saved to PostgreSQL  ←── Status: QUEUED
        │
        ▼
Evaluation job published to RabbitMQ
        │
        ▼   (API returns 202 Accepted immediately)
Evaluation Worker consumes message
        │
        ▼
Status updated  ←── RUNNING
        │
        ▼
Hidden test cases loaded from PostgreSQL
        │
        ▼
Execution request sent to Judge0 CE (sandboxed)
        │
        ▼
Output compared with expected output per test case
        │
        ▼
Score calculated · Verdict assigned
        │
        ▼
Submission result persisted  ←── ACCEPTED / WRONG_ANSWER / TLE / ...
        │
        ▼
Redis leaderboard updated instantly
        │
        ▼
Notification event published → Student notified
```

### Consistency Model

| Operation | Consistency |
|---|---|
| User / Problem / Contest creation | Strong (synchronous PostgreSQL write) |
| Submission record creation | Strong |
| Evaluation result | Eventual (async worker) |
| Leaderboard ranking | Eventual (Redis update post-evaluation) |
| Notifications | Eventual (notification queue) |
| Plagiarism reports | Eventual (post-contest background job) |

### Fault Tolerance Strategy

| Failure Scenario | Mitigation |
|---|---|
| Evaluation worker crash | RabbitMQ message re-queued automatically |
| Judge0 timeout | Submission marked `EVALUATION_ERROR`, retried via retry queue |
| Redis unavailable | Leaderboard falls back to PostgreSQL read |
| Duplicate message delivery | Idempotent consumer with status transition guards |
| Persistent evaluation failure | Message routed to Dead-Letter Queue for investigation |

---

## Features

### For Students
- Register, log in, and access role-specific dashboards
- Browse upcoming and live contests
- Solve problems with an in-browser Monaco code editor (with syntax highlighting)
- Submit solutions in multiple programming languages
- View real-time submission status and full test case results
- Track personal submission history and performance
- Monitor live contest leaderboard rankings
- Receive in-app notifications for evaluation results

### For Faculty
- Create and manage coding problems with full metadata (difficulty, tags, constraints, time/memory limits)
- Define sample and hidden test cases with configurable weights
- Create and schedule contests (DRAFT → UPCOMING → LIVE → ENDED)
- Assign problems to contests with custom point values
- View all student submissions per contest
- Trigger post-contest similarity analysis
- Review plagiarism flags with resolution workflow

### For Administrators
- Full user management (create, update, assign roles, activate/deactivate)
- Platform-wide analytics dashboard
- System health monitoring
- Complete audit log access
- Admin-level view of all contests and submissions
- Plagiarism flag oversight

### Platform
- JWT-based authentication with refresh token rotation
- Multi-language code execution (Java, C++, Python, JavaScript, and all Judge0-supported languages)
- Paginated, filterable REST API with consistent response envelopes
- Swagger/OpenAPI documentation auto-generated at `/swagger-ui`
- Health and metrics exposed via Spring Actuator at `/actuator/health` and `/actuator/prometheus`
- Runs on natively installed local services — no Docker required

---

## Tech Stack

### Backend

| Component | Technology | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3 |
| Security | Spring Security + JWT (jjwt) | — |
| Persistence | Spring Data JPA + Hibernate | — |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | — |
| Cache / Leaderboard | Redis (Spring Data Redis) | 7 |
| Message Broker | RabbitMQ (Spring AMQP) | 3 |
| Code Execution | Judge0 CE (remote/hosted instance) | 1.13 |
| Similarity Detection | JPlag | — |
| Mapping | MapStruct | 1.5 |
| Boilerplate reduction | Lombok | — |
| Validation | Jakarta Validation | — |
| Metrics | Micrometer + Prometheus Registry | — |
| API Docs | springdoc-openapi (Swagger UI) | — |

### Frontend

| Component | Technology | Version |
|---|---|---|
| Language | TypeScript | 5.4 |
| Framework | React | 18 |
| Build Tool | Vite | 5 |
| Styling | Tailwind CSS | 3 |
| Component Library | shadcn/ui | — |
| Code Editor | Monaco Editor (`@monaco-editor/react`) | — |
| Data Fetching | TanStack Query | 5 |
| HTTP Client | Axios | 1.6 |
| Routing | React Router | 6 |
| Forms | React Hook Form + Zod | — |
| Charts | Recharts | 2 |
| State Management | Zustand | 4 |

### Infrastructure

| Component | Technology |
|---|---|
| Orchestration | Docker Compose (`infra/docker-compose.yml`) |
| PostgreSQL, Redis, RabbitMQ | Containerized services |
| Code execution | Judge0 CE (self-hosted via Docker, `judge0-server` + `judge0-workers`) |
| Metrics | Spring Boot Actuator + Prometheus + Grafana |
| Email (dev) | MailHog (containerized) |
| Reverse proxy | Nginx (serves frontend, proxies `/api/*` to backend) |

---

## Repository Structure

```
CodeJudgeX/
│
├── backend/                              # Spring Boot application
│   ├── Dockerfile                        # Multi-stage build (JDK → JRE)
│   ├── .dockerignore
│   ├── pom.xml                           # Maven build descriptor
│   └── src/
│       ├── main/
│       │   ├── java/com/codejudgex/
│       │   │   ├── CodeJudgeXApplication.java
│       │   │   ├── auth/                 # JWT auth, login, register, refresh
│       │   │   ├── user/                 # User profile management
│       │   │   ├── role/                 # Role assignment and permissions
│       │   │   ├── problem/              # Problem CRUD, tags, difficulty
│       │   │   ├── testcase/             # Sample and hidden test cases
│       │   │   ├── contest/              # Contest lifecycle management
│       │   │   ├── submission/           # Code submission + queue publish
│       │   │   ├── evaluation/           # RabbitMQ consumer + Judge0 + scoring
│       │   │   ├── leaderboard/          # Redis ranking + PostgreSQL snapshots
│       │   │   ├── plagiarism/           # JPlag integration + flag review
│       │   │   ├── notification/         # In-app + email notifications
│       │   │   ├── audit/                # Immutable audit event logging
│       │   │   ├── admin/                # Admin-only operations
│       │   │   ├── analytics/            # Platform statistics
│       │   │   ├── common/               # Shared DTOs, exceptions, utilities
│       │   │   └── infrastructure/       # RabbitMQ config, Redis config, etc.
│       │   └── resources/
│       │       └── application.yml       # All config via env vars
│       └── test/
│           └── java/com/codejudgex/     # Module-level tests
│
├── frontend/                             # React + Vite application
│   ├── Dockerfile                        # Multi-stage build (Node → Nginx)
│   ├── .dockerignore
│   ├── nginx/
│   │   └── default.conf                  # Nginx reverse proxy config
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts                    # Vite config + API proxy
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── app/                          # App entry, global styles, providers
│       ├── pages/                        # Route-level page components
│       │   ├── auth/                     # Login, register
│       │   ├── dashboard/                # Role-specific dashboards
│       │   ├── contest/                  # Contest browse and detail
│       │   ├── problem/                  # Problem view with editor
│       │   ├── submission/               # Submission history and result
│       │   ├── leaderboard/              # Contest rankings
│       │   ├── faculty/                  # Faculty management pages
│       │   └── admin/                    # Admin control panel
│       ├── components/                   # Reusable UI components
│       │   ├── ui/                       # shadcn/ui primitives
│       │   ├── editor/                   # Monaco editor wrapper
│       │   ├── layout/                   # Sidebar, navbar, shell
│       │   └── shared/                   # Tables, cards, dialogs
│       ├── features/                     # Feature-scoped logic
│       │   ├── auth/                     # Auth store, hooks, guards
│       │   ├── contest/                  # Contest queries and mutations
│       │   ├── problem/                  # Problem queries
│       │   ├── submission/               # Submission polling + status
│       │   └── leaderboard/              # Leaderboard queries
│       ├── hooks/                        # Shared custom React hooks
│       ├── lib/                          # Axios instance, query client
│       ├── services/                     # API service functions
│       ├── store/                        # Zustand global state
│       ├── types/                        # TypeScript type definitions
│       └── utils/                        # Formatting, validation helpers
│
├── infra/                                # Infrastructure configuration
│   ├── docker-compose.yml                # Full stack: infra + Judge0 + backend + frontend
│   ├── prometheus/
│   │   └── prometheus.yml                # Prometheus scrape config
│   └── .env.example                      # Environment variable template
│
├── docs/                                 # Architecture and design documents
│   ├── architecture.md                   # System architecture decisions
│   ├── backend_structure.md              # Backend module design
│   ├── frontend_design.md                # Frontend architecture
│   ├── api_design.md                     # REST API contracts
│   ├── database_design.md                # PostgreSQL schema design
│   ├── security.md                       # Security model and controls
│   ├── submission_evaluation.md          # Evaluation pipeline detail
│   ├── judge_0_integration.md            # Judge0 CE integration guide
│   ├── system_design.md                  # High-level system design
│   ├── enterprise_documentation.md       # Enterprise feature documentation
│   └── remaining_docs_pack.md            # Additional design reference
│
├── Makefile                              # Developer task runner
├── .gitignore
└── README.md
```

Each backend module follows a strict layered structure:

```
module-name/
├── controller/       # HTTP layer — request/response mapping
├── dto/
│   ├── request/      # Inbound payload DTOs with validation annotations
│   └── response/     # Outbound response DTOs (entities never exposed directly)
├── entity/           # JPA entities
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic
├── mapper/           # MapStruct interfaces for DTO ↔ entity conversion
└── exception/        # Module-specific exception types
```

---

## Quick Start

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Docker | 24 |
| Docker Compose | v2 (bundled with Docker Desktop) |

CodeJudgeX runs as a full Docker Compose stack — Postgres, Redis, RabbitMQ, Judge0 CE,
the Spring Boot backend, and the React frontend (served via Nginx) all start with one command.

> **Native (non-Docker) development is also supported** for faster backend/frontend
> iteration — see [Native Development](#native-development) below. It requires
> JDK 21, Maven 3.9, Node 20, and either Docker (for just the infra services) or
> natively installed Postgres/Redis/RabbitMQ/Judge0.

### 1. Clone and Configure

```bash
git clone https://github.com/your-org/CodeJudgeX.git
cd CodeJudgeX

# Copy environment template
cp infra/.env.example infra/.env

# Edit infra/.env and set at minimum:
# JWT_SECRET=<256-bit random string>
```

### 2. Start the Stack

```bash
make up
# or directly:
docker compose -f infra/docker-compose.yml up -d --build
```

This builds and starts: `postgres`, `redis`, `rabbitmq`, `judge0-db`, `judge0-redis`,
`judge0-server`, `judge0-workers`, `mailhog`, `backend`, `frontend`, `prometheus`, `grafana`.

Flyway runs database migrations automatically on backend startup.

### 3. Stop the Stack

```bash
make down
# or:
docker compose -f infra/docker-compose.yml down
```

### Service Endpoints

| Service | URL | Credentials |
| --- | --- | --- |
| Frontend | <http://localhost> | — |
| Backend API | <http://localhost:8080/api/v1> | — |
| Swagger UI | <http://localhost:8080/api/v1/swagger-ui.html> | — |
| RabbitMQ Management | <http://localhost:15672> | guest / guest |
| Judge0 CE | <http://localhost:2358> | — |
| MailHog | <http://localhost:8025> | — |
| Prometheus | <http://localhost:9090> | — |
| Grafana | <http://localhost:3001> | admin / admin (default) |

---

### Native Development

For faster backend/frontend iteration without rebuilding Docker images each time:

```bash
# Start only infra services (postgres, redis, rabbitmq, judge0, mailhog) in Docker
make infra-up

# Run the backend natively
make backend
# or: cd backend && ./mvnw spring-boot:run

# Install + run the frontend natively
make frontend-install
make frontend
# or: cd frontend && npm install && npm run dev
```

The API starts on `http://localhost:8080`, Swagger UI at `http://localhost:8080/api/v1/swagger-ui.html`,
and the frontend dev server on `http://localhost:5173` (proxies `/api` to the backend).

`infra/.env.example` documents the env vars needed for native runs (all default to `localhost`).

---

## API Reference

All API endpoints are versioned under `/api/v1` and return a consistent response envelope.

### Response Envelope

**Success:**
```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": { },
  "timestamp": "2026-05-23T10:30:00Z"
}
```

**Paginated list:**
```json
{
  "success": true,
  "message": "Data fetched successfully",
  "data": {
    "items": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-05-23T10:30:00Z"
}
```

**Error:**
```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request payload",
  "details": [
    { "field": "email", "message": "Email is required" }
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-05-23T10:30:00Z"
}
```

### Endpoint Overview

| Group | Base Path | Roles |
|---|---|---|
| Authentication | `/api/v1/auth` | Public |
| User Profile | `/api/v1/users` | Authenticated |
| Problems | `/api/v1/problems` | Student (read) · Faculty (write) |
| Test Cases | `/api/v1/problems/{id}/test-cases` | Faculty+ |
| Contests | `/api/v1/contests` | Student (join/read) · Faculty (write) |
| Submissions | `/api/v1/submissions` | Student |
| Leaderboard | `/api/v1/contests/{id}/leaderboard` | Authenticated |
| Plagiarism | `/api/v1/contests/{id}/plagiarism` | Faculty+ |
| Notifications | `/api/v1/notifications` | Authenticated |
| Admin | `/api/v1/admin` | Admin · Super Admin |
| Audit Logs | `/api/v1/admin/audit-logs` | Admin · Super Admin |

### Key Endpoints

```http
# Authentication
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me

# Problems
GET    /api/v1/problems?page=0&size=20&difficulty=MEDIUM&tag=dp
POST   /api/v1/problems                           # Faculty+
GET    /api/v1/problems/{problemId}
PUT    /api/v1/problems/{problemId}               # Faculty+
PATCH  /api/v1/problems/{problemId}/archive       # Faculty+

# Contests
GET    /api/v1/contests?status=LIVE
POST   /api/v1/contests                           # Faculty+
GET    /api/v1/contests/{contestId}
POST   /api/v1/contests/{contestId}/problems      # Faculty+
POST   /api/v1/contests/{contestId}/join          # Student

# Submissions (async — returns 202 Accepted)
POST   /api/v1/submissions
GET    /api/v1/submissions/my
GET    /api/v1/submissions/{submissionId}

# Leaderboard
GET    /api/v1/contests/{contestId}/leaderboard

# Plagiarism
POST   /api/v1/contests/{contestId}/plagiarism/check   # Faculty+ · 202 Accepted
GET    /api/v1/contests/{contestId}/plagiarism/flags   # Faculty+
PATCH  /api/v1/plagiarism/flags/{flagId}/review        # Faculty+
```

Complete API documentation with request/response schemas, error codes, and role requirements is available at **`http://localhost:8080/swagger-ui`** when the backend is running.

---

## Security

Security is a first-class concern across every layer of the platform.

### Authentication

- **JWT Access Tokens** — short-lived (15 minutes by default), signed with a configurable HMAC-SHA256 secret
- **Refresh Tokens** — long-lived, stored hashed in PostgreSQL, rotated on use
- **BCrypt** password hashing (strength factor 12)
- All secrets loaded exclusively from environment variables — never hardcoded

### Authorization

CodeJudgeX uses a four-tier role system enforced at the controller layer on every protected endpoint:

| Role | Capabilities |
|---|---|
| `STUDENT` | Browse contests, submit code, view own results, view leaderboard |
| `FACULTY` | All student capabilities + create problems, manage contests, review plagiarism |
| `ADMIN` | All faculty capabilities + user management, platform analytics, audit log access |
| `SUPER_ADMIN` | Full platform access including system configuration |

### Data Protection

- **Hidden test cases** are never returned to student-facing API responses — enforced at the service layer
- **Source code** is never included in list API responses, only in individual submission detail (own submissions only)
- **Password hashes and refresh token hashes** are never serialized in any response DTO
- **Internal stack traces** are suppressed in production error responses

### Infrastructure Security

- All sensitive configuration via environment variables (never in source code)
- Rate limiting on authentication endpoints and submission API
- CORS configured with an explicit allowlist
- Judge0 provides full sandboxing: submitted code cannot access the network, filesystem, or host resources
- All significant actions produce immutable audit log entries (actor, action, timestamp, metadata)

---

## Observability

CodeJudgeX exposes built-in health and metrics via Spring Boot Actuator — no separate monitoring stack required.

### Metrics

Spring Boot Actuator exposes Prometheus-formatted metrics at `/actuator/prometheus`, which can optionally be scraped by an external Prometheus instance if desired:

- API request rate, latency, and error rate by endpoint
- RabbitMQ queue depth (evaluation queue, notification queue, DLQ)
- JVM heap usage and GC activity
- Database connection pool utilization
- Submission throughput and verdict distribution

### Health Checks

```http
GET /actuator/health          # Overall application health
GET /actuator/info            # Build info
GET /actuator/prometheus      # Prometheus metrics endpoint
```

### Audit Logging

Every significant platform action generates an immutable audit event:

- User login and logout (with IP metadata)
- Problem creation and modification
- Contest state transitions
- Submission creation
- Evaluation completion and verdict
- Plagiarism flag creation and resolution
- Admin actions (role changes, user suspension)

Audit logs are queryable by admin with filtering on actor, action type, and time range.

---

## Development

### Available Make Targets

```bash
make backend            # Run Spring Boot in dev mode
make frontend           # Run Vite dev server
make frontend-install   # Install npm dependencies
make typecheck          # Run TypeScript type check
make backend-build      # Build production JAR (skip tests)
make backend-test       # Run backend test suite
make build              # Full production build (backend + frontend)
```

PostgreSQL, Redis, and RabbitMQ must be running as local services before starting the backend (see [Quick Start](#quick-start)).

### Backend Module Conventions

- Every public service method must validate inputs at the controller boundary using Jakarta Validation
- JPA entities are **never** returned directly from controllers — always map through response DTOs
- MapStruct mappers handle all entity ↔ DTO conversions
- Business logic lives exclusively in `@Service` classes
- Controllers are thin: validate → delegate to service → return response DTO
- Module-specific exceptions extend a common base and are handled by a global `@RestControllerAdvice`

### Frontend Conventions

- All API calls go through the typed service layer in `src/services/`
- Server state managed by TanStack Query — no manual loading/error state
- Form validation via React Hook Form + Zod schemas
- Page-level components live in `src/pages/`, reusable components in `src/components/`
- Feature-scoped logic (queries, mutations, hooks) lives in `src/features/{name}/`
- The `@/` path alias maps to `src/`

### Environment Variables

Copy `infra/.env.example` to `infra/.env`:

```bash
cp infra/.env.example infra/.env
```

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/codejudgex` |
| `DB_USER` | PostgreSQL username | `codejudgex` |
| `DB_PASS` | PostgreSQL password | `secret` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `RABBITMQ_HOST` | RabbitMQ hostname | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ AMQP port | `5672` |
| `RABBITMQ_USER` | RabbitMQ username | `guest` |
| `RABBITMQ_PASS` | RabbitMQ password | `guest` |
| `JUDGE0_URL` | Judge0 CE base URL | `http://localhost:2358` |
| `JUDGE0_TOKEN` | Judge0 auth token | _(empty for CE)_ |
| `JWT_SECRET` | HMAC-SHA256 signing key | **Must be changed in production** |

---

## Documentation

All design documents live in [`docs/`](docs/):

| Document | Description |
|---|---|
| [architecture.md](docs/architecture.md) | System architecture, component responsibilities, data flow, consistency model, fault tolerance, and scalability strategy |
| [backend_structure.md](docs/backend_structure.md) | Backend module structure, package conventions, layered design patterns |
| [frontend_design.md](docs/frontend_design.md) | Frontend architecture, component hierarchy, routing, state management |
| [api_design.md](docs/api_design.md) | Complete REST API design: endpoints, request/response formats, error codes, HTTP status standards |
| [database_design.md](docs/database_design.md) | PostgreSQL schema design, entity relationships, indexing strategy, Flyway migrations |
| [security.md](docs/security.md) | Security model, CIA principles, authentication, authorization, audit design |
| [submission_evaluation.md](docs/submission_evaluation.md) | Full async evaluation pipeline, Judge0 integration, verdict types, scoring |
| [judge_0_integration.md](docs/judge_0_integration.md) | Judge0 CE setup, API usage, language support, sandboxing |
| [system_design.md](docs/system_design.md) | High-level system design decisions and trade-offs |
| [enterprise_documentation.md](docs/enterprise_documentation.md) | Enterprise feature coverage, compliance considerations |

---

## Roadmap

- [ ] Database schema implementation with Flyway migrations
- [ ] Core auth module (register, login, JWT, refresh)
- [ ] Problem and test case CRUD
- [ ] Contest lifecycle management
- [ ] Submission API + RabbitMQ publish
- [ ] Evaluation worker + Judge0 integration
- [ ] Redis leaderboard implementation
- [ ] Plagiarism check pipeline (JPlag)
- [ ] Notification system
- [ ] Audit logging
- [ ] Frontend role-based dashboards
- [ ] Monaco editor integration
- [ ] Submission status polling
- [ ] Admin analytics dashboard
- [ ] MailHog integration for dev email

---

## License

This project is licensed under the [MIT License](LICENSE).

---

<div align="center">

**Built with Java 21 · Spring Boot 3 · React 18 · PostgreSQL · Redis · RabbitMQ · Judge0**

*CodeJudgeX — Engineered for academic institutions. Built for scale.*

</div>
