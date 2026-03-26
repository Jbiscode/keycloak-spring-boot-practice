package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.config.SecurityConfig;
import com.example.keycloakdemo.dto.UserInfoResponse;
import com.example.keycloakdemo.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null")
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserInfoService userInfoService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void users_토큰없이_401반환() throws Exception {
        mockMvc.perform(get("/api/manager/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void users_ROLE_USER로_403반환() throws Exception {
        mockMvc.perform(get("/api/manager/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void users_ROLE_MANAGER로_200반환() throws Exception {
        UserInfoResponse userInfo = new UserInfoResponse(
                "testmanager",
                "testmanager@example.com",
                List.of("ROLE_USER", "ROLE_MANAGER"),
                "http://localhost:8080/realms/demo",
                Instant.now().plusSeconds(3600)
        );
        when(userInfoService.getUserInfo(any(Jwt.class))).thenReturn(userInfo);

        mockMvc.perform(get("/api/manager/users")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_MANAGER")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
