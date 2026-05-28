-- V4__create_contests.sql
-- Creates contest management tables.
-- Contest status lifecycle: DRAFT -> UPCOMING -> LIVE -> ENDED
-- problem_order in contest_problems allows custom display ordering.

CREATE TABLE contests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_contests_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_contests_status    CHECK (status IN ('DRAFT', 'UPCOMING', 'LIVE', 'ENDED')),
    CONSTRAINT chk_contests_times     CHECK (end_time > start_time)
);

CREATE TABLE contest_problems (
    contest_id     UUID NOT NULL,
    problem_id     UUID NOT NULL,
    problem_order  INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (contest_id, problem_id),
    CONSTRAINT fk_cp_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);

CREATE TABLE contest_participants (
    contest_id    UUID NOT NULL,
    user_id       UUID NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (contest_id, user_id),
    CONSTRAINT fk_cpart_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
    CONSTRAINT fk_cpart_user   FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
);

CREATE INDEX idx_contests_status        ON contests(status);
CREATE INDEX idx_contests_start_time    ON contests(start_time);
CREATE INDEX idx_contests_created_by    ON contests(created_by);
CREATE INDEX idx_cp_contest_id          ON contest_problems(contest_id);
CREATE INDEX idx_cpart_contest_id       ON contest_participants(contest_id);
CREATE INDEX idx_cpart_user_id          ON contest_participants(user_id);
