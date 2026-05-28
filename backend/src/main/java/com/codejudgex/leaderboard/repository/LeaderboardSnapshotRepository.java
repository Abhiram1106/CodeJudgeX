package com.codejudgex.leaderboard.repository;

import com.codejudgex.leaderboard.entity.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, UUID> {

    List<LeaderboardSnapshot> findByContestIdOrderByRankPositionAsc(UUID contestId);

    Optional<LeaderboardSnapshot> findByContestIdAndStudentId(UUID contestId, UUID studentId);

    @Query("SELECT MAX(s.snapshotAt) FROM LeaderboardSnapshot s WHERE s.contestId = :contestId")
    Optional<java.time.Instant> findLatestSnapshotTime(UUID contestId);
}
