# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Keycloak IAM을 처음 배우는 사람을 위한 인증/권한 관리 실습 프로젝트.
Docker로 Keycloak을 실행하고 Java Spring Boot(Gradle) 백엔드와 연동하여 OAuth2/OIDC + RBAC를 체험한다.

**Spec**: `specs/001-keycloak-setup/`

## 기술 스택

- **Keycloak 24.x**: IAM 서버 (Docker)
- **Java 17 / Spring Boot 3.2.x**: REST API 백엔드 (Gradle)
- **Spring Security + OAuth2 Resource Server**: JWT 인증
- **PostgreSQL 15**: Keycloak 전용 DB
- **Docker / Docker Compose**: 전체 환경 실행

## 프로젝트 구조

```text
keycloak_v1/
├── docker-compose.yml
├── .env
├── keycloak/
│   └── realm-export.json
├── backend/
│   ├── build.gradle
│   └── src/main/java/com/example/keycloakdemo/
│       ├── config/SecurityConfig.java
│       ├── config/KeycloakRoleConverter.java
│       └── controller/
└── specs/001-keycloak-setup/   # Feature spec & plan
```

## 개발 명령어

```bash
# 전체 실행
docker compose up

# Keycloak만 실행
docker compose up keycloak

# 백엔드만 빌드
cd backend && ./gradlew build

# 토큰 발급 테스트
TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=demo-app&client_secret=SECRET&username=testuser&password=user1234" \
  | jq -r '.access_token')
```

## 주요 구현 포인트

- `KeycloakRoleConverter`: Keycloak JWT의 `realm_access.roles`를 Spring Security `GrantedAuthority`로 변환 필수
- `application.yml`의 `issuer-uri`는 외부 접근 호스트(`localhost:8080`) 기준으로 설정
- Docker 내부 서비스 간 통신은 서비스명(`keycloak`) 사용

## 권장 진행 순서

`001-keycloak-setup`은 아래 순서로 진행한다.

1. `specs/001-keycloak-setup/spec.md`에서 P1~P4와 검증 시나리오를 먼저 읽는다.
2. `specs/001-keycloak-setup/plan.md` 기준으로 Phase 1 Keycloak 환경부터 완성한다.
3. `docker compose up keycloak`로 Keycloak 단독 기동 후 토큰 발급 curl을 먼저 검증한다.
4. 이후 Spring Boot 뼈대와 `KeycloakRoleConverter`를 붙여 `/api/public/health`와 `/api/user/me`를 확인한다.
5. 마지막으로 RBAC 엔드포인트(`/api/manager/**`, `/api/admin/**`)와 `/api/token/inspect`를 확장한다.

학습용 저장소이므로 P1 경로가 검증되기 전에는 P2/P3/P4 확장을 시작하지 않는다.

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
