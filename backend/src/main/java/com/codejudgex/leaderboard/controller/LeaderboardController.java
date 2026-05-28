package com.codejudgex.leaderboard.controller;

import com.codejudgex.common.dto.ApiResponse;
import com.codejudgex.leaderboard.dto.LeaderboardEntryResponse;
import com.codejudgex.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Contest rankings — live from Redis, fallback to PostgreSQL snapshot")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/contests/{contestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get top-N leaderboard entries for a contest")
    public ApiResponse<List<LeaderboardEntryResponse>> getTopN(
            @PathVariable UUID contestId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(leaderboardService.getTopN(contestId, limit));
    }

    @GetMapping("/contests/{contestId}/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the authenticated student's rank in a contest")
    public ApiResponse<LeaderboardEntryResponse> getMyRank(
            @PathVariable UUID contestId,
            Authentication authentication) {
        UUID studentId = UUID.fromString(authentication.getName());
        return ApiResponse.success(leaderboardService.getStudentRank(contestId, studentId));
    }
}
