---
type: session-continuity
updated: 2026-05-24
tool: claude-code
tags: [continuity, handoff]
---

# Session Continuity — CodeJudgeX

> This file is OVERWRITTEN at the end of every session.
> It is the FIRST file the next session reads — before anything else.

---

## Where we left off

**Date:** 2026-05-24
**Sessions completed today:** 3 (session-0003 is the latest)

### What was built this session (2026-05-24 — session 3)

Completed full **Week 1** implementation per ROADMAP.md:

**Flyway Migrations (V1–V9)** — all 9 written in `backend/src/main/resources/db/migration/`
- V1: users, roles, user_roles
- V2: problems, problem_tags, problem_tag_map
- V3: test_cases (is_sample, weight)
- V4: contests, contest_problems, contest_participants
- V5: submissions, submission_results
- V6: leaderboard_snapshots
- V7: plagiarism_jobs, plagiarism_flags
- V8: notifications, audit_logs, refresh_tokens
- V9: composite indexes

**Backend source** — `backend/src/main/java/com/codejudgex/`
- `common/` — ApiResponse, PageResponse, BaseEntity, GlobalExceptionHandler + 4 typed exceptions
- `infrastructure/config/` — RabbitMQConfig, RedisConfig, SecurityConfig, JacksonConfig
- `auth/` — Full module: entities → repos → DTOs → JwtService → RefreshTokenService → AuthService → JwtAuthenticationFilter → SecurityConfig → AuthController
- `problem/` — Full module with hidden test case protection in ProblemService
- `contest/` — Full module with EmbeddedId for junction tables
- `submission/` — Full module: async submit (202) → RabbitMQ publish → EvaluationMessage POJO

**Tests** — 19/19 passing (AuthServiceTest×6, ProblemServiceTest×4, ContestServiceTest×5, SubmissionServiceTest×4)

**Verification:**
- `./mvnw compile` → BUILD SUCCESS
- `./mvnw test` → Tests run: 19, Failures: 0, Errors: 0

---

## Active thread

**Week 1 complete.** All modules compiled and tested.

**Known open risk (MUST FIX before real use):**
`resolveUserId()` in all controllers uses `UUID.nameUUIDFromBytes(email.getBytes())` — deterministic email hash, not real UUID from DB. Must be replaced with JWT claim extraction (`userId` claim) before any real user can use the system. This is noted in `04-DECISIONS/decisions.md` under D-007.

---

## Current week goal

**Week 1 (2026-05-25 → 2026-05-31):** ✅ COMPLETE

**Week 2 (2026-06-01 → 2026-06-07):** Async Evaluation Pipeline + Leaderboard
- But before Week 2: fix `resolveUserId()` in ProblemController, ContestController, SubmissionController

---

## Verification state

- `./mvnw compile` → BUILD SUCCESS ✓
- `./mvnw test` → 19/19 ✓
- Docker stack NOT yet started — Flyway migrations not yet verified against live PostgreSQL
- Swagger UI not yet verified — needs Docker stack

---

## Next 3 concrete tasks

1. **Fix `resolveUserId()`** in ProblemController, ContestController, SubmissionController
   — Extract `userId` UUID from JWT claims instead of email hash
   — Add `userId` claim to token in JwtService.generateAccessToken()

2. **Start Docker stack** (`make dev`) and verify Flyway migrations run against PostgreSQL
   — Check `docker compose logs backend` for Flyway output

3. **Begin Week 2 per ROADMAP:** Judge0Client → EvaluationWorker → OutputComparator → ScoreCalculator

---

## Open risks

- `resolveUserId()` email-hash UUID is non-standard — must fix before Week 2 (see above)
- Contest `addProblem` does not verify the requesting faculty owns the contest — any FACULTY can add to any DRAFT
- No integration tests yet — Docker stack required
- Judge0 CE requires Docker privileged mode on Windows — test in Week 2
- Redis `maxmemory-policy` must be `noeviction` for leaderboard correctness (Week 2)

---

## Decisions made this session

- Skipped MapStruct mappers at Week 1 — manual builder mapping in all services (avoids annotation processor ordering issues)
- Refresh token reuse detection: presenting a revoked token revokes ALL tokens for that user
- Hidden test case protection: enforced at service layer via separate `findByProblemIdAndIsSample(id, true)` query — hidden rows never fetched for students
