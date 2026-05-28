-- V2__create_problems.sql
-- Creates problem library tables.
-- Difficulty: EASY | MEDIUM | HARD
-- Tags are normalised into a separate table to allow multi-tag problems.

CREATE TABLE problems (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    input_format     TEXT,
    output_format    TEXT,
    constraints_text TEXT,
    difficulty       VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    time_limit_ms    INT          NOT NULL DEFAULT 2000,
    memory_limit_mb  INT          NOT NULL DEFAULT 256,
    created_by       UUID         NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_problems_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_problems_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_problems_time_limit  CHECK (time_limit_ms > 0),
    CONSTRAINT chk_problems_memory      CHECK (memory_limit_mb > 0)
);

CREATE TABLE problem_tags (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE problem_tag_map (
    problem_id UUID NOT NULL,
    tag_id     UUID NOT NULL,
    PRIMARY KEY (problem_id, tag_id),
    CONSTRAINT fk_ptm_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT fk_ptm_tag    FOREIGN KEY (tag_id)    REFERENCES problem_tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_problems_created_by  ON problems(created_by);
CREATE INDEX idx_problems_difficulty  ON problems(difficulty);
CREATE INDEX idx_ptm_problem_id       ON problem_tag_map(problem_id);
