# Omnix Agents — CodeJudgeX

> Omnix agents are specialized roles activated by trigger phrases.
> Each agent role loads a specific mindset, checklist, and verification criteria.

---

## Agent roles and triggers

### debugger
**Triggers:** error, broken, crash, failing, exception, NPE, stack trace, 500, 404, not working

**Activates:**
- Load `03-ERRORS/error-memory.md` first — never diagnose something already solved
- Load `03-ERRORS/anti-patterns.md` — never repeat known bad patterns
- Root-cause tracing mode (symptoms ≠ causes — trace to origin)
- Regression test requirement before closing

**CodeJudgeX specifics:**
- Evaluation errors often surface at HTTP layer but originate in RabbitMQ consumer or Judge0 client — trace across async boundary
- Auth errors may be CORS, missing JWT, expired token, or wrong role — check all before diagnosing

---

### backend
**Triggers:** implement, add, create + any of: service, controller, repository, entity, DTO, endpoint, API, queue, migration

**Activates:**
- Load `02-PROJECTS/project-context.md` and `02-PROJECTS/active-goals.md`
- Layer discipline: entity → migration → repo → DTO → mapper → service → controller
- `@Transactional` on service, `@PreAuthorize` on controller, `@Valid` on request body
- Judge0 CE for execution — never Runtime.exec() or ProcessBuilder on user input
- Async evaluation via RabbitMQ — never synchronous code execution
- Hidden test cases never in student-facing responses

---

### frontend
**Triggers:** implement, add, create + any of: component, page, hook, form, UI, screen, view, store

**Activates:**
- Load `02-PROJECTS/project-context.md` for stack confirmation (Vite, not Next.js)
- Vite + React Router — no SSR, no server components
- TanStack Query for all data fetching — no useEffect for API calls
- Submission polling pattern with TERMINAL_STATUSES stop condition
- Role guard (`RequireRole`) at route level, not just UI conditional rendering

---

### architect
**Triggers:** design, architecture, refactor, restructure, modular, dependency, boundary

**Activates:**
- Load `04-DECISIONS/decisions.md` — check existing decisions before proposing changes
- Load `05-ARCHITECTURE/system-overview.md` — current component map
- Modular monolith boundary discipline (D-001) — no microservice drift
- Record any new architectural decision to `decisions.md` before implementing

---

### reviewer
**Triggers:** review, audit, check, quality (often paired with another role)

**Activates:**
- Code style rules from `.claude/rules/code-style.md` and `.claude/rules/frontend/react.md`
- Security checklist: secrets, SQL injection, auth bypass, hidden test case exposure
- No magic numbers, no commented-out code, no `any` in TypeScript
- No JPA entities in API responses (always map through DTOs)

---

### security
**Triggers:** security, auth, JWT, token, password, permission, role, CVE, vulnerability, RBAC

**Activates:**
- JWT: 15-minute access token, refresh token rotation, BCrypt strength 12
- Role hierarchy enforcement: STUDENT < FACULTY < ADMIN < SUPER_ADMIN
- Hidden test cases (`is_sample = false`) never returned in student-facing endpoints
- No secrets in any file — all via environment variables
- Spring Boot must never exec user code — Judge0 CE handles all execution

---

### database
**Triggers:** migration, schema, flyway, table, column, index, query, SQL

**Activates:**
- Flyway migration naming: `V{n}__{description}.sql` — never modify existing migrations
- No DML mixed with DDL in same migration file
- UUID PKs, TIMESTAMP WITH TIME ZONE timestamps, explicit FK constraint names
- `ddl-auto=validate` always — Hibernate never creates/drops tables
- Co-update entity + repository + mapper when schema changes

---

## Routing matrix

| Request type | Primary role | Secondary roles |
|---|---|---|
| Bug / error investigation | debugger | security |
| New backend feature | backend | architect + reviewer |
| New frontend feature | frontend | reviewer |
| API design | architect | backend + reviewer |
| Database schema change | database | backend |
| Auth / permissions | security | backend |
| Code quality audit | reviewer | security |
| Architecture decision | architect | reviewer |
| Full-stack feature | backend + frontend | architect + reviewer |
