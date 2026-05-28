package com.codejudgex.problem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateProblemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String inputFormat;
    private String outputFormat;
    private String constraintsText;

    @NotNull
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "Difficulty must be EASY, MEDIUM, or HARD")
    private String difficulty = "MEDIUM";

    @Min(value = 100, message = "Time limit must be at least 100ms")
    @Max(value = 10000, message = "Time limit cannot exceed 10000ms")
    private int timeLimitMs = 2000;

    @Min(value = 16, message = "Memory limit must be at least 16MB")
    @Max(value = 512, message = "Memory limit cannot exceed 512MB")
    private int memoryLimitMb = 256;

    private List<String> tags;
}
