package com.codejudgex.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class AuthResponse {
    private UUID id;
    private String name;
    private String email;
    private Set<String> roles;
    private String accessToken;
    private long accessTokenExpiresIn;
}
