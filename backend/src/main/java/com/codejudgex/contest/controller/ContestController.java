package com.codejudgex.contest.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.contest.dto.ContestResponse;
import com.codejudgex.contest.dto.ContestSummaryResponse;
import com.codejudgex.contest.dto.CreateContestRequest;
import com.codejudgex.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/contests")
@RequiredArgsConstructor
@Tag(name = "Contests", description = "Contest lifecycle and participation")
public class ContestController {

    private final ContestService contestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create a new contest (FACULTY+)")
    public ApiResponse<ContestResponse> create(
            @Valid @RequestBody CreateContestRequest request,
            @AuthenticationPrincipal Principal principal) {
        return ApiResponse.success(
                contestService.createContest(request, resolveUserId(principal)), "Contest created");
    }

    @PostMapping("/{id}/problems")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Add a problem to a contest (FACULTY+, DRAFT only)")
    public ApiResponse<ContestResponse> addProblem(
            @PathVariable UUID id,
            @RequestParam UUID problemId,
            @AuthenticationPrincipal Principal principal) {
        return ApiResponse.success(contestService.addProblem(id, problemId, resolveUserId(principal)));
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Register as a participant (STUDENT)")
    public ApiResponse<Void> register(
            @PathVariable UUID id,
            @AuthenticationPrincipal Principal principal) {
        contestService.registerParticipant(id, resolveUserId(principal));
        return ApiResponse.success(null, "Registered successfully");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List contests, optionally filtered by status")
    public ApiResponse<PageResponse<ContestSummaryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(contestService.listContests(
                status, PageRequest.of(page, size, Sort.by("startTime").descending())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get contest detail with problem list")
    public ApiResponse<ContestResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(contestService.getContest(id));
    }

    private UUID resolveUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
