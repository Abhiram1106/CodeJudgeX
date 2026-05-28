package com.codejudgex.contest.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContestSummaryResponse {
    private UUID id;
    private String title;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private long participantCount;
}
