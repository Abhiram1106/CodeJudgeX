package com.codejudgex.contest.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ContestResponse {
    private UUID id;
    private String title;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private UUID createdBy;
    private long participantCount;
    private List<ContestProblemEntry> problems;
    private Instant createdAt;

    @Getter
    @Builder
    public static class ContestProblemEntry {
        private UUID problemId;
        private String title;
        private String difficulty;
        private int problemOrder;
    }
}
