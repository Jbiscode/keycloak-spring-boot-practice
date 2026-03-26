# Feature Specification: Keycloak + Spring Boot 실무형 학습 환경 구축

**Feature Branch**: `001-keycloak-setup`
**Created**: 2026-03-23
**Status**: Draft
**Input**: 초보자가 Keycloak을 이해하기 쉽게 실습하되, 구조는 실무에서 복사 가능한 수준으로 유지한다.

## User Scenarios & Testing

### User Story 1 - Keycloak 실행 및 토큰 발급 (Priority: P1)

초보 개발자가 로컬 환경을 한 번에 실행하고, curl로 JWT 액세스 토큰을 발급받아
Keycloak의 인증 흐름과 토큰 구조를 직접 확인한다.

**Why this priority**: Keycloak 자체가 동작해야 모든 후속 실습이 가능하다.

**Independent Test**: `docker compose up keycloak` 후 curl로 토큰을 발급받아 JWT 구조를 확인한다.

**Verification Commands**:
- `docker compose up keycloak`
- `curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token ...`
- `echo "$TOKEN" | cut -d. -f2 | base64 -d`

**Acceptance Scenarios**:

1. **Given** docker compose up keycloak 실행, **When** curl로 Password Grant 토큰 요청, **Then** access_token 포함 JSON 응답 반환
2. **Given** 토큰 발급 완료, **When** jwt.io에 붙여넣기, **Then** realm_access.roles 클레임 확인 가능
3. **Given** 잘못된 비밀번호, **When** 토큰 요청, **Then** 401 에러 반환
4. **Given** 처음 보는 학습자, **When** 프로젝트 문서의 실행 순서를 따른다, **Then** 별도 콘솔 수동 설정 없이 토큰 발급까지 도달 가능하다

---

### User Story 2 - Spring Boot API 인증 연동 (Priority: P2)

Spring Boot API가 Keycloak에서 발급한 JWT를 검증하여, 인증된 사용자만 보호된
API에 접근할 수 있도록 하고, 사용자 정보 조회 흐름을 이해하기 쉽게 노출한다.

**Why this priority**: Resource Server 역할이 이 프로젝트의 핵심 학습 목표다.

**Independent Test**: Bearer 토큰 없이 `/api/user/me` 호출 → 401, 유효한 토큰으로 호출 → 200 반환 확인.

**Verification Commands**:
- `cd backend && ./gradlew test`
- `curl -i http://localhost:8081/api/public/health`
- `curl -i http://localhost:8081/api/user/me`
- `curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/user/me`

**Acceptance Scenarios**:

1. **Given** 유효한 Bearer 토큰, **When** `/api/user/me` 호출, **Then** JWT에서 추출한 사용자 정보 반환
2. **Given** 토큰 없이, **When** 보호된 API 호출, **Then** 401 반환
3. **Given** 만료된 토큰, **When** API 호출, **Then** 401 반환
4. **Given** `/api/public/health`, **When** 토큰 없이 호출, **Then** 200 반환
5. **Given** 인증 연동 로직, **When** 다른 보호 API를 추가한다, **Then** 공통 보안 설정과 사용자 정보 변환 로직을 재사용할 수 있다

---

### User Story 3 - RBAC 역할 기반 접근 제어 (Priority: P3)

사용자의 Keycloak Role에 따라 접근 가능한 API가 달라지는 RBAC를 실습하고,
새 역할이 추가되어도 같은 규칙으로 확장 가능한 구조를 확인한다.

**Why this priority**: 인증(P2) 이후 권한 제어를 추가하는 단계다.

**Independent Test**: ROLE_USER 토큰으로 `/api/admin/dashboard` 호출 → 403, ROLE_ADMIN 토큰으로 호출 → 200 확인.

**Verification Commands**:
- `curl -i -H "Authorization: Bearer $USER_TOKEN" http://localhost:8081/api/admin/dashboard`
- `curl -i -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8081/api/admin/dashboard`
- `curl -i -H "Authorization: Bearer $MANAGER_TOKEN" http://localhost:8081/api/manager/users`

**Acceptance Scenarios**:

1. **Given** ROLE_USER 토큰, **When** `/api/admin/dashboard` 호출, **Then** 403 반환
2. **Given** ROLE_ADMIN 토큰, **When** `/api/admin/dashboard` 호출, **Then** 200 반환
3. **Given** ROLE_MANAGER 토큰, **When** `/api/manager/users` 호출, **Then** 200 반환
4. **Given** 새로운 보호 엔드포인트, **When** 기존 권한 규칙을 적용한다, **Then** 기존 보안 설정을 중복 수정하지 않고 확장 가능하다

---

### User Story 4 - JWT 내부 구조 시각화 (Priority: P4)

현재 토큰의 모든 클레임(sub, iss, exp, realm_access.roles 등)을 API로 출력하여 JWT 구조를 이해한다.

**Why this priority**: 학습 보조 기능으로, 앞선 스토리가 완성된 후 추가한다.

**Independent Test**: 유효한 토큰으로 `/api/token/inspect` 호출 → JWT payload 전체 JSON 반환 확인.

**Verification Commands**:
- `curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/token/inspect`

**Acceptance Scenarios**:

1. **Given** 유효한 Bearer 토큰, **When** `/api/token/inspect` 호출, **Then** JWT payload 전체 반환
2. **Given** 반환된 payload, **Then** sub, iss, exp, realm_access.roles 필드 포함
3. **Given** 학습자, **When** inspect 응답을 본다, **Then** 인증과 권한 판단에 사용되는 핵심 클레임을 빠르게 식별할 수 있다

---

### Edge Cases

- Keycloak이 아직 준비되지 않았을 때 Spring Boot가 시작되면? → healthcheck + depends_on으로 순서 보장
- Docker 컨테이너 내부에서 Keycloak 호출 시 호스트명이 다르면? → 서비스명(`keycloak`) 사용
- realm-export.json import 실패 시? → Keycloak 로그 확인 방법 안내
- role 클레임 이름이 Spring Security 기본 규칙과 다르면? → 명시적 변환 규칙으로 매핑
- 학습용 설명을 위해 추가한 API가 실제 권한 정책을 흐리면? → public/private/role 제한을 명시적으로 구분
- 새 role 또는 보호 API가 추가되면? → 기존 공통 보안 설정과 상수/변환 규칙만 재사용해 확장

---

## Requirements

### Functional Requirements

- **FR-001**: Docker Compose 한 번으로 Keycloak + PostgreSQL + Spring Boot 실행 가능
- **FR-002**: Keycloak realm/client/role/user가 realm-export.json으로 자동 프로비저닝
- **FR-003**: Spring Boot는 Keycloak JWKS로 JWT 서명 자동 검증
- **FR-004**: `/api/public/**` 경로는 인증 없이 접근 가능
- **FR-005**: `/api/user/**`, `/api/manager/**`, `/api/admin/**` 는 각 Role 필요
- **FR-006**: `/api/token/inspect` 는 현재 JWT 전체 클레임 반환
- **FR-007**: 모든 보호 경로는 public, authenticated, role-restricted 중 하나로 명시적으로 분류되어야 한다
- **FR-008**: 각 사용자 스토리는 초보자가 그대로 실행할 수 있는 검증 명령을 문서에 포함해야 한다
- **FR-009**: 인증, 권한 변환, 컨트롤러 응답 조립 로직은 분리되어 다른 보호 API에서 재사용 가능해야 한다
- **FR-010**: 역할 문자열, 예외 응답 규칙, 사용자 정보 변환 규칙은 중복 없이 공통 규약으로 관리되어야 한다
- **FR-011**: 새 Role 또는 새 보호 API를 추가할 때 기존 핵심 보안 설정을 대규모 수정하지 않고 확장 가능해야 한다
- **FR-012**: 프로젝트 문서는 초보자가 실행 순서, 실패 지점, 기대 결과를 단계별로 이해할 수 있게 설명해야 한다

### Non-Functional Requirements

- **NFR-001**: 학습자가 P1부터 P3까지를 순서대로 따라가며 인증과 인가의 차이를 이해할 수 있어야 한다
- **NFR-002**: 실습용 코드는 계층 책임이 분리되어 객체지향적으로 읽히고 수정 가능해야 한다
- **NFR-003**: 데모 편의 기능이 추가되더라도 보안 경계와 권한 규칙은 실제 서비스와 유사한 수준으로 유지되어야 한다

### Key Entities

- **Realm**: `demo` — 모든 인증 도메인
- **Client (demo-app)**: Authorization Code + Password Grant, 브라우저/curl 테스트용
- **Client (demo-service)**: Client Credentials, M2M 인증 실습용
- **Role**: ROLE_USER, ROLE_MANAGER, ROLE_ADMIN (계층적 권한)
- **User**: testuser, testmanager, testadmin (각 Role 배정)
- **Protected Route Policy**: 각 API 경로에 대한 공개/인증필요/역할필요 규칙
- **User Profile View**: JWT 클레임에서 학습용 응답으로 변환된 사용자 정보 모델

### Assumptions

- 이 기능의 1차 목표는 초보 학습자가 Keycloak과 Spring Security 연결 흐름을 이해하는 것이다
- 프론트엔드는 포함하지 않고 curl 또는 API 클라이언트 기반 검증만 제공한다
- 실무형 구조는 모듈 분리, 책임 분리, 공통 규약 유지 수준까지 포함하며, 멀티서비스 플랫폼 수준의 공통 라이브러리화는 범위 밖이다

---

## Success Criteria

- **SC-001**: 신규 학습자가 문서만 보고 30분 이내에 토큰 발급과 보호 API 호출까지 재현할 수 있다
- **SC-002**: curl만으로 토큰 발급 후 401, 403, 200 응답 차이를 각 역할별로 검증할 수 있다
- **SC-003**: 학습자는 `/api/user/me`와 `/api/token/inspect` 결과를 통해 인증 정보와 권한 정보의 출처를 설명할 수 있다
- **SC-004**: 새 보호 API 또는 새 Role 추가 시 기존 인증 변환 규칙과 권한 분류 방식을 재사용해 확장할 수 있다
- **SC-005**: 코드 구조를 읽는 개발자가 보안 설정, 권한 변환, 응답 조립 책임을 서로 다른 구성 요소로 식별할 수 있다
