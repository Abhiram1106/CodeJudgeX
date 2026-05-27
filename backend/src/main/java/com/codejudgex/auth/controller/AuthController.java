package com.codejudgex.auth.controller;

import com.codejudgex.auth.dto.AuthResponse;
import com.codejudgex.auth.dto.LoginRequest;
import com.codejudgex.auth.dto.RefreshRequest;
import com.codejudgex.auth.dto.RegisterRequest;
import com.codejudgex.auth.dto.UserProfileResponse;
import com.codejudgex.auth.service.AuthService;
import com.codejudgex.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new student account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Registration successful");
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive access + refresh tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and receive a new access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke all refresh tokens for the authenticated user")
    public ApiResponse<Void> logout(Authentication authentication) {
        authService.logoutByEmail(resolveEmail(authentication));
        return ApiResponse.success(null, "Logged out successfully");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the authenticated user's profile")
    public ApiResponse<UserProfileResponse> me(Authentication authentication) {
        return ApiResponse.success(authService.getProfile(resolveEmail(authentication)));
    }

    /**
     * After the JWT filter fix, authentication.getName() is the userId UUID string.
     * The email is stored as credentials on UsernamePasswordAuthenticationToken.
     * In tests (@WithMockUser), credentials is null — getName() returns the mock username.
     */
    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken token) {
            Object credentials = token.getCredentials();
            if (credentials instanceof String email && !email.isBlank()) {
                return email;
            }
        }
        return authentication.getName();
    }
}
