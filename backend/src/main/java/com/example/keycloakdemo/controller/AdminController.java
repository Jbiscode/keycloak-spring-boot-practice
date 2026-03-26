package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.constant.Roles;
import com.example.keycloakdemo.dto.ApiResponse;
import com.example.keycloakdemo.dto.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        // 학습 목적: 고정값 반환 (실제 서비스에서는 DB 통계 조회)
        // TODO: 실제 구현 시 UserRepository.count(), roleService.getActiveRoles() 로 대체
        //       AdminControllerTest의 .value(3) 검증도 함께 수정 필요
        DashboardResponse response = new DashboardResponse(
                3,
                List.of(Roles.USER, Roles.MANAGER, Roles.ADMIN)
        );
        return ApiResponse.of(response);
    }
}
