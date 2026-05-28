package com.codejudgex.submission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SubmissionStatusResponse {
    private UUID id;
    private String status;
    private int score;
}
