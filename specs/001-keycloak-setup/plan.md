# Implementation Plan: Keycloak + Spring Boot 실무형 학습 환경 구축

**Branch**: `001-keycloak-setup` | **Date**: 2026-03-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-keycloak-setup/spec.md`

## Summary

Docker Compose로 Keycloak + PostgreSQL + Spring Boot를 한 번에 실행하고,
Spring Boot를 OAuth2 Resource Server로 구성하여 JWT 인증 + RBAC를 실습한다.
realm-export.json으로 Keycloak 설정을 자동 프로비저닝하여 수동 설정 없이 즉시
실습 가능하게 하며, 보안 변환 로직과 API 계층을 분리해 다른 보호 API에도
재사용 가능한 구조를 유지한다.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.2.x
**Primary Dependencies**: Spring Security, spring-boot-starter-oauth2-resource-server, Lombok (service/config 한정)
**Build Tool**: Gradle
**Storage**: PostgreSQL 15 (Keycloak 전용), Spring Boot는 DB 없음
**Testing**: JUnit 5, AssertJ, Mockito, spring-security-test
**Target Platform**: macOS (Docker 기반 로컬 개발)
**Project Type**: web-service (REST API)
**Performance Goals**: 학습용, 성능 요구사항 없음
**Constraints**: docker compose up 한 번으로 전체 실행 가능해야 함
**Scale/Scope**: 단일 개발자 로컬 실습 환경

## Constitution Check

- P1 학습 경로는 `Keycloak 실행 → 토큰 발급 → 보호 API 호출` 순서로 정의되어 있다.
- 로컬 환경은 Docker Compose와 버전 관리된 설정 파일만으로 재현 가능해야 한다.
- 모든 엔드포인트는 public, authenticated, role-restricted 중 하나로 명시되어야 한다.
- 각 사용자 스토리는 `docker compose`, `./gradlew test`, `curl` 기반 검증 경로를 가져야 한다.
- 보안 변환, 응답 조립, 컨트롤러 책임은 분리해 다른 보호 API에 재사용 가능해야 한다.

## Project Structure

### Documentation (this feature)

```text
specs/001-keycloak-setup/
├── plan.md              # 이 파일
├── spec.md              # Feature specification
├── research.md          # Phase 0: 기술 조사
├── data-model.md        # Phase 1: 데이터 모델
├── contracts/           # Phase 1: API 계약
│   └── api.md
└── tasks.md             # Phase 2: /speckit.tasks 출력
```

### Source Code (repository root)

```text
keycloak_v1/
├── docker-compose.yml
├── .env
├── keycloak/
│   └── realm-export.json
└── backend/
    ├── build.gradle
    ├── settings.gradle
    ├── Dockerfile
    └── src/
        ├── main/
        │   ├── java/com/example/keycloakdemo/
        │   │   ├── KeycloakDemoApplication.java
        │   │   ├── config/
        │   │   │   ├── SecurityConfig.java              # 앱 경로별 RBAC 설정만 담당
        │   │   │   └── security/                        # ← Keycloak 전용 분리 (다른 서비스 복사 단위)
        │   │   │       └── KeycloakRoleConverter.java   # realm_access.roles → GrantedAuthority
        │   │   ├── controller/
        │   │   │   ├── PublicController.java
        │   │   │   ├── UserController.java
        │   │   │   ├── ManagerController.java
        │   │   │   ├── AdminController.java
        │   │   │   └── TokenController.java
        │   │   ├── service/
        │   │   │   ├── UserInfoService.java             # JWT → UserInfoResponse 변환 로직
        │   │   │   └── TokenInspectionService.java      # JWT claims 추출/포맷
        │   │   ├── dto/                                 # Java 17 record 전용
        │   │   │   ├── UserInfoResponse.java            # record
        │   │   │   ├── ApiResponse.java                 # record<T> — 모든 API 응답 통일
        │   │   │   └── DashboardResponse.java           # record
        │   │   ├── exception/
        │   │   │   ├── InvalidTokenException.java       # unchecked, 토큰 파싱 실패
        │   │   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice
        │   │   └── constant/
        │   │       └── Roles.java                       # ROLE_USER, ROLE_MANAGER, ROLE_ADMIN 상수
        │   └── resources/
        │       ├── application.yml                      # 공통 설정
        │       └── application-local.yml                # 로컬 환경 오버라이드 (포트, 로그 레벨 등)
        └── test/
            └── java/com/example/keycloakdemo/          # main 구조 미러링
                ├── controller/
                │   ├── PublicControllerTest.java        # MockMvc, 인증 없이 200 확인
                │   └── UserControllerTest.java          # @WithMockUser, 401/403/200
                └── service/
                    ├── UserInfoServiceTest.java         # JUnit5 + AssertJ, Mockito
                    └── TokenInspectionServiceTest.java  # InvalidTokenException 발생 케이스 포함
```

**Structure Decision**: Web application (backend only) — Option 2 변형. 프론트엔드 없이 curl/Postman으로 테스트.

## Complexity Tracking

해당 없음.

---

## Phase 0: Research

*→ [research.md](research.md) 참고*

**조사 항목**:
- Keycloak 24.x realm-export.json 포맷
- Spring Boot 3.x OAuth2 Resource Server + Keycloak 연동 방식
- `realm_access.roles` 커스텀 변환기 필요 이유
- Docker Compose healthcheck으로 Keycloak 준비 완료 후 Spring Boot 시작하는 패턴

---

## Phase 1: Design & Contracts

### API 계약 → [contracts/api.md](contracts/api.md)

| Method | Path | 인증 | Role | 응답 |
|--------|------|------|------|------|
| GET | `/api/public/health` | ❌ | - | `{"status":"UP"}` |
| GET | `/api/user/me` | ✅ | ROLE_USER | `UserInfoResponse` |
| GET | `/api/user/hello` | ✅ | ROLE_USER | `{"message":"Hello, {username}"}` |
| GET | `/api/manager/users` | ✅ | ROLE_MANAGER | `List<UserInfoResponse>` |
| GET | `/api/admin/dashboard` | ✅ | ROLE_ADMIN | `{"stats":{...}}` |
| GET | `/api/token/inspect` | ✅ | ROLE_USER | JWT claims JSON |

### 데이터 모델 → [data-model.md](data-model.md)

**UserInfoResponse** — Java 17 `record`
```java
// ✅ record: 불변, equals/hashCode/toString 자동 생성, Lombok 불필요
public record UserInfoResponse(
    @NonNull String username,     // JWT preferred_username
    @NonNull String email,        // JWT email
    @NonNull List<String> roles,  // realm_access.roles
    @NonNull String issuer,       // JWT iss
    @NonNull Instant expiresAt    // JWT exp
) {}

// ✅ ApiResponse: generic record 래퍼
public record ApiResponse<T>(
    @NonNull T data,
    @NonNull Instant timestamp
) {
    public static <T> ApiResponse<T> of(@NonNull T data) {
        return new ApiResponse<>(data, Instant.now());
    }
}
```

**Roles 상수** — 문자열 리터럴 중복 방지
```java
// constant/Roles.java
public final class Roles {
    public static final String USER    = "ROLE_USER";
    public static final String MANAGER = "ROLE_MANAGER";
    public static final String ADMIN   = "ROLE_ADMIN";
    private Roles() {}
}
```

**도메인 예외** — unchecked 예외로 fail fast
```java
// exception/InvalidTokenException.java
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}
```

**로깅** — 서비스 레이어에 SLF4J 적용
```java
private static final Logger log = LoggerFactory.getLogger(UserInfoService.class);
log.info("fetch_user_info sub={}", jwt.getSubject());
log.error("failed_token_inspection reason={}", message, ex);
```

### Keycloak 설정

| 항목 | 값 |
|------|-----|
| Realm | `demo` |
| Client | `demo-app` (confidential, password grant + auth code) |
| Client | `demo-service` (confidential, client credentials) |
| Roles | `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN` |
| Users | testuser / testmanager / testadmin |

---

## Implementation TODO

### Phase 1: Keycloak 환경
- [X] `docker-compose.yml` 작성 (postgres + keycloak, healthcheck 포함)
- [X] `keycloak/realm-export.json` 작성 (Realm/Client/Role/User 자동 프로비저닝)
- [ ] `docker compose up keycloak` 실행 확인 + curl로 토큰 발급 테스트 ← 실제 컨테이너 실행 필요
- [X] realm-export.json import 실패 시 확인 방법 문서화 (`docker logs keycloak | grep -i import`)

### Phase 2: Spring Boot 뼈대 (Java Coding Standards 준수)
- [X] Gradle 프로젝트 생성 (`com.example.keycloakdemo`)
- [X] `application.yml` + `application-local.yml` 설정 (Keycloak `issuer-uri` / `jwk-set-uri`)
- [X] `constant/Roles.java` 작성 — Role 문자열 상수 정의
- [X] `config/security/KeycloakRoleConverter.java` 작성 (재활용 단위로 분리)
- [X] `SecurityConfig.java` 작성 (경로 권한만, Keycloak 설정은 `KeycloakRoleConverter` 위임)
- [X] `PublicController.java` → `/api/public/health` 동작 확인

### Phase 3: 서비스 레이어 + 인증 연동
- [X] `UserInfoService.java` 작성 — JWT → `UserInfoResponse` record 변환, SLF4J 로깅
- [X] `TokenInspectionService.java` 작성 — JWT claims 추출, `InvalidTokenException` 처리
- [X] `GlobalExceptionHandler.java` 작성 — `@RestControllerAdvice`, 예외 → 표준 응답
- [X] `UserController.java` 작성 + `UserControllerTest.java` (MockMvc, 401/200)
- [X] `ManagerController.java`, `AdminController.java` 작성
- [X] `TokenController.java` 작성
- [ ] RBAC 매트릭스 테스트 (401/403/200 curl로 확인) ← 실제 컨테이너 실행 필요

### Phase 4: 테스트 완성
- [X] `UserInfoServiceTest.java` — JUnit5 + AssertJ + Mockito
- [X] `UserControllerTest.java` — `@WithMockUser(roles="USER")` 403/200 분기
- [X] `ManagerControllerTest.java`, `AdminControllerTest.java` — RBAC 401/403/200 분기
- [X] `TokenInspectionServiceTest.java`, `TokenControllerTest.java`

### Phase 5: Docker 통합
- [X] `backend/Dockerfile` 작성 (multi-stage: builder + runtime)
- [X] `docker-compose.yml` backend 서비스 추가 (`depends_on: keycloak: condition: service_healthy`)
- [ ] `docker compose up` 전체 통합 실행 테스트 ← 실제 컨테이너 실행 필요

---

## 구현 완료 상태 (2026-03-23)

### 완료된 항목

| 파일 | 상태 |
|------|------|
| `backend/build.gradle`, `settings.gradle` | ✓ |
| `backend/Dockerfile` | ✓ |
| `backend/src/main/resources/application.yml` | ✓ |
| `backend/src/main/resources/application-local.yml` | ✓ |
| `docker-compose.yml` | ✓ |
| `keycloak/realm-export.json` | ✓ |
| `.env`, `.env.example` | ✓ |
| `KeycloakDemoApplication.java` | ✓ |
| `constant/Roles.java` | ✓ |
| `config/security/KeycloakRoleConverter.java` | ✓ |
| `config/SecurityConfig.java` | ✓ |
| `dto/ApiResponse.java`, `ErrorResponse.java` | ✓ |
| `dto/UserInfoResponse.java`, `DashboardResponse.java` | ✓ |
| `exception/InvalidTokenException.java` | ✓ |
| `exception/GlobalExceptionHandler.java` | ✓ |
| `service/UserInfoService.java` | ✓ |
| `service/TokenInspectionService.java` | ✓ |
| `controller/PublicController.java` | ✓ |
| `controller/UserController.java` | ✓ |
| `controller/ManagerController.java` | ✓ |
| `controller/AdminController.java` | ✓ |
| `controller/TokenController.java` | ✓ |
| 테스트: PublicControllerTest, UserControllerTest | ✓ |
| 테스트: ManagerControllerTest, AdminControllerTest | ✓ |
| 테스트: UserInfoServiceTest | ✓ |
| 테스트: TokenInspectionServiceTest, TokenControllerTest | ✓ |
| `specs/001-keycloak-setup/quickstart.md` | ✓ |

### 잔여 항목 (실제 컨테이너 실행 필요)

- `docker compose up keycloak` 후 토큰 발급 curl 테스트
- `docker compose up` 전체 통합 실행 테스트
- RBAC 매트릭스 curl 검증
