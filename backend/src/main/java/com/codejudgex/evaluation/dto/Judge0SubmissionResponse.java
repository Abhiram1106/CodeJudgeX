package com.codejudgex.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response from Judge0 CE GET /submissions/{token}.
 * Only fields relevant to evaluation are mapped; the rest are ignored.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Judge0SubmissionResponse {

    private String token;

    private Judge0Status status;

    private String stdout;

    private String stderr;

    @JsonProperty("compile_output")
    private String compileOutput;

    /** Wall-clock time in seconds (string from Judge0, e.g. "0.123"). */
    private String time;

    /** Memory used in kilobytes. */
    private Integer memory;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Judge0Status {
        private int id;
        private String description;
    }
}
