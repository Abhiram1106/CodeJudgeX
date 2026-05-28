# Session Digest — 2026-05-28

## ✅ What was done this session

### Task 2 — MockMvc controller tests (COMPLETED)
- Fixed `ProblemController.java`: replaced `@AuthenticationPrincipal Principal` with `Authentication authentication`
- Fixed `AuthController.java`: same fix for `/logout` and `/me` endpoints
- Fixed `SecurityConfig.java`: added `authenticationEntryPoint(HttpStatusEntryPoint(UNAUTHORIZED))` and `accessDeniedHandler`
- Fixed `GlobalExceptionHandler.java`: corrected import from `java.nio.file.AccessDeniedException` → `org.springframework.security.access.AccessDeniedException`; added `AuthorizationDeniedException` handler for `@PreAuthorize` role failures mapping to 403
- All 20 MockMvc tests now pass (7 Auth + 4 Contest + 4 Problem + 5 Submission)
- Commit: `test(controllers): add MockMvc tests for Auth, Problem, Contest, Submission controllers`

### Task 3–5 — Evaluation Pipeline (COMPLETED)
- `evaluation/dto/Judge0SubmissionRequest.java` — POST body to Judge0 CE
- `evaluation/dto/Judge0SubmissionResponse.java` — response from Judge0 CE
- `evaluation/Judge0StatusMapper.java` — maps Judge0 status IDs (1–14) to internal verdict strings
- `evaluation/EvaluationException.java` — typed exception for retry/DLQ triggering
- `evaluation/Judge0Client.java` — RestTemplate wrapper: submit + poll with configurable interval/attempts
- `evaluation/OutputComparator.java` — CRLF-normalized, trim-per-line output comparison
- `evaluation/ScoreCalculator.java` — weight-sum score + priority-ordered overall verdict
- `evaluation/EvaluationWorker.java` — `@RabbitListener` consumer: QUEUED→RUNNING→verdict, retry (3x) then DLQ + INTERNAL_ERROR
- `infrastructure/config/Judge0Config.java` — `RestTemplate` bean with timeouts + optional auth token
- `submission/entity/SubmissionResult.java` — added `verdict`, `weight`, `memoryUsedKb` fields
- `submission/repository/SubmissionResultRepository.java` — `findBySubmissionId()`
- `db/migration/V10__add_verdict_weight_to_submission_results.sql` — ALTER TABLE migration

### Task 6 — Leaderboard Module (COMPLETED)
- `leaderboard/entity/LeaderboardSnapshot.java` — JPA entity for PG snapshot
- `leaderboard/repository/LeaderboardSnapshotRepository.java`
- `leaderboard/dto/LeaderboardEntryResponse.java`
- `leaderboard/service/LeaderboardService.java` — Redis ZSET live source, PG fallback, `snapshotToPostgres()`
- `leaderboard/controller/LeaderboardController.java` — GET /leaderboards/contests/{id} + /me

### Task 7 — Notification Module (COMPLETED)
- `notification/entity/Notification.java`
- `notification/dto/NotificationMessage.java` — RabbitMQ POJO
- `notification/dto/NotificationResponse.java`
- `notification/repository/NotificationRepository.java`
- `notification/service/EmailService.java` — JavaMailSender → MailHog, non-fatal on failure
- `notification/service/NotificationWorker.java` — `@RabbitListener` consumer
- `notification/controller/NotificationController.java` — GET /notifications + PATCH /{id}/read

### Tests
- `OutputComparatorTest` — 8 tests
- `ScoreCalculatorTest` — 7 tests
- `LeaderboardServiceTest` — 5 tests
- Full suite: **59/59 passing**, BUILD SUCCESS

### Commits
- `test(controllers): add MockMvc tests...` — 12 files, 1211 insertions
- `feat(evaluation): implement Week 2 async evaluation pipeline + leaderboard + notifications`

## 🔧 What still needs to be done

- [ ] Task 8: Frontend foundation (types, axios, QueryClient, Zustand auth store, 6 service files)
- [ ] Docker stack verification: `make dev` → check Flyway applied 10 migrations
- [ ] Week 3: Security hardening, rate limiting, audit module, plagiarism module
- [ ] EvaluationWorker unit test (needs Mockito mocking of Judge0Client)
- [ ] `git push origin HEAD` for code commits

## 🧪 What to test manually

- `cd backend && ./mvnw test` → expect 59/59
- POST `/api/v1/auth/register` → 201 + tokens
- POST `/api/v1/auth/login` → 200 + tokens
- POST `/api/v1/contests` (FACULTY JWT) → 201 DRAFT contest
- POST `/api/v1/submissions` (STUDENT JWT) → 202 QUEUED
- GET `/api/v1/leaderboards/contests/{id}` → ranked list
- GET `/api/v1/notifications` → in-app notifications list

## ⚠️ Open risks / known issues

- EvaluationWorker requires Docker stack (RabbitMQ + Judge0 CE) to run end-to-end — not yet started
- V10 migration adds NOT NULL DEFAULT columns — safe for existing empty DB, but review before applying to any data
- Frontend is 0% — Task 8 not started yet
- `git push` not yet executed for either code commit this session

## 📋 Decisions made

- `SubmissionResult.verdict` added as separate field from `status` to allow richer verdict tracking (status mirrors verdict for now)
- Email send failure in `EmailService` is non-fatal — logged as ERROR, in-app notification is still persisted
- Redis ZADD always overwrites (no max-guard) — if a student re-submits with lower score, leaderboard may go down; needs best-score logic (compare before ZADD)

## 🚀 Recommended next step

Push code commits (`git push origin HEAD`), then start Task 8 frontend foundation: create `src/types/`, `src/lib/axios.ts`, `src/lib/queryClient.ts`, `src/stores/auth.store.ts`, and the 6 service files per ROADMAP.
