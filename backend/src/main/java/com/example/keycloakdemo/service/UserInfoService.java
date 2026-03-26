package com.example.keycloakdemo.service;

import com.example.keycloakdemo.dto.UserInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoService {

    private static final Logger log = LoggerFactory.getLogger(UserInfoService.class);

    public UserInfoResponse getUserInfo(Jwt jwt) {
        // PII 로깅 규칙: sub(UUID)만 기록, email/username은 로그에 포함 금지
        log.info("fetch_user_info sub={}", jwt.getSubject());

        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        var rawIssuer = jwt.getIssuer();
        String issuer = rawIssuer != null ? rawIssuer.toString() : null;
        List<String> roles = extractRoles(jwt);

        return new UserInfoResponse(username, email, roles, issuer, jwt.getExpiresAt());
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof Collection<?> rawRoles) {
            return rawRoles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }
}
