-- V10__add_verdict_weight_to_submission_results.sql
-- Adds verdict and weight columns to submission_results for the evaluation pipeline.
-- verdict: human-readable verdict string (ACCEPTED, WRONG_ANSWER, etc.)
-- weight:  test case weight used for score calculation (0-100)
-- memory_used_kb: memory consumed by the submission in KB

ALTER TABLE submission_results
    ADD COLUMN IF NOT EXISTS verdict        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS weight         INT         NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS memory_used_kb INT;
