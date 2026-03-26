# Tasks: Keycloak + Spring Boot 실무형 학습 환경 구축

**Input**: `/specs/001-keycloak-setup/`의 설계 문서
**Prerequisites**: `plan.md`(필수), `spec.md`(필수), `research.md`, `data-model.md`, `contracts/`

**Tests**: 모든 사용자 스토리는 실행 가능한 검증 경로가 필요하다. 백엔드 동작, JWT 매핑, RBAC 규칙을 위한 자동화 테스트를 포함한다.

**Organization**: 작업은 사용자 스토리별로 묶어 독립 구현과 독립 검증이 가능하도록 구성한다.

## Format: `[ID] [P?] [Story] 설명`

- **[P]**: 병렬 수행 가능 작업(서로 다른 파일, 미완료 의존성 없음)
- **[Story]**: 해당 작업이 속한 사용자 스토리(예: US1, US2, US3)
- 모든 작업 설명에는 정확한 파일 경로를 포함한다

## Phase 1: 설정 (공통 기반 준비)

**Purpose**: 모든 스토리에 필요한 프로젝트 구조와 학습용 문서 골격을 준비한다

- [X] T001 `backend/build.gradle`, `backend/settings.gradle`에 Gradle 프로젝트 설정을 작성한다
- [X] T002 Spring Boot 시작 클래스를 `backend/src/main/java/com/example/keycloakdemo/KeycloakDemoApplication.java`에 작성한다
- [X] T003 [P] `.env`에 로컬 환경 기본값을 작성한다
- [X] T004 [P] `specs/001-keycloak-setup/quickstart.md`에 학습자용 실행 가이드 초안을 작성한다

---

## Phase 2: 기반 작업 (모든 스토리의 선행 조건)

**Purpose**: 공통 보안, 응답 규약, 예외 처리, 컨테이너 빌드 규칙을 먼저 확정한다

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 어떤 사용자 스토리도 시작하지 않는다

- [X] T005 `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-local.yml`에 `issuer-uri`와 로컬 설정을 구성한다
- [X] T006 [P] `backend/src/main/java/com/example/keycloakdemo/constant/Roles.java`에 공통 role 상수를 정의한다
- [X] T007 [P] `backend/src/main/java/com/example/keycloakdemo/config/security/KeycloakRoleConverter.java`에 Keycloak JWT authority 매핑을 구현한다
- [X] T008 경로 보안 정책, JWT converter 연결, 401/403 처리를 `backend/src/main/java/com/example/keycloakdemo/config/SecurityConfig.java`에 구현한다
- [X] T009 [P] `backend/src/main/java/com/example/keycloakdemo/dto/ApiResponse.java`, `backend/src/main/java/com/example/keycloakdemo/dto/ErrorResponse.java`에 공통 응답 record를 작성한다
- [X] T010 [P] `backend/src/main/java/com/example/keycloakdemo/exception/InvalidTokenException.java`, `backend/src/main/java/com/example/keycloakdemo/exception/GlobalExceptionHandler.java`에 토큰 예외와 전역 예외 처리를 구현한다
- [X] T011 [P] `backend/Dockerfile`에 백엔드 컨테이너 이미지 빌드 구성을 추가한다
- [X] T012 hostname, issuer, role naming 규칙을 `specs/001-keycloak-setup/quickstart.md`에 문서화한다

**Checkpoint**: 공통 보안 기반이 준비되어 이후 스토리를 우선순위대로 진행할 수 있어야 한다

---

## Phase 3: User Story 1 - Keycloak 실행 및 토큰 발급 (Priority: P1) 🎯 MVP

**Goal**: 관리 콘솔 수동 설정 없이 Keycloak을 실행하고 학습자가 직접 확인 가능한 JWT를 발급한다

**Independent Test**: `docker compose up keycloak` 실행 후 `http://localhost:8080/realms/demo/protocol/openid-connect/token`에서 토큰을 발급받고 JWT payload에 `realm_access.roles`가 포함되는지 확인한다

### Implementation for User Story 1

- [X] T013 [US1] `docker-compose.yml`에 PostgreSQL과 Keycloak 서비스 및 readiness check를 구성한다
- [X] T014 [P] [US1] `keycloak/realm-export.json`에 Realm, client, role, demo user를 정의한다
- [X] T015 [US1] `specs/001-keycloak-setup/quickstart.md`에 토큰 발급 및 JWT decode 절차를 추가한다
- [X] T016 [US1] `specs/001-keycloak-setup/quickstart.md`에 import 실패 확인 방법과 기동 기대 결과를 정리한다

**Checkpoint**: Keycloak이 단독으로 기동되고 학습자가 검증 가능한 토큰을 발급할 수 있어야 한다

---

## Phase 4: User Story 2 - Spring Boot API 인증 연동 (Priority: P2)

**Goal**: 보호 API가 유효한 토큰만 허용하고, 사용자 정보 응답이 학습자에게 이해하기 쉽게 보이도록 만든다

**Independent Test**: `/api/public/health`는 토큰 없이 `200`, `/api/user/me`는 토큰 없이 `401`, 유효한 토큰으로는 `200`을 반환해야 한다

### Tests for User Story 2

> **NOTE: 테스트를 먼저 작성하고, 구현 전 실패 상태를 확인한다**

- [X] T017 [P] [US2] `backend/src/test/java/com/example/keycloakdemo/controller/PublicControllerTest.java`, `backend/src/test/java/com/example/keycloakdemo/controller/UserControllerTest.java`에 public/authenticated controller 테스트를 추가한다
- [X] T018 [P] [US2] `backend/src/test/java/com/example/keycloakdemo/service/UserInfoServiceTest.java`에 JWT → 응답 변환 테스트를 추가한다
- [X] T019 [US2] `specs/001-keycloak-setup/quickstart.md`에 이 스토리의 검증 명령을 정리한다

### Implementation for User Story 2

- [X] T020 [P] [US2] `backend/src/main/java/com/example/keycloakdemo/dto/UserInfoResponse.java`에 사용자 정보 응답 record를 작성한다
- [X] T021 [US2] `backend/src/main/java/com/example/keycloakdemo/service/UserInfoService.java`에 JWT 사용자 정보 변환 서비스를 구현한다
- [X] T022 [US2] `backend/src/main/java/com/example/keycloakdemo/controller/PublicController.java`에 public health 엔드포인트를 구현한다
- [X] T023 [US2] `backend/src/main/java/com/example/keycloakdemo/controller/UserController.java`에 인증 사용자 엔드포인트를 구현한다

**Checkpoint**: public 엔드포인트와 authenticated 엔드포인트가 독립적으로 검증 가능해야 한다

---

## Phase 5: User Story 3 - RBAC 역할 기반 접근 제어 (Priority: P3)

**Goal**: 역할별 접근 제어를 보여주고, 새 보호 엔드포인트를 추가해도 같은 패턴으로 확장 가능한 구조를 유지한다

**Independent Test**: `/api/admin/dashboard`는 user 토큰으로 `403`, admin 토큰으로 `200`, `/api/manager/users`는 manager 토큰으로 `200`을 반환해야 한다

### Tests for User Story 3

- [X] T024 [P] [US3] `backend/src/test/java/com/example/keycloakdemo/controller/ManagerControllerTest.java`, `backend/src/test/java/com/example/keycloakdemo/controller/AdminControllerTest.java`에 RBAC controller 테스트를 추가한다
- [X] T025 [US3] `specs/001-keycloak-setup/quickstart.md`에 RBAC 검증 명령을 정리한다

### Implementation for User Story 3

- [X] T026 [P] [US3] `backend/src/main/java/com/example/keycloakdemo/dto/DashboardResponse.java`에 admin dashboard 응답 record를 작성한다
- [X] T027 [US3] `backend/src/main/java/com/example/keycloakdemo/controller/ManagerController.java`에 manager 전용 엔드포인트를 구현한다
- [X] T028 [US3] `backend/src/main/java/com/example/keycloakdemo/controller/AdminController.java`에 admin 전용 엔드포인트를 구현한다

**Checkpoint**: 역할 기반 접근 제어가 인증 기능을 깨지 않고 독립적으로 검증 가능해야 한다

---

## Phase 6: User Story 4 - JWT 내부 구조 시각화 (Priority: P4)

**Goal**: 애플리케이션의 보안 경계를 유지하면서 학습자가 JWT 클레임을 직접 확인할 수 있게 한다

**Independent Test**: 유효한 토큰으로 `/api/token/inspect`를 호출했을 때 `sub`, `iss`, `exp`, `realm_access.roles`가 포함되어야 한다

### Tests for User Story 4

- [X] T029 [P] [US4] `backend/src/test/java/com/example/keycloakdemo/service/TokenInspectionServiceTest.java`, `backend/src/test/java/com/example/keycloakdemo/controller/TokenControllerTest.java`에 token inspection 테스트를 추가한다
- [X] T030 [US4] `specs/001-keycloak-setup/quickstart.md`에 token inspection 검증 명령을 정리한다

### Implementation for User Story 4

- [X] T031 [US4] `backend/src/main/java/com/example/keycloakdemo/service/TokenInspectionService.java`에 token inspection 서비스를 구현한다
- [X] T032 [US4] `backend/src/main/java/com/example/keycloakdemo/controller/TokenController.java`에 token inspection 엔드포인트를 구현한다

**Checkpoint**: 모든 사용자 스토리가 독립적으로 동작하고 검증 가능해야 한다

---

## Phase 7: 마무리 및 공통 점검

**Purpose**: 전체 흐름 검증, 문서 정리, 구현 책임 분리 점검을 마친다

- [X] T033 전체 컨테이너 통합 실행 절차와 기대 결과를 `specs/001-keycloak-setup/quickstart.md`에 반영한다
- [X] T034 [P] `specs/001-keycloak-setup/spec.md`, `specs/001-keycloak-setup/contracts/api.md`, `specs/001-keycloak-setup/research.md`의 정합성을 최종 점검한다
- [X] T035 [P] `backend/src/main/java/com/example/keycloakdemo/config/SecurityConfig.java`, `backend/src/main/java/com/example/keycloakdemo/config/security/KeycloakRoleConverter.java`, `backend/src/main/java/com/example/keycloakdemo/service/UserInfoService.java`, `backend/src/main/java/com/example/keycloakdemo/service/TokenInspectionService.java`의 책임 분리가 계획과 맞는지 점검한다
- [X] T036 백엔드 테스트 및 검증 상태를 `specs/001-keycloak-setup/plan.md`에 기록한다

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 진행, 모든 사용자 스토리를 막는 선행 단계
- **User Story 1 (Phase 3)**: Foundational 완료 후 진행
- **User Story 2 (Phase 4)**: 실제 토큰 기반 검증을 위해 User Story 1 이후 진행
- **User Story 3 (Phase 5)**: 인증 기반이 필요하므로 User Story 2 이후 진행
- **User Story 4 (Phase 6)**: 인증 처리 공통 기반이 필요하므로 User Story 2 이후 진행
- **Polish (Phase 7)**: 원하는 사용자 스토리 구현이 끝난 뒤 진행

### User Story Dependencies

- **US1 (P1)**: 최초 실행 가능한 학습 흐름, 후속 스토리 의존성 없음
- **US2 (P2)**: US1의 토큰 발급 흐름을 기반으로 검증
- **US3 (P3)**: US2의 인증 처리 위에서 RBAC 확장
- **US4 (P4)**: US2의 인증 처리 위에서 JWT inspection 확장

### Within Each User Story

- 검증 명령이 문서에 있어야 스토리를 완료 처리할 수 있다
- 자동화 테스트가 포함된 경우 구현 전에 실패 상태를 확인한다
- DTO 및 공통 모델을 먼저 만든다
- 서비스 구현 후 컨트롤러를 구현한다
- 컨트롤러 구현 후 문서 검증을 마무리한다

### Parallel Opportunities

- Setup 단계에서는 `T003`, `T004`를 병렬로 수행할 수 있다
- Foundational 단계에서는 `T006`, `T007`, `T009`, `T010`, `T011`을 병렬로 수행할 수 있다
- US2에서는 `T017`, `T018`을 병렬로 수행할 수 있다
- US3에서는 `T024`, `T026`을 병렬로 수행할 수 있다
- US4에서는 공통 인증 흐름이 안정된 후 `T029`, `T031`을 병렬로 수행할 수 있다

---

## Parallel Example: User Story 2

```bash
Task: "backend/src/test/java/com/example/keycloakdemo/controller/PublicControllerTest.java, backend/src/test/java/com/example/keycloakdemo/controller/UserControllerTest.java 에 public/authenticated controller 테스트 추가"
Task: "backend/src/test/java/com/example/keycloakdemo/service/UserInfoServiceTest.java 에 JWT → 응답 변환 테스트 추가"
Task: "backend/src/main/java/com/example/keycloakdemo/dto/UserInfoResponse.java 에 사용자 정보 응답 record 작성"
```

## Parallel Example: User Story 3

```bash
Task: "backend/src/test/java/com/example/keycloakdemo/controller/ManagerControllerTest.java, backend/src/test/java/com/example/keycloakdemo/controller/AdminControllerTest.java 에 RBAC controller 테스트 추가"
Task: "backend/src/main/java/com/example/keycloakdemo/dto/DashboardResponse.java 에 admin dashboard 응답 record 작성"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료
3. Phase 3: User Story 1 완료
4. 백엔드 API 작업 전에 토큰 발급 흐름을 독립 검증

### Incremental Delivery

1. Setup + Foundational 완료
2. US1 전달 후 Keycloak 기동과 토큰 발급 검증
3. US2 전달 후 public/authenticated 접근 검증
4. US3 전달 후 RBAC 매트릭스 검증
5. US4 전달 후 JWT claim inspection 검증

### Suggested MVP Scope

- **MVP**: User Story 1만 포함
- **첫 실사용 데모 범위**: User Story 1 + User Story 2
- **전체 학습 흐름**: User Story 1 + User Story 2 + User Story 3 + User Story 4

---

## Notes

- 모든 작업은 체크박스, Task ID, 라벨, 파일 경로를 포함하는 형식을 따른다
- 학습자 검증 문서는 마무리 작업이 아니라 각 스토리의 일부로 취급한다
- `ROLE_` 규칙, issuer 일관성, 401/403 처리 누락을 방지하도록 작업을 명시했다
