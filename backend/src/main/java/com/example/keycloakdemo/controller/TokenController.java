package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.dto.ApiResponse;
import com.example.keycloakdemo.service.TokenInspectionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    private final TokenInspectionService tokenInspectionService;

    public TokenController(TokenInspectionService tokenInspectionService) {
        this.tokenInspectionService = tokenInspectionService;
    }

    @GetMapping("/inspect")
    public ApiResponse<Map<String, Object>> inspect(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(tokenInspectionService.inspect(jwt));
    }
}
