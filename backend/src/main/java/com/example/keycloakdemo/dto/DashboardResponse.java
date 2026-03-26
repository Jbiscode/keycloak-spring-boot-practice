package com.example.keycloakdemo.dto;

import java.util.List;

public record DashboardResponse(
        int totalUsers,
        List<String> activeRoles
) {}
