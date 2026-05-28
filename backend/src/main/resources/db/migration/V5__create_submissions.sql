-- V5__create_submissions.sql
-- Creates submission and per-test-case result tables.
-- source_code_hash (SHA-256) used for plagiarism matching without storing code twice.
-- Status lifecycle: QUEUED -> RUNNING -> <terminal>
-- Terminal statuses: ACCEPTED, WRONG_ANSWER, PARTIALLY_ACCEPTED,
--   COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED,
--   MEMORY_LIMIT_EXCEEDED, INTERNAL_ERROR

CREATE TABLE submissions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID         NOT NULL,
    contest_id         UUID         NOT NULL,
    problem_id         UUID         NOT NULL,
    language_id        INT          NOT NULL,
    source_code        TEXT         NOT NULL,
    source_code_hash   VARCHAR(64)  NOT NULL,
    status             VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    score              INT          NOT NULL DEFAULT 0,
    execution_time_ms  INT,
    memory_used_mb     INT,
    submitted_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    evaluated_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_submissions_student   FOREIGN KEY (student_id)  REFERENCES users(id),
    CONSTRAINT fk_submissions_contest   FOREIGN KEY (contest_id)  REFERENCES contests(id),
    CONSTRAINT fk_submissions_problem   FOREIGN KEY (problem_id)  REFERENCES problems(id),
    CONSTRAINT chk_submissions_status   CHECK (status IN (
        'QUEUED', 'RUNNING', 'ACCEPTED', 'WRONG_ANSWER', 'PARTIALLY_ACCEPTED',
        'COMPILATION_ERROR', 'RUNTIME_ERROR', 'TIME_LIMIT_EXCEEDED',
        'MEMORY_LIMIT_EXCEEDED', 'INTERNAL_ERROR'
    ))
);

CREATE TABLE submission_results (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id     UUID        NOT NULL,
    test_case_id      UUID        NOT NULL,
    status            VARCHAR(30) NOT NULL,
    actual_output     TEXT,
    execution_time_ms INT,
    error_message     TEXT,
    CONSTRAINT fk_sr_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_test_case  FOREIGN KEY (test_case_id)  REFERENCES test_cases(id)
);

CREATE INDEX idx_submissions_student_id   ON submissions(student_id);
CREATE INDEX idx_submissions_contest_id   ON submissions(contest_id);
CREATE INDEX idx_submissions_problem_id   ON submissions(problem_id);
CREATE INDEX idx_submissions_status       ON submissions(status);
CREATE INDEX idx_submissions_submitted_at ON submissions(submitted_at);
CREATE INDEX idx_sr_submission_id         ON submission_results(submission_id);
