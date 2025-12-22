# 보안 가이드

## 목차
- [환경 설정 보안](#환경-설정-보안)
- [API 키 관리](#api-키-관리)
- [데이터베이스 보안](#데이터베이스-보안)
- [프로덕션 배포 체크리스트](#프로덕션-배포-체크리스트)
- [보안 모범 사례](#보안-모범-사례)

---

## 환경 설정 보안

### 1. .env 파일 설정

⚠️ **중요**: `.env` 파일은 **절대 Git에 커밋하지 마세요!**

#### 초기 설정
```bash
# .env.example을 복사하여 .env 파일 생성
cp .env.example .env

# .env 파일 편집
vi .env
```

#### 필수 변경 사항

##### 1) SECRET_KEY 생성
```bash
# 64자리 랜덤 시크릿 키 생성
python -c "import secrets; print(secrets.token_urlsafe(64))"

# 생성된 키를 .env 파일에 설정
SECRET_KEY=<생성된_키>
```

##### 2) API_KEY 생성
```bash
# 32자리 랜덤 API 키 생성
python -c "import secrets; print(secrets.token_urlsafe(32))"

# 생성된 키를 .env 파일에 설정
API_KEY=<생성된_키>
```

##### 3) 데이터베이스 비밀번호 변경
```bash
# 강력한 비밀번호 생성 (특수문자, 숫자, 대소문자 포함)
python -c "import secrets, string; print(''.join(secrets.choice(string.ascii_letters + string.digits + '!@#$%^&*') for i in range(20)))"

# .env 파일에서 변경
POSTGRES_PASSWORD=<생성된_강력한_비밀번호>
DATABASE_URL=postgresql://crawler:<생성된_강력한_비밀번호>@postgres:5432/school_notices
```

---

## API 키 관리

### API 키 사용법

#### 1. API 요청 시 헤더에 포함
```bash
curl -X POST \
  -H "X-API-Key: your-api-key-here" \
  http://localhost:8000/run-crawler/volunteer
```

#### 2. Python에서 사용
```python
import requests

headers = {"X-API-Key": "your-api-key-here"}
response = requests.post(
    "http://localhost:8000/run-crawler/volunteer",
    headers=headers
)
```

### API 키 검증 메커니즘

시스템은 다음과 같은 보안 메커니즘을 사용합니다:

1. **Constant-time 비교**: 타이밍 공격 방지
2. **로깅**: 잘못된 API 키 시도 기록
3. **401/403 응답**:
   - 401: API 키 미제공
   - 403: 잘못된 API 키

---

## 데이터베이스 보안

### 1. PostgreSQL 비밀번호 관리

#### 비밀번호 변경 (Docker 컨테이너)
```bash
# PostgreSQL 컨테이너 접속
docker exec -it college_noti-postgres-1 psql -U crawler -d school_notices

# 비밀번호 변경
ALTER USER crawler WITH PASSWORD 'new_strong_password';

# .env 파일도 함께 업데이트
POSTGRES_PASSWORD=new_strong_password
DATABASE_URL=postgresql://crawler:new_strong_password@postgres:5432/school_notices
```

### 2. pgAdmin 보안

⚠️ **프로덕션 환경에서는 pgAdmin을 외부 노출하지 마세요!**

개발 환경에서만 사용:
```yaml
# docker-compose.yml에서 pgAdmin 포트를 localhost에만 바인딩
pgadmin:
  ports:
    - "127.0.0.1:5050:80"  # 로컬호스트에서만 접근 가능
```

또는 프로덕션에서는 완전히 제거:
```bash
# docker-compose.yml에서 pgadmin 서비스 주석 처리 또는 제거
```

---

## 프로덕션 배포 체크리스트

### 필수 보안 설정

- [ ] **SECRET_KEY 변경** (최소 64자)
- [ ] **API_KEY 변경** (최소 32자)
- [ ] **데이터베이스 비밀번호 변경** (강력한 비밀번호)
- [ ] **ENV=production 설정**
- [ ] **DEBUG=false 설정**
- [ ] **pgAdmin/Adminer 비활성화 또는 제거**
- [ ] **ALLOWED_ORIGINS 설정** (실제 도메인으로)
- [ ] **TRUSTED_HOSTS 설정** (실제 도메인으로)
- [ ] **HTTPS 활성화** (리버스 프록시 설정)
- [ ] **방화벽 설정** (필요한 포트만 열기)
- [ ] **Sentry DSN 설정** (에러 추적)
- [ ] **로그 모니터링 설정**

### 배포 전 검증

```bash
# 1. 설정 검증
docker-compose exec fastapi python -c "from app.config import validate_settings; validate_settings()"

# 2. 보안 헤더 확인
curl -I http://localhost:8000/health

# 예상 출력:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 1; mode=block
# Strict-Transport-Security: max-age=31536000

# 3. API 키 검증 확인
curl http://localhost:8000/run-crawler/volunteer
# 예상: 401 Unauthorized

curl -H "X-API-Key: wrong-key" http://localhost:8000/run-crawler/volunteer
# 예상: 403 Forbidden
```

---

## 보안 모범 사례

### 1. 비밀번호 정책

- **최소 길이**: 20자 이상
- **복잡도**: 대소문자, 숫자, 특수문자 포함
- **주기적 변경**: 3-6개월마다
- **재사용 금지**: 이전 비밀번호 재사용 금지

### 2. API 키 관리

```bash
# ✅ 좋은 예시 (환경 변수 사용)
API_KEY=$(cat /run/secrets/api_key)

# ❌ 나쁜 예시 (코드에 하드코딩)
API_KEY = "dev-api-key-12345"  # 절대 이렇게 하지 마세요!
```

### 3. 로그 관리

민감한 정보는 로그에 남기지 않습니다:

```python
# ✅ 좋은 예시
logger.info(f"API key attempt: {api_key[:8]}...")

# ❌ 나쁜 예시
logger.info(f"API key received: {api_key}")  # 전체 키 노출!
```

### 4. CORS 설정

```python
# ✅ 좋은 예시 (특정 도메인만 허용)
ALLOWED_ORIGINS=https://your-domain.com,https://admin.your-domain.com

# ❌ 나쁜 예시 (모든 도메인 허용)
ALLOWED_ORIGINS=*  # 절대 이렇게 하지 마세요!
```

### 5. Docker Secrets (프로덕션 권장)

Docker Swarm 또는 Kubernetes Secrets 사용:

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  fastapi:
    secrets:
      - db_password
      - api_key
      - secret_key
    environment:
      DATABASE_URL: postgresql://crawler:run/secrets/db_password@postgres:5432/school_notices

secrets:
  db_password:
    external: true
  api_key:
    external: true
  secret_key:
    external: true
```

---

## 취약점 보고

보안 취약점을 발견하셨나요?

1. **공개 이슈 생성 금지**: GitHub Issues에 공개하지 마세요
2. **이메일로 보고**: [보안팀 이메일 주소]
3. **상세 정보 포함**:
   - 취약점 설명
   - 재현 단계
   - 영향 범위
   - (선택) 수정 제안

---

## 보안 업데이트 내역

### 2025-11-02
- ✅ API 키 검증 강화 (constant-time 비교)
- ✅ .env.example 템플릿 추가
- ✅ docker-compose.yml 환경 변수 분리
- ✅ 설정 검증 로직 추가
- ✅ 보안 헤더 미들웨어 구현
- ✅ Rate limiting 구현
- ✅ Health checks 추가

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [FastAPI Security](https://fastapi.tiangolo.com/tutorial/security/)
- [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/auth-pg-hba-conf.html)
