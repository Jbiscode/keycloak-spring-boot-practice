package com.example.keycloakdemo.service;

import com.example.keycloakdemo.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenInspectionServiceTest {

    private TokenInspectionService tokenInspectionService;

    @BeforeEach
    void setUp() {
        tokenInspectionService = new TokenInspectionService();
    }

    @Test
    void JWT_클레임을_올바르게_추출한다() {
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

        Map<String, Object> result = tokenInspectionService.inspect(jwt);

        assertThat(result).containsKey("sub");
        assertThat(result).containsKey("iss");
        assertThat(result).containsKey("exp");
        assertThat(result).containsKey("preferred_username");
        assertThat(result).containsKey("realm_access");
        assertThat(result.get("preferred_username")).isEqualTo("testuser");
    }

    @Test
    void sub가_없으면_InvalidTokenException을_발생시킨다() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("iss", "http://localhost:8080/realms/demo")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> tokenInspectionService.inspect(jwt))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("sub");
    }
}
