package com.codejudgex.leaderboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LeaderboardEntryResponse {
    private int rank;
    private UUID studentId;
    private String studentName;
    private int totalScore;
    private int solvedCount;
    private Instant lastSubmissionAt;
}
