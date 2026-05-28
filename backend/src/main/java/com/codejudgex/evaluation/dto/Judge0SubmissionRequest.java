package com.codejudgex.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Request body for POST /submissions to Judge0 CE.
 * All text fields are base64-encoded per Judge0 API contract.
 */
@Getter
@Builder
public class Judge0SubmissionRequest {

    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("stdin")
    private String stdin;

    @JsonProperty("expected_output")
    private String expectedOutput;

    @JsonProperty("cpu_time_limit")
    private double cpuTimeLimit;

    @JsonProperty("memory_limit")
    private int memoryLimit;
}
