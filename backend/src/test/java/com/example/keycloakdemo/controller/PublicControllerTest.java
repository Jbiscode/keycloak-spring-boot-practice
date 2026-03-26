package com.example.keycloakdemo.controller;

import com.example.keycloakdemo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@Import(SecurityConfig.class)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // SecurityConfig가 JwtDecoder를 필요로 하므로 mock 등록
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void health_토큰없이_200반환() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
