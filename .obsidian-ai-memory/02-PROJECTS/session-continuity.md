---
type: session-continuity
updated: 2026-05-28
tool: claude-code
tags: [continuity, handoff]
---

# Session Continuity — CodeJudgeX

**Last updated:** 2026-05-28 by claude-sonnet-4-6
**Branch:** main
**Full suite:** 59/59 ✅

---

## Current State

### What's complete (backend)
- **Week 1:** Auth, Problem, TestCase, Contest, Submission modules — entities, repos, services, controllers
- **MockMvc tests:** AuthControllerTest (7), ContestControllerTest (4), ProblemControllerTest (4), SubmissionControllerTest (5) — all passing
- **Security fixes:** `authenticationEntryPoint` (401), `accessDeniedHandler` (403), `AuthorizationDeniedException` handler in GlobalExceptionHandler
- **JWT fix:** `userId` UUID stored as principal name; email stored as credentials on UsernamePasswordAuthenticationToken
- **Week 2 evaluation pipeline:**
  - `Judge0Client` → `EvaluationWorker` → `OutputComparator` → `ScoreCalculator`
  - `EvaluationWorker` updates Redis leaderboard after evaluation
  - Retry (3x) → DLQ + INTERNAL_ERROR on failure
- **Leaderboard module:** Redis ZSET live + PG snapshot fallback
- **Notification module:** in-app + MailHog email via RabbitMQ consumer
- **Flyway migrations:** V1–V10 (V10 adds verdict/weight/memoryUsedKb to submission_results)

### What's NOT done
- Frontend: 0% — not started
- Docker stack: not verified running
- EvaluationWorker unit test (needs Judge0Client mock)
- Week 3: rate limiting, audit module, plagiarism module
- Code push to remote

---

## Immediate Next Steps (in order)

1. `git push origin HEAD` — push code commits
2. Task 8: Frontend foundation
   - `frontend/src/types/` — SubmissionStatus, UserRole, API types
   - `frontend/src/lib/axios.ts` — apiClient with JWT interceptor
   - `frontend/src/lib/queryClient.ts` — TanStack Query client config
   - `frontend/src/stores/auth.store.ts` — Zustand auth store
   - `frontend/src/services/` — auth, problem, contest, submission, leaderboard, notification services
3. Docker stack: `make dev` or `docker compose -f infra/docker-compose.yml up -d`
4. Week 3: rate limiting (Redis), Audit module (AOP + append-only log)

---

## Known Issues / Bugs to Avoid

- `@AuthenticationPrincipal Principal` returns null with `@WithMockUser` — always use `Authentication authentication` parameter directly
- `AuthorizationDeniedException` from `@PreAuthorize` bypasses security filter — must be handled in `GlobalExceptionHandler`, not just `accessDeniedHandler`
- IDE (NetBeans/VSCode Java LS) shows false Lombok errors — Maven compile is the only ground truth
- Redis ZADD always overwrites — if student re-submits with lower score, leaderboard may decrease; add best-score guard before ZADD

---

## Key File Paths

| What | Path |
|------|------|
| Security config | `backend/src/main/java/com/codejudgex/infrastructure/config/SecurityConfig.java` |
| Global exception handler | `backend/src/main/java/com/codejudgex/common/exception/GlobalExceptionHandler.java` |
| JWT filter | `backend/src/main/java/com/codejudgex/auth/filter/JwtAuthenticationFilter.java` |
| Evaluation worker | `backend/src/main/java/com/codejudgex/evaluation/EvaluationWorker.java` |
| Judge0 client | `backend/src/main/java/com/codejudgex/evaluation/Judge0Client.java` |
| Leaderboard service | `backend/src/main/java/com/codejudgex/leaderboard/service/LeaderboardService.java` |
| Notification worker | `backend/src/main/java/com/codejudgex/notification/service/NotificationWorker.java` |
| Flyway migrations | `backend/src/main/resources/db/migration/` (V1–V10) |
