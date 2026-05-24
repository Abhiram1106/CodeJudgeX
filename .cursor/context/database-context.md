# Database Context — CodeJudgeX

> @-include this file when working on schema, migrations, or repository layer.

## Technology

- **Primary DB:** PostgreSQL 16 (source of truth for everything)
- **Cache / Live leaderboard:** Redis 7 (fast, ephemeral — falls back to PostgreSQL if unavailable)
- **Migrations:** Flyway (versioned SQL files in `backend/src/main/resources/db/migration/`)

## Core tables (planned schema)

```sql
users              id, name, email, password_hash, department, year, status, created_at
roles              id, name (STUDENT|FACULTY|ADMIN|SUPER_ADMIN)
user_roles         user_id FK, role_id FK

problems           id, title, description, difficulty, input_format, output_format,
                   constraints_text, time_limit_ms, memory_limit_mb, status, created_by FK, created_at
problem_tags       problem_id FK, tag

test_cases         id, problem_id FK, input_data, expected_output, is_sample,
                   weight, created_at
                   -- NEVER expose hidden test cases (is_sample=false) to students

contests           id, title, description, start_time, end_time, status, created_by FK, created_at
                   -- status: DRAFT|UPCOMING|LIVE|ENDED|ARCHIVED

contest_problems   id, contest_id FK, problem_id FK, points, display_order
contest_participants id, contest_id FK, user_id FK, joined_at

submissions        id, contest_id FK, problem_id FK, user_id FK, language,
                   source_code, status, score, submitted_at
                   -- status: QUEUED|RUNNING|ACCEPTED|WRONG_ANSWER|TIME_LIMIT_EXCEEDED|
                   --         MEMORY_LIMIT_EXCEEDED|RUNTIME_ERROR|COMPILATION_ERROR|EVALUATION_ERROR

submission_results id, submission_id FK, test_case_id FK, actual_output,
                   status, execution_time_ms, memory_used_mb

leaderboard_snapshots id, contest_id FK, user_id FK, total_score, solved_count,
                      rank, snapshot_at

plagiarism_jobs    id, contest_id FK, status, triggered_by FK, created_at
plagiarism_flags   id, job_id FK, submission_a_id FK, submission_b_id FK,
                   similarity_score, status (PENDING|CONFIRMED|DISMISSED), review_note, reviewed_by FK

notifications      id, user_id FK, type, title, body, is_read, created_at
refresh_tokens     id, user_id FK, token_hash, expires_at, revoked
audit_logs         id, actor_id FK, action, entity_type, entity_id, metadata JSONB, created_at
```

## Indexing conventions

Always add indexes for:
- All foreign key columns
- `status` columns that are filtered in list queries
- `created_at` for time-range queries
- `contest_id + user_id` composite on `submissions`
- `email` on `users` (unique)

## Redis data structures

```
contest:{contestId}:leaderboard   → SORTED SET (score → userId, for ZREVRANGE)
submission:{submissionId}:status  → STRING with TTL 10m (live status cache during eval)
rate_limit:submission:{userId}    → STRING counter with TTL (submission rate limiting)
rate_limit:login:{ip}             → STRING counter with TTL (login rate limiting)
```

## Flyway naming

```
V1__create_users_and_roles.sql
V2__create_problems_and_test_cases.sql
V3__create_contests.sql
V4__create_submissions.sql
V5__create_leaderboard_snapshots.sql
V6__create_plagiarism_tables.sql
V7__create_notifications.sql
V8__create_audit_logs.sql
V9__create_refresh_tokens.sql
```

Never modify a previously applied migration — always create a new version.

## Consistency model

| Operation | Type | Reason |
|---|---|---|
| User / problem / contest creation | Strong (sync PostgreSQL write) | Must be immediately visible |
| Submission creation | Strong | QUEUED record must exist before RabbitMQ publish |
| Evaluation result | Eventual (async worker) | Async pipeline |
| Redis leaderboard update | Eventual | After evaluation completes |
| Plagiarism report | Eventual | Post-contest background job |
