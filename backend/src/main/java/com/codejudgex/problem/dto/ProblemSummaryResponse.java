package com.codejudgex.problem.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProblemSummaryResponse {
    private UUID id;
    private String title;
    private String difficulty;
    private int timeLimitMs;
    private int memoryLimitMb;
    private Set<String> tags;
}
