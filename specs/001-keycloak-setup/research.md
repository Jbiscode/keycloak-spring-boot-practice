# Research: Keycloak + Spring Boot 연동

## 1. Keycloak realm-export.json

**Decision**: `start-dev --import-realm` 커맨드와
`/opt/keycloak/data/import/realm-export.json` 마운트로 초기 Realm을 자동 import한다.

**Rationale**:
- 초보자가 관리 콘솔에서 수동 설정하지 않아도 바로 실습을 시작할 수 있다.
- 버전 관리된 Realm 설정을 유지해 재현 가능성을 확보할 수 있다.

**Alternatives considered**:
- Keycloak Admin REST API로 초기 설정 생성
  - 자동화는 가능하지만 학습용 저장소에서는 초기 진입 비용이 커진다.

## 2. Spring Boot 3.x + OAuth2 Resource Server

**Decision**: Spring Security의 `oauth2-resource-server` 방식을 사용하고,
Keycloak 전용 Spring Adapter는 사용하지 않는다.

**Rationale**:
- Spring Boot 3.x 기준 표준 방식이다.
- Keycloak 종속 설정을 줄이고 다른 OIDC 공급자와도 유사한 구조를 유지할 수 있다.
- 학습자에게 "Keycloak 전용 마법 설정"보다 "JWT Resource Server 기본 원리"를
  설명하기 쉽다.

**Alternatives considered**:
- Keycloak 전용 Spring Adapter
  - 최신 Spring Boot 흐름과 맞지 않고 재사용성도 낮다.

## 3. `realm_access.roles` 변환 규칙

**Decision**: `KeycloakRoleConverter`를 별도 클래스로 두고,
`realm_access.roles`를 Spring Security authority로 변환한다.

**Rationale**:
- Keycloak의 role 클레임은 중첩 구조라 기본 변환으로는 바로 사용하기 어렵다.
- 보안 설정과 role 추출 로직을 분리하면 다른 보호 API에서도 재사용하기 쉽다.
- Java 관례상 변환 책임은 `SecurityConfig`에 섞지 않고 전용 converter로 분리하는
  편이 읽기 쉽다.

**Role naming rule**:
- Keycloak Realm role 이름은 `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`으로 유지한다.
- 변환기는 입력 role이 이미 `ROLE_`로 시작하면 그대로 사용하고, 그렇지 않으면
  `ROLE_` 접두어를 보정한다.
- `SecurityConfig`와 서비스 레이어는 최종 authority 값을 `ROLE_*` 형태로 사용한다.

```java
public final class KeycloakRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> rawRoles)) {
            return List.of();
        }

        return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::normalizeRole)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private String normalizeRole(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
```

## 4. Docker Compose healthcheck 패턴

**Decision**: Keycloak readiness healthcheck와
`depends_on: condition: service_healthy`를 사용한다.

**Rationale**:
- Keycloak은 초기 부팅 시간이 길고, 준비 전 JWT 검증을 시도하면 혼란스러운
  시작 실패가 발생한다.
- 학습자가 "왜 backend가 먼저 실패했는지" 추적하지 않도록 시작 순서를 고정한다.

```yaml
keycloak:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
    interval: 10s
    timeout: 5s
    retries: 10

backend:
  depends_on:
    keycloak:
      condition: service_healthy
```

## 5. Hostname / issuer 규칙

**Decision**:
- 외부 접근 URL은 `http://localhost:8080`을 사용한다.
- Docker 네트워크 내부 통신은 서비스명 `http://keycloak:8080`을 사용한다.
- Keycloak이 발급하는 `iss` 클레임은 외부 접근 기준인
  `http://localhost:8080/realms/demo`로 고정한다.
- Spring Boot의 JWT issuer 검증 기준도 같은 외부 issuer 값으로 맞춘다.

**Rationale**:
- 학습자는 브라우저와 curl에서 `localhost`를 보게 되므로 토큰의 `iss`도 같은 값을
  유지하는 편이 이해하기 쉽다.
- 내부 통신 주소와 외부 issuer 주소를 구분해 문서화하면 Docker 네트워크 개념도 함께
  설명할 수 있다.

**Operational note**:
- 컨테이너 내부에서 Keycloak 관리 API를 호출해야 하는 경우 서비스명 `keycloak`을
  사용한다.
- JWT `iss` 검증은 외부 issuer 기준으로 수행되므로 Keycloak hostname 설정을
  `localhost`에 맞춰야 한다.

## 6. API 계층과 객체지향 구조

**Decision**:
- Controller는 요청/응답 경계만 담당한다.
- JWT 해석과 응답 변환은 Service가 담당한다.
- 보안 정책은 `SecurityConfig`, role 변환은 `KeycloakRoleConverter`,
  예외 응답은 `GlobalExceptionHandler`가 담당한다.

**Rationale**:
- 초보자도 각 클래스 책임을 쉽게 추적할 수 있다.
- 실무형 구조를 유지하면서도 불필요한 추상화는 추가하지 않는다.
- 새 보호 API를 추가할 때 controller만 늘리고 기존 보안/변환 규칙은 재사용할 수 있다.
