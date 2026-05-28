package com.codejudgex.submission.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.submission.dto.CreateSubmissionRequest;
import com.codejudgex.submission.dto.SubmissionResponse;
import com.codejudgex.submission.dto.SubmissionStatusResponse;
import com.codejudgex.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Code submission and status tracking")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit code — returns 202 immediately, evaluation is async")
    public ApiResponse<SubmissionResponse> submit(
            @Valid @RequestBody CreateSubmissionRequest request,
            Authentication authentication) {
        return ApiResponse.success(submissionService.submit(request, resolveUserId(authentication)));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current status of a submission (poll until terminal)")
    public ApiResponse<SubmissionStatusResponse> getStatus(@PathVariable UUID id) {
        return ApiResponse.success(submissionService.getStatus(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full submission detail (own submissions only for STUDENT)")
    public ApiResponse<SubmissionResponse> get(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponse.success(submissionService.getSubmission(id, resolveUserId(authentication)));
    }

    private UUID resolveUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
