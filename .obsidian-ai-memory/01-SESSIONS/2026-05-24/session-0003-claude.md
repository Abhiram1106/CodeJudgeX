---
type: session-digest
date: 2026-05-24
time: "session-0003"
tool: claude
week-goal: Week 1 — Database Foundation + Backend Core Modules
tags: [session, week1, flyway, auth, problem, contest, submission, build]
---

# Session Digest — 2026-05-24 session 3 (claude)

## Request

"Follow docs/ROADMAP.md to build this application, also include the roadmap in vault"

## Memory retrieved at session start

- session-continuity.md: Week 1 ready to start, next task = V1 migration
- active-goals.md: all Week 1 tasks unchecked
- error-memory.md: empty

## Files created / changed

### Vault
| File | Purpose |
|---|---|
| `.obsidian-ai-memory/05-ARCHITECTURE/ROADMAP-snapshot.md` | Mirror of ROADMAP for offline agent retrieval |

### Database (backend/src/main/resources/db/migration/)
| File | Tables |
|---|---|
| V1__create_users_roles.sql | users, roles, user_roles |
| V2__create_problems.sql | problems, problem_tags, problem_tag_map |
| V3__create_test_cases.sql | test_cases |
| V4__create_contests.sql | contests, contest_problems, contest_participants |
| V5__create_submissions.sql | submissions, submission_results |
| V6__create_leaderboard.sql | leaderboard_snapshots |
| V7__create_plagiarism.sql | plagiarism_jobs, plagiarism_flags |
| V8__create_supporting.sql | notifications, audit_logs, refresh_tokens |
| V9__add_indexes.sql | composite indexes |

### Backend source (backend/src/main/java/com/codejudgex/)
| Package | Files |
|---|---|
| common/dto | ApiResponse, PageResponse |
| common/entity | BaseEntity |
| common/exception | ResourceNotFoundException, DuplicateResourceException, AccessDeniedException, BusinessException, GlobalExceptionHandler |
| infrastructure/config | RabbitMQConfig, RedisConfig, JacksonConfig, SecurityConfig |
| auth/entity | User, Role, RefreshToken |
| auth/repository | UserRepository, RoleRepository, RefreshTokenRepository |
| auth/dto | RegisterRequest, LoginRequest, AuthResponse, RefreshRequest, UserProfileResponse |
| auth/service | JwtService, RefreshTokenService, AuthService |
| auth/filter | JwtAuthenticationFilter |
| auth/controller | AuthController |
| problem/entity | Problem, TestCase, ProblemTag |
| problem/repository | ProblemRepository, TestCaseRepository, ProblemTagRepository |
| problem/dto | CreateProblemRequest, AddTestCaseRequest, ProblemResponse, ProblemSummaryResponse, TestCaseResponse |
| problem/service | ProblemService |
| problem/controller | ProblemController |
| contest/entity | Contest, ContestProblem (EmbeddedId), ContestParticipant (EmbeddedId) |
| contest/repository | ContestRepository, ContestProblemRepository, ContestParticipantRepository |
| contest/dto | CreateContestRequest, ContestResponse, ContestSummaryResponse |
| contest/service | ContestService |
| contest/controller | ContestController |
| submission/entity | Submission, SubmissionResult |
| submission/repository | SubmissionRepository |
| submission/dto | CreateSubmissionRequest, SubmissionResponse, SubmissionStatusResponse, EvaluationMessage |
| submission/service | SubmissionService |
| submission/controller | SubmissionController |
| (root) | CodeJudgeXApplication |

### Backend tests
| File | Tests |
|---|---|
| AuthServiceTest | 6 tests (register success/dup, login success/wrong-pass/inactive/not-found) |
| ProblemServiceTest | 4 tests (create, student gets sample only, not-found, addTestCase) |
| ContestServiceTest | 5 tests (create, end-before-start, not-live, already-registered, not-found) |
| SubmissionServiceTest | 4 tests (submit success+RabbitMQ, contest not LIVE, not-registered, getStatus) |

### Config
| File | Change |
|---|---|
| backend/pom.xml | Added: Flyway, springdoc, jackson-jsr310, mail, testcontainers |
| backend/src/main/resources/application.yml | Extended: Flyway config, Hikari pool, mail, SpringDoc, JWT expiry, Judge0 poll settings |

### Maven wrapper
- Generated `./mvnw` (3.9.6) — was missing from scaffold

## Decisions made

None new — followed existing D-001 through D-006.

Notable implementation choices:
- Skipped MapStruct mappers; used manual builder mapping in all services. Simpler and avoids annotation processor ordering issues at this stage.
- Refresh token reuse detection: when a revoked token is presented, all tokens for that user are revoked (compromise mitigation).
- Hidden test case protection: `ProblemService.getProblemForStudent()` calls `findByProblemIdAndIsSample(id, true)` — hidden cases are never fetched at all.

## Errors encountered

1. `logout` in AuthController passed email string to UUID.fromString → Fixed: changed to `logoutByEmail(email)` method.
2. `convertAndSend` ambiguous overload in SubmissionServiceTest → Fixed: `any(Object.class)`.
3. `User.getId()` null in AuthServiceTest → Fixed: used reflection to set id field in test setUp (JPA doesn't fire on `new`).
4. IDE warning on `springdoc` in application.yml (code 513) → Not a compile error; safe to ignore.

## Tests / verification

- `./mvnw compile` → BUILD SUCCESS
- `./mvnw test` (4 unit test classes) → Tests run: 19, Failures: 0, Errors: 0

## Assumptions made

- MapStruct mappers omitted at Week 1 — manual mapping is clearer and avoids annotation processor ordering conflicts. Can add later.
- `resolveUserId(principal)` in controllers uses `UUID.nameUUIDFromBytes(email.getBytes())` as a temporary approach. Week 2: JWT claims will carry the real `userId` UUID.
- `server.servlet.context-path=/api/v1` — all endpoints are under /api/v1/.

## Open risks

- `resolveUserId()` using name-based UUID is deterministic but non-standard — must replace with JWT claim lookup before any real user can use the system.
- Contest module `addProblem` does not yet verify the requesting faculty owns the contest — any FACULTY can add problems to any DRAFT contest.
- No integration tests yet — module acceptance criteria not fully met until Docker stack runs.

## Next recommended steps

1. **Replace `resolveUserId()`** in all controllers to read userId from JWT claims (not email-hash).
2. **Start Docker stack** with `make dev` and verify Flyway migrations run cleanly against PostgreSQL.
3. **Run Swagger** at http://localhost:8080/api/v1/swagger-ui.html and confirm all endpoints appear.
4. **Begin Week 2**: Judge0Client → EvaluationWorker (ROADMAP Week 2 section).
