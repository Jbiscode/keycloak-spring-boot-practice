package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.config.SecurityConfig;
import com.example.keycloakdemo.service.TokenInspectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null")
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenInspectionService tokenInspectionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void inspect_토큰없이_401반환() throws Exception {
        mockMvc.perform(get("/api/token/inspect"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inspect_ROLE_USER_토큰으로_200반환_및_주요클레임포함() throws Exception {
        Map<String, Object> claims = Map.of(
                "sub", "user-uuid-123",
                "iss", "http://localhost:8080/realms/demo",
                "exp", 1234567890L,
                "preferred_username", "testuser",
                "realm_access", Map.of("roles", List.of("ROLE_USER"))
        );
        when(tokenInspectionService.inspect(any(Jwt.class))).thenReturn(claims);

        mockMvc.perform(get("/api/token/inspect")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sub").value("user-uuid-123"))
                .andExpect(jsonPath("$.data.iss").value("http://localhost:8080/realms/demo"))
                .andExpect(jsonPath("$.data.preferred_username").value("testuser"));
    }
}
