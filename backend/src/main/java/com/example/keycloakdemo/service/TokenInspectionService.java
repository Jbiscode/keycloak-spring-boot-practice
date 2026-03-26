package com.example.keycloakdemo.service;

import com.example.keycloakdemo.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenInspectionService {

    private static final Logger log = LoggerFactory.getLogger(TokenInspectionService.class);

    public Map<String, Object> inspect(Jwt jwt) {
        log.info("token_inspect sub={}", jwt.getSubject());

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new InvalidTokenException("JWT에 sub 클레임이 없습니다");
        }

        var rawIssuer = jwt.getIssuer();
        var rawExp = jwt.getExpiresAt();

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", sub);
        claims.put("iss", rawIssuer != null ? rawIssuer.toString() : null);
        claims.put("exp", rawExp != null ? rawExp.getEpochSecond() : null);
        claims.put("preferred_username", jwt.getClaimAsString("preferred_username"));
        claims.put("email", jwt.getClaimAsString("email"));

        // realm_access 내부 구조 전체 노출 대신 roles 목록만 반환
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> rawRoles) {
            List<String> roles = rawRoles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            claims.put("realm_access", Map.of("roles", roles));
        }

        return claims;
    }
}
