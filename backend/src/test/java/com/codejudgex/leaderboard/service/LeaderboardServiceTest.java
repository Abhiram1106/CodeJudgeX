package com.codejudgex.leaderboard.service;

import com.codejudgex.leaderboard.dto.LeaderboardEntryResponse;
import com.codejudgex.leaderboard.entity.LeaderboardSnapshot;
import com.codejudgex.leaderboard.repository.LeaderboardSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock LeaderboardSnapshotRepository snapshotRepository;
    @Mock ZSetOperations<String, String> zSetOperations;

    @InjectMocks LeaderboardService leaderboardService;

    private final UUID contestId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void updateScore_callsZSetAdd() {
        leaderboardService.updateScore(contestId, studentId, 80);
        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void getTopN_redisHasData_returnsRankedList() {
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn(studentId.toString());
        when(tuple.getScore()).thenReturn(90.0);
        when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of(tuple));

        List<LeaderboardEntryResponse> result = leaderboardService.getTopN(contestId, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalScore()).isEqualTo(90);
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    void getTopN_redisMiss_fallsBackToPostgres() {
        when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(null);

        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setStudentId(studentId);
        snapshot.setTotalScore(70);
        snapshot.setRankPosition(1);

        when(snapshotRepository.findByContestIdOrderByRankPositionAsc(contestId))
                .thenReturn(List.of(snapshot));

        List<LeaderboardEntryResponse> result = leaderboardService.getTopN(contestId, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalScore()).isEqualTo(70);
    }

    @Test
    void getStudentRank_redisHasData_returnsRank() {
        String key = "leaderboard:contest:" + contestId;
        when(zSetOperations.reverseRank(key, studentId.toString())).thenReturn(2L);
        when(zSetOperations.score(key, studentId.toString())).thenReturn(60.0);

        LeaderboardEntryResponse result = leaderboardService.getStudentRank(contestId, studentId);

        assertThat(result.getRank()).isEqualTo(3);
        assertThat(result.getTotalScore()).isEqualTo(60);
    }

    @Test
    void getStudentRank_redisMiss_fallsBackToPostgres() {
        String key = "leaderboard:contest:" + contestId;
        when(zSetOperations.reverseRank(key, studentId.toString())).thenReturn(null);
        when(zSetOperations.score(key, studentId.toString())).thenReturn(null);

        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setStudentId(studentId);
        snapshot.setTotalScore(50);
        snapshot.setRankPosition(5);

        when(snapshotRepository.findByContestIdAndStudentId(contestId, studentId))
                .thenReturn(Optional.of(snapshot));

        LeaderboardEntryResponse result = leaderboardService.getStudentRank(contestId, studentId);

        assertThat(result.getRank()).isEqualTo(5);
        assertThat(result.getTotalScore()).isEqualTo(50);
    }
}
