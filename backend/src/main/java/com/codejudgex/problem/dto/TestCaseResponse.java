package com.codejudgex.problem.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Safe DTO for student-facing responses.
 * Never includes inputData or expectedOutput for hidden (non-sample) test cases.
 * The service enforces this — do not change the mapping without updating ProblemService.
 */
@Getter
@Builder
public class TestCaseResponse {
    private UUID id;
    private String inputData;
    private String expectedOutput;
    private boolean isSample;
    private int weight;
}
