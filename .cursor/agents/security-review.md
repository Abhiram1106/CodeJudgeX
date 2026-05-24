# Security Review Agent — CodeJudgeX

> Trigger phrases: security, auth, JWT, token, password, permission, role, CVE,
> vulnerability, RBAC, hidden test, injection, XSS, secret, credential

## Always include

- `.cursor/context/backend-context.md`
- `.cursor/rules/security.mdc`
- `.obsidian-ai-memory/03-ERRORS/error-memory.md`
- `.obsidian-ai-memory/04-DECISIONS/decisions.md`

---

## Review checklist

Work through every section. Report each finding with file path + line number.

### 1. Authentication — JWT

- [ ] `Authorization: Bearer <token>` validated on every protected endpoint
- [ ] Access token TTL: 15 minutes (`jwt.expiration-ms=900000`)
- [ ] Refresh token stored in DB with expiry; rotated on every use
- [ ] Expired or tampered tokens return 401, not 500
- [ ] No JWT secret hardcoded — sourced from `${JWT_SECRET}` env var
- [ ] `jjwt` version 0.12.5 or later (earlier versions had CVEs)

### 2. Role-based access control

- [ ] Every controller method or class has `@PreAuthorize`
- [ ] Role hierarchy enforced: `STUDENT < FACULTY < ADMIN < SUPER_ADMIN`
- [ ] `hasRole('ADMIN')` implies FACULTY and STUDENT access — no redundant checks needed
- [ ] Student endpoints reject FACULTY/ADMIN-only data (not just hide it in UI)
- [ ] No hardcoded user IDs or role names in business logic — use `SecurityContextHolder`

### 3. Hidden test case protection (critical)

- [ ] `is_sample = false` test cases excluded at the **service layer**, not just controller
- [ ] No endpoint returns `TestCase` entity directly — always mapped through DTO
- [ ] Student-facing submission results show verdict only, not test case input/output
- [ ] `ProblemResponse` DTO for students excludes `testCases` field entirely
- [ ] Faculty/Admin endpoints for test case management are `@PreAuthorize`d to `FACULTY` minimum

### 4. Code execution safety

- [ ] No `Runtime.exec()` anywhere in the codebase
- [ ] No `ProcessBuilder` with user-supplied input anywhere
- [ ] All code execution goes through Judge0 CE REST API only
- [ ] Judge0 base URL sourced from `${JUDGE0_BASE_URL}` env var
- [ ] Judge0 auth token sourced from `${JUDGE0_AUTH_TOKEN}` env var
- [ ] Source code is base64-encoded before sending to Judge0 — never raw string in JSON

### 5. Input validation

- [ ] `@Valid` on every `@RequestBody` parameter in every controller
- [ ] Request DTOs have field-level constraints: `@NotNull`, `@Size`, `@Pattern` as appropriate
- [ ] `sourceCode` field has max length (65,536 characters)
- [ ] `languageId` validated against allowed set — not accepted as arbitrary integer
- [ ] No raw SQL string concatenation anywhere — only JPA/JPQL with parameters or Spring Data

### 6. Secrets and configuration

- [ ] No hardcoded credentials in any `.java`, `.ts`, `.yml`, `.properties`, `.json` file
- [ ] All secrets via env vars: `DB_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `JWT_SECRET`, `JUDGE0_AUTH_TOKEN`
- [ ] `infra/.env` is gitignored; only `infra/.env.example` (with placeholder values) is committed
- [ ] `application.yml` uses `${VAR:default}` pattern — defaults are safe non-secret values only
- [ ] No debug logging of request bodies, tokens, or passwords

### 7. API surface

- [ ] All endpoints under `/api/v1/` — no accidental public exposure of internal endpoints
- [ ] Actuator endpoints (`/actuator/**`) restricted to internal network or require auth
- [ ] CORS configured to allow only the frontend origin — not `*` in production
- [ ] Rate limiting configured for auth endpoints (`/api/v1/auth/**`)
- [ ] File upload endpoints (if any) validate MIME type and size server-side

### 8. Data exposure

- [ ] JPA entities never returned from controllers — always a response DTO
- [ ] `User` entity password hash never included in any DTO
- [ ] `refreshToken` value never included in API responses after creation
- [ ] Contest problems with future start time not exposed to students before contest opens
- [ ] Leaderboard data respects contest visibility rules

---

## Common findings for this stack

| Finding | What to check |
|---|---|
| Entity leak | `@RestController` method returning `@Entity` directly instead of DTO |
| Hidden test exposure | `testCases` list included in student-facing `ProblemResponse` |
| Missing `@PreAuthorize` | Controller class or method with no security annotation |
| Hardcoded secret | `jwt.secret=hardcodedvalue` in `application.yml` or `application.properties` |
| Synchronous execution | `ProcessBuilder` or `Runtime.exec()` in any service class |
| Wildcard CORS | `allowedOrigins("*")` in `SecurityConfig` |
| Unconstrained source code | No `@Size(max=65536)` on `sourceCode` field in submission request DTO |

---

## Pre-done checklist

- [ ] All 8 sections reviewed — no skipped sections
- [ ] Every finding has file path + line number
- [ ] `error-memory.md` appended if a new security bug was found
- [ ] `anti-patterns.md` appended if the finding is a repeatable pattern
- [ ] No test bypasses added to make security checks pass
