package com.codejudgex.leaderboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class LeaderboardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "contest_id", nullable = false)
    private UUID contestId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    @Column(name = "solved_count", nullable = false)
    private int solvedCount = 0;

    @Column(name = "last_submission_at")
    private Instant lastSubmissionAt;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private Instant snapshotAt;

    @PrePersist
    protected void onCreate() {
        snapshotAt = Instant.now();
    }
}
