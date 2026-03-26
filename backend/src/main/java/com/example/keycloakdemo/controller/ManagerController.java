package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.dto.ApiResponse;
import com.example.keycloakdemo.dto.UserInfoResponse;
import com.example.keycloakdemo.service.UserInfoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final UserInfoService userInfoService;

    public ManagerController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserInfoResponse>> users(@AuthenticationPrincipal Jwt jwt) {
        // 학습 목적: 현재 인증된 사용자 정보만 반환 (실제 서비스에서는 DB 조회)
        UserInfoResponse currentUser = userInfoService.getUserInfo(jwt);
        return ApiResponse.of(List.of(currentUser));
    }
}
