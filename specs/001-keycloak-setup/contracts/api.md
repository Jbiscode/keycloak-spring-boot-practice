# API Contracts

Base URL: `http://localhost:8081`

모든 엔드포인트는 아래 세 가지 중 하나로 분류한다.
- `public`: 인증 불필요
- `authenticated`: 유효한 Bearer 토큰 필요
- `role-restricted`: 유효한 Bearer 토큰과 특정 Role 필요

---

## Public

### GET /api/public/health

**Security**: `public`

서버가 요청을 받을 준비가 되었는지 확인한다.

**Response 200**
```json
{
  "data": {
    "status": "UP"
  },
  "timestamp": "2026-03-23T00:00:00Z"
}
```

---

## User

### GET /api/user/me

**Security**: `role-restricted` (`ROLE_USER`)

현재 Bearer 토큰에서 추출한 사용자 정보를 반환한다.

**Request Header**
```http
Authorization: Bearer <access_token>
```

**Response 200**
```json
{
  "data": {
    "username": "testuser",
    "email": "testuser@example.com",
    "roles": ["ROLE_USER"],
    "issuer": "http://localhost:8080/realms/demo",
    "expiresAt": "2026-03-23T01:00:00Z"
  },
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 401**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Bearer token is missing or invalid",
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 403**
```json
{
  "error": "FORBIDDEN",
  "message": "Required role is missing",
  "timestamp": "2026-03-23T00:00:00Z"
}
```

---

### GET /api/user/hello

**Security**: `role-restricted` (`ROLE_USER`)

학습자가 인증 성공 이후 가장 단순한 보호 응답을 확인하는 예제 엔드포인트.

**Request Header**
```http
Authorization: Bearer <access_token>
```

**Response 200**
```json
{
  "data": {
    "message": "Hello, testuser!"
  },
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 401**: 토큰 없음 또는 만료
**Response 403**: `ROLE_USER` 부족

---

## Manager

### GET /api/manager/users

**Security**: `role-restricted` (`ROLE_MANAGER`)

관리자 권한 이상에서 접근 가능한 사용자 목록 예제 엔드포인트.

**Request Header**
```http
Authorization: Bearer <access_token>
```

**Response 200**
```json
{
  "data": [
    {
      "username": "testuser",
      "email": "testuser@example.com",
      "roles": ["ROLE_USER"],
      "issuer": "http://localhost:8080/realms/demo",
      "expiresAt": "2026-03-23T01:00:00Z"
    }
  ],
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 401**: 토큰 없음 또는 만료
**Response 403**: `ROLE_MANAGER` 부족

---

## Admin

### GET /api/admin/dashboard

**Security**: `role-restricted` (`ROLE_ADMIN`)

관리자 권한 전용 대시보드 예제 엔드포인트.

**Request Header**
```http
Authorization: Bearer <access_token>
```

**Response 200**
```json
{
  "data": {
    "totalUsers": 3,
    "activeRoles": ["ROLE_USER", "ROLE_MANAGER", "ROLE_ADMIN"]
  },
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 401**: 토큰 없음 또는 만료
**Response 403**: `ROLE_ADMIN` 부족

---

## Token

### GET /api/token/inspect

**Security**: `role-restricted` (`ROLE_USER`)

현재 JWT의 주요 클레임을 그대로 보여주는 학습용 엔드포인트.

**Request Header**
```http
Authorization: Bearer <access_token>
```

**Response 200**
```json
{
  "data": {
    "sub": "uuid-...",
    "iss": "http://localhost:8080/realms/demo",
    "exp": 1234567890,
    "preferred_username": "testuser",
    "email": "testuser@example.com",
    "realm_access": {
      "roles": ["ROLE_USER", "default-roles-demo"]
    }
  },
  "timestamp": "2026-03-23T00:00:00Z"
}
```

**Response 401**: 토큰 없음 또는 만료
**Response 403**: `ROLE_USER` 부족
