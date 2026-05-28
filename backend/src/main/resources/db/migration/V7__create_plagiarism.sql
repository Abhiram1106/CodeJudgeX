-- V7__create_plagiarism.sql
-- Creates plagiarism detection job and flag tables.
-- Jobs are triggered by admin after contest ends.
-- Flags are pairs of submissions with high similarity scores.
-- Flag status: PENDING | REVIEWED | DISMISSED

CREATE TABLE plagiarism_jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contest_id   UUID        NOT NULL,
    triggered_by UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at   TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_pj_contest     FOREIGN KEY (contest_id)   REFERENCES contests(id),
    CONSTRAINT fk_pj_triggered   FOREIGN KEY (triggered_by) REFERENCES users(id),
    CONSTRAINT chk_pj_status     CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE plagiarism_flags (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id               UUID           NOT NULL,
    submission_id        UUID           NOT NULL,
    matched_submission_id UUID          NOT NULL,
    similarity_score     DECIMAL(5, 2)  NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_pf_job         FOREIGN KEY (job_id)                REFERENCES plagiarism_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_submission  FOREIGN KEY (submission_id)         REFERENCES submissions(id),
    CONSTRAINT fk_pf_matched     FOREIGN KEY (matched_submission_id) REFERENCES submissions(id),
    CONSTRAINT chk_pf_status     CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    CONSTRAINT chk_pf_score      CHECK (similarity_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_pj_contest_id   ON plagiarism_jobs(contest_id);
CREATE INDEX idx_pf_job_id       ON plagiarism_flags(job_id);
CREATE INDEX idx_pf_submission   ON plagiarism_flags(submission_id);
