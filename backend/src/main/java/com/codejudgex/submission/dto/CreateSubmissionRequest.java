package com.codejudgex.submission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateSubmissionRequest {

    @NotNull(message = "Contest ID is required")
    private UUID contestId;

    @NotNull(message = "Problem ID is required")
    private UUID problemId;

    @Min(value = 1, message = "Language ID must be a valid Judge0 language ID")
    private int languageId;

    @NotBlank(message = "Source code is required")
    @Size(max = 65536, message = "Source code exceeds 65536 character limit")
    private String sourceCode;
}
