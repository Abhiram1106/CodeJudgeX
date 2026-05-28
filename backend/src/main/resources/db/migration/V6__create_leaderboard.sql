-- V6__create_leaderboard.sql
-- PostgreSQL snapshot of leaderboard state.
-- Redis sorted sets are the live source; this table is the durable fallback.
-- Snapshots written every 5 minutes and on contest end.

CREATE TABLE leaderboard_snapshots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contest_id       UUID      NOT NULL,
    student_id       UUID      NOT NULL,
    total_score      INT       NOT NULL DEFAULT 0,
    solved_count     INT       NOT NULL DEFAULT 0,
    last_submission_at TIMESTAMP WITH TIME ZONE,
    rank_position    INT,
    snapshot_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_lb_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
    CONSTRAINT fk_lb_student FOREIGN KEY (student_id) REFERENCES users(id)    ON DELETE CASCADE
);

CREATE INDEX idx_leaderboard_contest_id ON leaderboard_snapshots(contest_id);
CREATE INDEX idx_leaderboard_student_id ON leaderboard_snapshots(student_id);
CREATE INDEX idx_leaderboard_contest_rank ON leaderboard_snapshots(contest_id, rank_position);
