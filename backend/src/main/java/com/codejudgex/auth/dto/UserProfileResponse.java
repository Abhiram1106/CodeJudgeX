package com.codejudgex.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String department;
    private Short year;
    private String status;
    private Set<String> roles;
    private Instant createdAt;
}
