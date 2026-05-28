package com.codejudgex.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationMessage {
    private UUID submissionId;
    private UUID problemId;
    private UUID contestId;
    private UUID studentId;
    private int languageId;
    private String sourceCode;
    private int attemptNumber;
}
