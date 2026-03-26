package com.example.keycloakdemo.dto;

import java.time.Instant;
import java.util.List;

public record UserInfoResponse(
        String username,
        String email,
        List<String> roles,
        String issuer,
        Instant expiresAt
) {}
