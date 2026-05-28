-- V9__add_indexes.sql
-- Composite indexes for common query patterns identified from application layer.
-- All single-column indexes are already in V1–V8.

-- Leaderboard query: rank all students in a contest ordered by score desc, then submission time asc
CREATE INDEX idx_submissions_contest_student
    ON submissions(contest_id, student_id);

-- Problem stats query: count verdicts per problem in a contest
CREATE INDEX idx_submissions_contest_problem
    ON submissions(contest_id, problem_id);

-- Student submission history in a contest (most common frontend query)
CREATE INDEX idx_submissions_student_contest_time
    ON submissions(student_id, contest_id, submitted_at DESC);

-- Audit log resource history (ADMIN view)
CREATE INDEX idx_audit_logs_resource_time
    ON audit_logs(resource_type, resource_id, created_at DESC);

-- Active contest lookup (scheduler, contest status transitions)
CREATE INDEX idx_contests_status_times
    ON contests(status, start_time, end_time);
