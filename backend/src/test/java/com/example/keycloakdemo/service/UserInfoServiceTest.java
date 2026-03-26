package com.example.keycloakdemo.service;

import com.example.keycloakdemo.dto.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoServiceTest {

    private UserInfoService userInfoService;

    @BeforeEach
    void setUp() {
        userInfoService = new UserInfoService();
    }

    @Test
    void JWT에서_사용자정보를_올바르게_추출한다() {
        Instant exp = Instant.now().plusSeconds(3600);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "user-uuid-123")
                .claim("iss", "http://localhost:8080/realms/demo")
                .claim("preferred_username", "testuser")
                .claim("email", "testuser@example.com")
                .claim("realm_access", Map.of("roles", List.of("ROLE_USER")))
                .expiresAt(exp)
                .issuedAt(Instant.now())
                .build();

        UserInfoResponse result = userInfoService.getUserInfo(jwt);

        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("testuser@example.com");
        assertThat(result.roles()).contains("ROLE_USER");
        assertThat(result.issuer()).isEqualTo("http://localhost:8080/realms/demo");
        assertThat(result.expiresAt()).isEqualTo(exp);
    }

    @Test
    void realm_access가_없으면_빈_role_목록을_반환한다() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "user-uuid-123")
                .claim("iss", "http://localhost:8080/realms/demo")
                .claim("preferred_username", "testuser")
                .claim("email", "testuser@example.com")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .build();

        UserInfoResponse result = userInfoService.getUserInfo(jwt);

        assertThat(result.roles()).isEmpty();
    }
}
