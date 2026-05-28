package com.codejudgex.problem.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProblemResponse {
    private UUID id;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String constraintsText;
    private String difficulty;
    private int timeLimitMs;
    private int memoryLimitMb;
    private Set<String> tags;
    private List<TestCaseResponse> sampleTestCases;
    private UUID createdBy;
    private Instant createdAt;
}
