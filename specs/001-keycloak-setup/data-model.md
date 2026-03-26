# Data Model

Java 17 기준으로 DTO는 `record`를 사용해 불변성을 유지한다.
유효성 보정과 변환 책임은 service 또는 compact constructor에서 처리한다.

## DTOs

### UserInfoResponse

현재 JWT에서 학습자가 이해할 수 있는 사용자 정보만 추출한 응답 모델.

```java
public record UserInfoResponse(
        String username,
        String email,
        List<String> roles,
        String issuer,
        Instant expiresAt
) {}
```

| Field | Source | Description |
|-------|--------|-------------|
| `username` | `preferred_username` | Keycloak 사용자명 |
| `email` | `email` | 사용자 이메일 |
| `roles` | `realm_access.roles` | 변환 후 authority 목록 |
| `issuer` | `iss` | 토큰 발급자 |
| `expiresAt` | `exp` | 토큰 만료 시각 |

---

### ApiResponse<T>

모든 정상 응답에서 사용하는 공통 래퍼.

```java
public record ApiResponse<T>(
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Instant.now());
    }
}
```

---

### ErrorResponse

인증 실패, 권한 부족, 학습용 토큰 해석 실패 등 오류 응답에 사용하는 공통 모델.

```java
public record ErrorResponse(
        String error,
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now());
    }
}
```

---

### DashboardResponse

관리자 대시보드 전용 응답 모델.

```java
public record DashboardResponse(
        int totalUsers,
        List<String> activeRoles
) {}
```

---

### ProtectedRoutePolicy

문서와 보안 설정에서 공통으로 사용하는 경로 보안 분류 개념.

| Policy | Meaning |
|--------|---------|
| `PUBLIC` | 인증 없이 접근 가능 |
| `AUTHENTICATED` | 유효한 Bearer 토큰 필요 |
| `ROLE_RESTRICTED` | 유효한 Bearer 토큰과 특정 Role 필요 |

---

## Constants

### Roles

Role 문자열 중복을 줄이기 위한 상수 클래스.
Spring Security authority와 같은 값을 사용한다.

```java
public final class Roles {
    public static final String USER = "ROLE_USER";
    public static final String MANAGER = "ROLE_MANAGER";
    public static final String ADMIN = "ROLE_ADMIN";

    private Roles() {
    }
}
```

---

## Exceptions

### InvalidTokenException

JWT 클레임 추출 또는 학습용 inspect 변환 실패 시 발생하는 도메인 예외.

```java
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### GlobalExceptionHandler

예외를 API 응답 규약으로 변환하는 전역 예외 처리기.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        log.error("invalid_token message={}", ex.getMessage(), ex);
        return ErrorResponse.of("INVALID_TOKEN", ex.getMessage());
    }
}
```

---

## Keycloak Configuration Entities

### Realm

| Field | Value |
|-------|-------|
| `realm` | `demo` |

### Clients

| clientId | Purpose | directAccessGrantsEnabled | serviceAccountsEnabled |
|----------|---------|---------------------------|------------------------|
| `demo-app` | curl 및 브라우저 기반 학습용 | true | false |
| `demo-service` | client credentials 학습 확장용 | false | true |

### Realm Roles

| roleName | Description |
|----------|-------------|
| `ROLE_USER` | 기본 보호 API 접근 |
| `ROLE_MANAGER` | 관리자 목록 조회 |
| `ROLE_ADMIN` | 관리자 대시보드 접근 |

### Users

| username | password | roles |
|----------|----------|-------|
| `testuser` | `user1234` | `ROLE_USER` |
| `testmanager` | `manager1234` | `ROLE_USER`, `ROLE_MANAGER` |
| `testadmin` | `admin1234` | `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN` |
