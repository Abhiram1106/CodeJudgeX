# CodeJudgeX Database Design

## 1. Purpose

This document defines the database design for **CodeJudgeX**.

PostgreSQL is used as the primary source of truth for users, roles, contests, problems, test cases, submissions, results, leaderboards, plagiarism flags, notifications, and audit logs.

Redis is used only for fast temporary data such as live leaderboards, rate limiting, submission status cache, and contest statistics.

---

## 2. Database Choice

### Primary Database

```text
PostgreSQL
```

### Why PostgreSQL?

PostgreSQL is chosen because CodeJudgeX has highly relational data:

```text
users
roles
problems
contests
test cases
submissions
submission results
leaderboards
audit logs
```

PostgreSQL gives:

```text
strong consistency
foreign keys
transactions
indexes
constraints
query optimization
JSONB support if needed
reliable relational modeling
```

---

## 3. Migration Tool

Use:

```text
Flyway
```

Migration files should be stored in:

```text
backend/src/main/resources/db/migration
```

Example:

```text
V1__create_users_table.sql
V2__create_problems_table.sql
V3__create_contests_table.sql
```

Rules:

```text
Never manually change production schema without migration.
Migration files must be versioned.
Migration files should be reviewed before merge.
Do not edit already-applied migrations.
Create a new migration for every schema change.
```

---

## 4. Core Entity Relationship Overview

```text
User
 ├── creates Problems
 ├── creates Contests
 ├── submits Submissions
 └── appears in Audit Logs

Contest
 ├── has many Contest Problems
 ├── has many Participants
 ├── has many Submissions
 └── has Leaderboard Entries

Problem
 ├── has many Test Cases
 ├── belongs to many Contests
 └── has many Submissions

Submission
 ├── belongs to User
 ├── belongs to Contest
 ├── belongs to Problem
 └── has many Submission Results
```

---

## 5. Database Naming Conventions

Use snake_case for database names.

Examples:

```text
users
contest_participants
submission_results
created_at
updated_at
source_code_hash
```

Use UUID primary keys for important domain entities.

Recommended ID type:

```text
UUID
```

Timestamp fields:

```text
created_at
updated_at
submitted_at
evaluated_at
```

---

## 6. Tables

## 6.1 users

Stores all users.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    department VARCHAR(120),
    year INT,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Status Values

```text
ACTIVE
INACTIVE
SUSPENDED
DELETED
```

### Indexes

```sql
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
```

---

## 6.2 refresh_tokens

Stores refresh tokens for JWT authentication.

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);
```

### Indexes

```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

---

## 6.3 problems

Stores coding problems.

```sql
CREATE TABLE problems (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    difficulty VARCHAR(40) NOT NULL,
    input_format TEXT,
    output_format TEXT,
    constraints_text TEXT,
    time_limit_ms INT NOT NULL,
    memory_limit_mb INT NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Difficulty Values

```text
EASY
MEDIUM
HARD
```

### Status Values

```text
DRAFT
PUBLISHED
ARCHIVED
```

### Indexes

```sql
CREATE UNIQUE INDEX idx_problems_slug ON problems(slug);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problems_created_by ON problems(created_by);
CREATE INDEX idx_problems_status ON problems(status);
```

---

## 6.4 problem_tags

Stores tags for problems.

```sql
CREATE TABLE problem_tags (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

### Indexes

```sql
CREATE INDEX idx_problem_tags_problem_id ON problem_tags(problem_id);
CREATE INDEX idx_problem_tags_tag ON problem_tags(tag);
```

---

## 6.5 test_cases

Stores sample and hidden test cases.

```sql
CREATE TABLE test_cases (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample BOOLEAN NOT NULL DEFAULT FALSE,
    weight INT NOT NULL DEFAULT 1,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Important Rule

```text
Students must never receive hidden test case data from APIs.
```

### Indexes

```sql
CREATE INDEX idx_test_cases_problem_id ON test_cases(problem_id);
CREATE INDEX idx_test_cases_is_sample ON test_cases(is_sample);
```

---

## 6.6 contests

Stores contests or coding assessments.

```sql
CREATE TABLE contests (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Status Values

```text
DRAFT
UPCOMING
LIVE
ENDED
ARCHIVED
```

### Indexes

```sql
CREATE UNIQUE INDEX idx_contests_slug ON contests(slug);
CREATE INDEX idx_contests_status ON contests(status);
CREATE INDEX idx_contests_start_time ON contests(start_time);
CREATE INDEX idx_contests_end_time ON contests(end_time);
CREATE INDEX idx_contests_created_by ON contests(created_by);
```

---

## 6.7 contest_problems

Maps problems to contests.

```sql
CREATE TABLE contest_problems (
    id UUID PRIMARY KEY,
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    problem_id UUID NOT NULL REFERENCES problems(id),
    points INT NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(contest_id, problem_id)
);
```

### Indexes

```sql
CREATE INDEX idx_contest_problems_contest_id ON contest_problems(contest_id);
CREATE INDEX idx_contest_problems_problem_id ON contest_problems(problem_id);
```

---

## 6.8 contest_participants

Stores users participating in contests.

```sql
CREATE TABLE contest_participants (
    id UUID PRIMARY KEY,
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    joined_at TIMESTAMP NOT NULL,
    status VARCHAR(40) NOT NULL,
    UNIQUE(contest_id, user_id)
);
```

### Status Values

```text
REGISTERED
ACTIVE
DISQUALIFIED
REMOVED
```

### Indexes

```sql
CREATE INDEX idx_contest_participants_contest_id ON contest_participants(contest_id);
CREATE INDEX idx_contest_participants_user_id ON contest_participants(user_id);
```

---

## 6.9 submissions

Stores code submissions.

```sql
CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    contest_id UUID NOT NULL REFERENCES contests(id),
    problem_id UUID NOT NULL REFERENCES problems(id),
    language VARCHAR(50) NOT NULL,
    source_code TEXT NOT NULL,
    source_code_hash VARCHAR(255) NOT NULL,
    status VARCHAR(60) NOT NULL,
    score INT NOT NULL DEFAULT 0,
    execution_time_ms INT,
    memory_used_mb INT,
    attempt_number INT NOT NULL DEFAULT 1,
    submitted_at TIMESTAMP NOT NULL,
    evaluated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Status Values

```text
QUEUED
RUNNING
ACCEPTED
WRONG_ANSWER
PARTIALLY_ACCEPTED
COMPILATION_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
INTERNAL_ERROR
```

### Indexes

```sql
CREATE INDEX idx_submissions_student_id ON submissions(student_id);
CREATE INDEX idx_submissions_contest_id ON submissions(contest_id);
CREATE INDEX idx_submissions_problem_id ON submissions(problem_id);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_submitted_at ON submissions(submitted_at DESC);
CREATE INDEX idx_submissions_contest_student ON submissions(contest_id, student_id);
CREATE INDEX idx_submissions_contest_problem ON submissions(contest_id, problem_id);
CREATE INDEX idx_submissions_source_code_hash ON submissions(source_code_hash);
```

---

## 6.10 submission_results

Stores per-test-case execution result.

```sql
CREATE TABLE submission_results (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL REFERENCES test_cases(id),
    status VARCHAR(60) NOT NULL,
    actual_output TEXT,
    expected_output TEXT,
    execution_time_ms INT,
    memory_used_mb INT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL
);
```

### Indexes

```sql
CREATE INDEX idx_submission_results_submission_id ON submission_results(submission_id);
CREATE INDEX idx_submission_results_test_case_id ON submission_results(test_case_id);
CREATE INDEX idx_submission_results_status ON submission_results(status);
```

---

## 6.11 leaderboard_entries

Stores persistent leaderboard snapshots.

```sql
CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id),
    total_score INT NOT NULL DEFAULT 0,
    solved_count INT NOT NULL DEFAULT 0,
    last_submission_at TIMESTAMP,
    rank_snapshot INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(contest_id, student_id)
);
```

### Indexes

```sql
CREATE INDEX idx_leaderboard_entries_contest_id ON leaderboard_entries(contest_id);
CREATE INDEX idx_leaderboard_entries_student_id ON leaderboard_entries(student_id);
CREATE INDEX idx_leaderboard_entries_score ON leaderboard_entries(contest_id, total_score DESC);
```

---

## 6.12 plagiarism_flags

Stores similarity detection flags.

```sql
CREATE TABLE plagiarism_flags (
    id UUID PRIMARY KEY,
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    submission_id UUID NOT NULL REFERENCES submissions(id),
    matched_submission_id UUID NOT NULL REFERENCES submissions(id),
    similarity_score DECIMAL(5,2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_note TEXT
);
```

### Status Values

```text
OPEN
UNDER_REVIEW
CONFIRMED
DISMISSED
```

### Indexes

```sql
CREATE INDEX idx_plagiarism_flags_contest_id ON plagiarism_flags(contest_id);
CREATE INDEX idx_plagiarism_flags_submission_id ON plagiarism_flags(submission_id);
CREATE INDEX idx_plagiarism_flags_status ON plagiarism_flags(status);
CREATE INDEX idx_plagiarism_flags_similarity ON plagiarism_flags(similarity_score DESC);
```

---

## 6.13 notifications

Stores in-app and email notification records.

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
```

### Status Values

```text
PENDING
SENT
FAILED
READ
```

### Indexes

```sql
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
```

---

## 6.14 audit_logs

Stores accountability records.

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80),
    resource_id UUID,
    ip_address VARCHAR(80),
    user_agent TEXT,
    request_id VARCHAR(120),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL
);
```

### Indexes

```sql
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
```

---

## 7. Important Relationships

```text
users 1 → many problems
users 1 → many contests
users 1 → many submissions
users 1 → many audit_logs

problems 1 → many test_cases
problems many → many contests through contest_problems

contests 1 → many contest_problems
contests 1 → many contest_participants
contests 1 → many submissions
contests 1 → many leaderboard_entries

submissions 1 → many submission_results
submissions 1 → many plagiarism_flags
```

---

## 8. Transaction Boundaries

Use transactions for:

```text
user registration
problem creation with tags
contest creation with problems
submission creation + queue publishing preparation
submission result update
leaderboard snapshot update
plagiarism flag storage
audit log creation
```

Important:

```text
RabbitMQ publishing and DB writes should be designed carefully to avoid lost jobs.
```

Recommended later improvement:

```text
Transactional outbox pattern
```

---

## 9. Submission Immutability

Once a submission is created, the source code should not be edited.

Allowed updates:

```text
status
score
execution_time_ms
memory_used_mb
evaluated_at
updated_at
```

Not allowed:

```text
changing submitted source code
changing student_id
changing contest_id
changing problem_id
```

This protects result integrity.

---

## 10. Hidden Test Case Protection

Hidden test cases must never be returned to student APIs.

Student-facing APIs should only return:

```text
sample input
sample output
problem statement
constraints
```

Faculty/admin APIs may access hidden tests based on permissions.

---

## 11. Source Code Hashing

Every submission should store a source code hash.

Used for:

```text
duplicate detection
similarity pre-check
submission integrity
plagiarism analysis optimization
```

Example field:

```text
source_code_hash
```

Hashing approach:

```text
SHA-256(normalized_source_code)
```

---

## 12. Pagination Strategy

Paginate all list APIs.

Required for:

```text
users
problems
contests
submissions
audit_logs
notifications
plagiarism_flags
```

Default:

```text
page=0
size=20
```

Maximum:

```text
size=100
```

---

## 13. Query Optimization Rules

Rules:

```text
Never load all submissions for a contest without pagination.
Always index high-filter columns.
Avoid N+1 queries.
Use fetch joins only when necessary.
Use projections for dashboard queries.
Avoid returning source_code in list APIs.
Avoid returning hidden test cases in public APIs.
```

---

## 14. Recommended Indexes Summary

High-priority indexes:

```text
users(email)
problems(slug)
contests(slug)
submissions(contest_id, student_id)
submissions(contest_id, problem_id)
submissions(status)
submissions(submitted_at)
leaderboard_entries(contest_id, total_score)
audit_logs(created_at)
plagiarism_flags(contest_id, status)
notifications(user_id, status)
```

---

## 15. Data Retention Strategy

Initial version:

```text
Keep all data permanently.
```

Future version:

```text
Archive old contest submissions.
Archive old audit logs.
Compress large submission outputs.
Delete expired refresh tokens.
Clean old notifications.
```

---

## 16. Backup Strategy

For local/self-hosted setup:

```text
PostgreSQL dump
Docker volume backup
Scheduled manual backup script
```

Example:

```bash
pg_dump -U codejudgex -d codejudgex_db > backup.sql
```

---

## 17. Redis Data Model Reference

Redis keys are documented separately in `redis-usage.md`.

High-level usage:

```text
leaderboard:contest:{contestId}
submission_status:{submissionId}
rate_limit:submission:user:{userId}:contest:{contestId}
contest_stats:{contestId}
jwt_blacklist:{tokenId}
```

---

## 18. Future Database Improvements

Future improvements:

```text
partition submissions by contest or time
archive old submission_results
add materialized views for analytics
add read replicas if needed
add full-text search for problems
add JSONB metadata for flexible analytics
use transactional outbox for reliable messaging
```

---

## 19. Database Success Criteria

Database design is successful if:

```text
relationships are clear
hidden test cases are protected
submission history is immutable
leaderboards can be rebuilt from PostgreSQL
audit logs track important actions
queries remain paginated and indexed
schema changes are handled through Flyway
Redis is not treated as source of truth
```

---

## 20. Final Summary

The CodeJudgeX database is designed around PostgreSQL as the durable source of truth and Redis as the fast-access layer.

PostgreSQL handles correctness, relationships, auditability, and persistence.

Redis handles speed for leaderboard, rate limiting, and temporary state.

This design gives CodeJudgeX a strong foundation for correctness, scalability, and future growth.

