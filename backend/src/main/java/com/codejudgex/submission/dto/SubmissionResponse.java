package com.codejudgex.submission.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SubmissionResponse {
    private UUID id;
    private UUID studentId;
    private UUID contestId;
    private UUID problemId;
    private int languageId;
    private String status;
    private int score;
    private Integer executionTimeMs;
    private Integer memoryUsedMb;
    private Instant submittedAt;
    private Instant evaluatedAt;
}
