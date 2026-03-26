package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.dto.ApiResponse;
import com.example.keycloakdemo.dto.UserInfoResponse;
import com.example.keycloakdemo.service.UserInfoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserInfoService userInfoService;

    public UserController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(userInfoService.getUserInfo(jwt));
    }

    @GetMapping("/hello")
    public ApiResponse<Map<String, String>> hello(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        String display = username != null ? username : "unknown";
        return ApiResponse.of(Map.of("message", "Hello, " + display + "!"));
    }
}
