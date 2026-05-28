package com.codejudgex.problem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTestCaseRequest {

    @NotBlank(message = "Input data is required")
    private String inputData;

    @NotBlank(message = "Expected output is required")
    private String expectedOutput;

    private boolean isSample = false;

    @Min(value = 1, message = "Weight must be at least 1")
    private int weight = 1;
}
