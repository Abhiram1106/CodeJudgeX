package com.codejudgex.problem.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.common.dto.PageResponse;
import com.codejudgex.problem.dto.AddTestCaseRequest;
import com.codejudgex.problem.dto.CreateProblemRequest;
import com.codejudgex.problem.dto.ProblemResponse;
import com.codejudgex.problem.dto.ProblemSummaryResponse;
import com.codejudgex.problem.dto.TestCaseResponse;
import com.codejudgex.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
@Tag(name = "Problems", description = "Problem library management")
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create a new problem (FACULTY+)")
    public ApiResponse<ProblemResponse> create(
            @Valid @RequestBody CreateProblemRequest request,
            Authentication authentication) {
        UUID createdBy = resolveUserId(authentication);
        return ApiResponse.success(problemService.createProblem(request, createdBy), "Problem created");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all problems (paginated)")
    public ApiResponse<PageResponse<ProblemSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                problemService.listProblems(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get problem detail — students receive sample test cases only")
    public ApiResponse<ProblemResponse> getForStudent(@PathVariable UUID id) {
        return ApiResponse.success(problemService.getProblemForStudent(id));
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get problem with all test cases including hidden (FACULTY+)")
    public ApiResponse<ProblemResponse> getForFaculty(@PathVariable UUID id) {
        return ApiResponse.success(problemService.getProblemForFaculty(id));
    }

    @PostMapping("/{id}/test-cases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Add a test case to a problem (FACULTY+)")
    public ApiResponse<TestCaseResponse> addTestCase(
            @PathVariable UUID id,
            @Valid @RequestBody AddTestCaseRequest request,
            Authentication authentication) {
        return ApiResponse.success(problemService.addTestCase(id, request, resolveUserId(authentication)));
    }

    @GetMapping("/{id}/test-cases")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "List all test cases for a problem (FACULTY+)")
    public ApiResponse<List<TestCaseResponse>> listTestCases(@PathVariable UUID id) {
        return ApiResponse.success(problemService.getProblemForFaculty(id).getSampleTestCases());
    }

    private UUID resolveUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
