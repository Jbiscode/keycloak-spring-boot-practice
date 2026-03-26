# ⚠️ 로컬 학습 전용 크레덴셜

이 디렉토리의 `realm-export.json`에는 **학습 목적의 더미 크레덴셜**이 포함되어 있습니다.

## 절대 실제 환경에서 사용 금지

| 항목 | 값 | 용도 |
|------|----|------|
| testuser 비밀번호 | `user1234` | 로컬 학습 전용 |
| testmanager 비밀번호 | `manager1234` | 로컬 학습 전용 |
| testadmin 비밀번호 | `admin1234` | 로컬 학습 전용 |
| demo-service secret | `demo-service-secret` | 로컬 학습 전용 |
| Keycloak admin | `admin` / `admin` | 로컬 학습 전용 |

## 실제 환경 적용 시

1. 이 파일의 모든 비밀번호와 시크릿을 안전한 값으로 교체
2. `credentials` 블록을 제거하고 Keycloak Admin REST API로 초기화
3. `.gitignore`에 실제 realm 파일 추가
4. Keycloak Admin Console에서 `bruteForceProtected: true` 확인
