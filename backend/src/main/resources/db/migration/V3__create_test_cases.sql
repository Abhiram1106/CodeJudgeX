-- V3__create_test_cases.sql
-- Creates test case table for problems.
-- SECURITY: is_sample=false rows MUST NEVER appear in student-facing API responses.
-- Enforcement is done in ProblemService, not at the controller or DB layer.

CREATE TABLE test_cases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id      UUID    NOT NULL,
    input_data      TEXT    NOT NULL,
    expected_output TEXT    NOT NULL,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,
    weight          INT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT chk_test_cases_weight CHECK (weight > 0)
);

CREATE INDEX idx_test_cases_problem_id ON test_cases(problem_id);
CREATE INDEX idx_test_cases_is_sample  ON test_cases(problem_id, is_sample);
