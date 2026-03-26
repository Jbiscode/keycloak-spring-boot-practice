# Quickstart: Keycloak + Spring Boot 학습 환경

이 가이드는 Docker Compose로 Keycloak + PostgreSQL + Spring Boot를 한 번에 실행하고,
JWT 인증과 RBAC를 직접 확인하는 방법을 설명한다.

---

## 사전 요구사항

- Docker Desktop (또는 Docker Engine + Compose)
- `curl`, `jq` (토큰 확인용)

---

## 빠른 시작

```bash
# 저장소 루트에서 실행
cp .env.example .env       # 최초 1회만

docker compose up -d       # 전체 스택 기동
```

> **참고**: Keycloak 초기 기동에 30~60초 소요될 수 있다.

---

## Hostname / Issuer 규칙

| 구분 | 주소 |
|------|------|
| 브라우저 / curl (외부) | `http://localhost:8080` |
| Docker 네트워크 내부 | `http://keycloak:8080` |
| JWT `iss` 클레임 값 | `http://localhost:8080/realms/demo` |
| Spring Boot issuer 검증 기준 | `http://localhost:8080/realms/demo` |

> Keycloak이 발급하는 `iss` 클레임은 **외부 접근 기준**으로 고정된다.
> Spring Boot의 JWT 검증 기준도 같은 값으로 맞춰야 인증이 성공한다.

---

## User Story 1: Keycloak 실행 및 토큰 발급

### Keycloak 단독 기동

```bash
docker compose up -d postgres keycloak
```

### 기동 확인

```bash
# Keycloak 준비 완료 확인
curl -s http://localhost:8080/health/ready | jq .
# → {"status":"UP"}
```

### 토큰 발급

```bash
# testuser 토큰 발급
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=demo-app" \
  -d "username=testuser" \
  -d "password=user1234" \
  | jq -r '.access_token')

echo $TOKEN
```

### JWT Payload 확인

```bash
# access_token의 payload 부분 decode (base64)
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq .
```

확인 항목:
- `iss`: `http://localhost:8080/realms/demo`
- `realm_access.roles`: `["ROLE_USER", "default-roles-demo", ...]`
- `preferred_username`: `testuser`
- `exp`: 만료 시각 (Unix timestamp)

### import 실패 확인 방법

```bash
# Keycloak 로그에서 realm import 관련 메시지 확인
docker logs keycloak 2>&1 | grep -i import
docker logs keycloak 2>&1 | grep -i "demo"
```

정상 기동 시 로그에서 확인할 내용:
- `Realm demo imported`
- Keycloak 컨테이너 상태: `healthy`

---

## User Story 2: Spring Boot API 인증 연동

### 전체 스택 기동

```bash
docker compose up -d
```

### 검증 명령

```bash
# 1. public health 엔드포인트 (토큰 불필요)
curl -s http://localhost:8081/api/public/health | jq .
# → {"data":{"status":"UP"},"timestamp":"..."}

# 2. 인증 없이 보호 API 접근 → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/user/me
# → 401

# 3. 유효한 토큰으로 사용자 정보 조회 → 200
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=demo-app" \
  -d "username=testuser" -d "password=user1234" \
  | jq -r '.access_token')

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/user/me | jq .
# → {"data":{"username":"testuser","email":"testuser@example.com","roles":[...],...},...}

# 4. hello 엔드포인트
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/user/hello | jq .
# → {"data":{"message":"Hello, testuser!"},"timestamp":"..."}
```

---

## User Story 3: RBAC 역할 기반 접근 제어

### 역할별 토큰 발급

```bash
# user 토큰
TOKEN_USER=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=demo-app" \
  -d "username=testuser" -d "password=user1234" \
  | jq -r '.access_token')

# manager 토큰
TOKEN_MANAGER=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=demo-app" \
  -d "username=testmanager" -d "password=manager1234" \
  | jq -r '.access_token')

# admin 토큰
TOKEN_ADMIN=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=demo-app" \
  -d "username=testadmin" -d "password=admin1234" \
  | jq -r '.access_token')
```

### RBAC 검증 명령

```bash
# admin 대시보드: user 토큰 → 403
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN_USER" \
  http://localhost:8081/api/admin/dashboard
# → 403

# admin 대시보드: admin 토큰 → 200
curl -s -H "Authorization: Bearer $TOKEN_ADMIN" \
  http://localhost:8081/api/admin/dashboard | jq .
# → {"data":{"totalUsers":3,"activeRoles":[...]},...}

# manager 사용자 목록: manager 토큰 → 200
curl -s -H "Authorization: Bearer $TOKEN_MANAGER" \
  http://localhost:8081/api/manager/users | jq .
# → {"data":[...],"timestamp":"..."}

# manager 사용자 목록: user 토큰 → 403
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN_USER" \
  http://localhost:8081/api/manager/users
# → 403
```

---

## User Story 4: JWT 내부 구조 시각화

### Token Inspection 검증

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=demo-app" \
  -d "username=testuser" -d "password=user1234" \
  | jq -r '.access_token')

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/token/inspect | jq .
```

확인 항목:
- `data.sub`: UUID 형식 사용자 식별자
- `data.iss`: `http://localhost:8080/realms/demo`
- `data.exp`: Unix timestamp
- `data.preferred_username`: `testuser`
- `data.realm_access.roles`: role 목록

---

## 전체 컨테이너 통합 실행

```bash
# 전체 스택 기동 (postgres → keycloak → backend 순서 자동 적용)
docker compose up -d

# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f backend
docker compose logs -f keycloak

# 전체 중지 및 데이터 초기화
docker compose down -v
```

### 기대 결과

| 서비스 | 포트 | 상태 확인 |
|--------|------|----------|
| PostgreSQL | 5432 | `docker compose ps` → `healthy` |
| Keycloak | 8080 | `curl http://localhost:8080/health/ready` → `UP` |
| Spring Boot | 8081 | `curl http://localhost:8081/api/public/health` → `200` |

---

## 자동화 테스트 실행

```bash
cd backend
./gradlew test
```
