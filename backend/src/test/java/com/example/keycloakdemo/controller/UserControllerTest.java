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

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserInfoService userInfoService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void me_토큰없이_401반환() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_ROLE_USER_토큰으로_200반환() throws Exception {
        UserInfoResponse response = new UserInfoResponse(
                "testuser",
                "testuser@example.com",
                List.of("ROLE_USER"),
                "http://localhost:8080/realms/demo",
                Instant.now().plusSeconds(3600)
        );
        when(userInfoService.getUserInfo(any(Jwt.class))).thenReturn(response);

        mockMvc.perform(get("/api/user/me")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"));
    }

    @Test
    void hello_토큰없이_401반환() throws Exception {
        mockMvc.perform(get("/api/user/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hello_ROLE_USER_토큰으로_200반환() throws Exception {
        mockMvc.perform(get("/api/user/hello")
                        .with(jwt()
                                .jwt(builder -> builder.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Hello, testuser!"));
    }
}
