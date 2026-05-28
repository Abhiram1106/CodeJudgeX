package com.codejudgex.leaderboard.service;

import com.codejudgex.common.exception.ResourceNotFoundException;
import com.codejudgex.leaderboard.dto.LeaderboardEntryResponse;
import com.codejudgex.leaderboard.entity.LeaderboardSnapshot;
import com.codejudgex.leaderboard.repository.LeaderboardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Leaderboard service using Redis sorted sets as the live source.
 *
 * Redis key schema:
 *   leaderboard:contest:{contestId}  → ZSET  member=studentId  score=totalScore
 *
 * PostgreSQL is the durable fallback — snapshots written on demand.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final String KEY_PREFIX = "leaderboard:contest:";

    private final RedisTemplate<String, String> redisTemplate;
    private final LeaderboardSnapshotRepository snapshotRepository;

    /**
     * Update (or insert) a student's score in Redis.
     * Called by EvaluationWorker after each successful evaluation.
     */
    public void updateScore(UUID contestId, UUID studentId, int newScore) {
        String key = key(contestId);
        redisTemplate.opsForZSet().add(key, studentId.toString(), newScore);
        log.debug("Leaderboard updated — contest={} student={} score={}", contestId, studentId, newScore);
    }

    /**
     * Returns the top-N ranked entries for a contest.
     * Falls back to the latest PostgreSQL snapshot when Redis has no data.
     */
    public List<LeaderboardEntryResponse> getTopN(UUID contestId, int n) {
        String key = key(contestId);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, n - 1);

        if (tuples != null && !tuples.isEmpty()) {
            return toEntryList(tuples);
        }

        log.debug("Redis miss for leaderboard:contest:{} — falling back to PG snapshot", contestId);
        return snapshotRepository.findByContestIdOrderByRankPositionAsc(contestId)
                .stream()
                .map(s -> LeaderboardEntryResponse.builder()
                        .rank(s.getRankPosition() != null ? s.getRankPosition() : 0)
                        .studentId(s.getStudentId())
                        .totalScore(s.getTotalScore())
                        .solvedCount(s.getSolvedCount())
                        .lastSubmissionAt(s.getLastSubmissionAt())
                        .build())
                .toList();
    }

    /**
     * Returns the authenticated student's own rank and score.
     */
    public LeaderboardEntryResponse getStudentRank(UUID contestId, UUID studentId) {
        String key = key(contestId);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, studentId.toString());
        Double score = redisTemplate.opsForZSet().score(key, studentId.toString());

        if (rank != null && score != null) {
            return LeaderboardEntryResponse.builder()
                    .rank((int) (rank + 1))
                    .studentId(studentId)
                    .totalScore(score.intValue())
                    .build();
        }

        // Fall back to PG snapshot
        return snapshotRepository.findByContestIdAndStudentId(contestId, studentId)
                .map(s -> LeaderboardEntryResponse.builder()
                        .rank(s.getRankPosition() != null ? s.getRankPosition() : 0)
                        .studentId(s.getStudentId())
                        .totalScore(s.getTotalScore())
                        .solvedCount(s.getSolvedCount())
                        .lastSubmissionAt(s.getLastSubmissionAt())
                        .build())
                .orElseThrow(() -> new ResourceNotFoundException("No leaderboard entry found for student " + studentId));
    }

    /**
     * Writes the current Redis state to leaderboard_snapshots in PostgreSQL.
     * Called on contest end or periodically (every 5 minutes).
     */
    @Transactional
    public void snapshotToPostgres(UUID contestId) {
        String key = key(contestId);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            log.warn("Snapshot requested but Redis has no data for contest={}", contestId);
            return;
        }

        int rank = 1;
        List<LeaderboardSnapshot> snapshots = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
            snapshot.setContestId(contestId);
            snapshot.setStudentId(UUID.fromString(tuple.getValue()));
            snapshot.setTotalScore(tuple.getScore() != null ? tuple.getScore().intValue() : 0);
            snapshot.setRankPosition(rank++);
            snapshot.setLastSubmissionAt(Instant.now());
            snapshots.add(snapshot);
        }

        snapshotRepository.saveAll(snapshots);
        log.info("Leaderboard snapshot written — contest={} entries={}", contestId, snapshots.size());
    }

    private static String key(UUID contestId) {
        return KEY_PREFIX + contestId;
    }

    private List<LeaderboardEntryResponse> toEntryList(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .studentId(UUID.fromString(tuple.getValue()))
                    .totalScore(tuple.getScore() != null ? tuple.getScore().intValue() : 0)
                    .build());
        }
        return result;
    }
}
