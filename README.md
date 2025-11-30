# 인천대학교 공지사항 앱 백엔드

인천대학교 학생들을 위한 공지사항 통합 플랫폼의 백엔드 시스템입니다.

## 📊 현재 프로젝트 상태

✅ **Phase 1-2 완료** (2024-11-15)
- 전체 15개 핵심 API 구현 및 테스트 완료 (100% PASS)
- RESTful HTTP 상태 코드 개선 (409, 401, 400 등)
- Swagger UI 완전 설정 및 문서화
- 인증, 사용자, 북마크, 카테고리, 공지사항 기능 완료

🚀 **다음 단계**: Phase 3 - FCM 푸시 알림 시스템 구현

## 시스템 아키텍처

```
┌─────────────────┐
│  Flutter 앱     │ ◄─── 사용자 인터페이스
└────────┬────────┘
         │ REST API (JWT 인증)
         ▼
┌─────────────────┐
│  Spring Boot    │ ◄─── 메인 백엔드 (포트: 8080)
│  (Java 17)      │
└────┬────────┬───┘
     │        │
     │        └──────► Redis (캐싱)
     │
     │        ┌──────► PostgreSQL (데이터베이스)
     │        │
     └────────┼──────► FastAPI 크롤러 (포트: 8000)
              │        └─► BeautifulSoup (웹 크롤링)
              │
              └──────► Firebase (FCM 푸시 알림)
```

## 기술 스택

### 백엔드 (Spring Boot)
- **Framework**: Spring Boot 3.2.0
- **언어**: Java 17
- **데이터베이스**: PostgreSQL 16
- **ORM**: Spring Data JPA
- **캐싱**: Redis 7
- **인증**: JWT (JSON Web Token)
- **API 문서**: Swagger/OpenAPI
- **모니터링**: Prometheus + Grafana
- **빌드 도구**: Gradle

### 크롤링 서버 (FastAPI)
- **Framework**: FastAPI
- **언어**: Python 3.11
- **웹 크롤링**: BeautifulSoup4
- **HTTP 클라이언트**: httpx

### 배포
- **컨테이너**: Docker + Docker Compose
- **클라우드**: GCP (Google Cloud Platform)

## 프로젝트 구조

```
incheon-notice-backend/
├── src/main/java/com/incheon/notice/
│   ├── config/          # 설정 클래스 (Security, Swagger, Redis 등)
│   ├── controller/      # REST API 컨트롤러
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터베이스 접근
│   ├── entity/          # JPA 엔티티 (테이블 매핑)
│   ├── dto/             # API 요청/응답 객체
│   ├── security/        # JWT 인증 관련
│   └── util/            # 유틸리티 클래스
├── src/main/resources/
│   ├── application.yml         # 기본 설정
│   ├── application-dev.yml     # 개발 환경 설정
│   └── application-prod.yml    # 운영 환경 설정
├── crawler-service/     # FastAPI 크롤링 서버
│   ├── main.py         # FastAPI 애플리케이션
│   ├── crawler.py      # 크롤링 로직
│   └── requirements.txt
├── docker-compose.yml  # Docker Compose 설정
├── Dockerfile          # Spring Boot 이미지
└── README.md

## 주요 기능

### 1. 사용자 인증
- 회원가입 / 로그인
- JWT 기반 토큰 인증
- 토큰 갱신 (Refresh Token)

### 2. 공지사항 관리
- 공지사항 목록 조회 (페이징)
- 공지사항 검색 (제목, 내용)
- 카테고리별 필터링
- 중요 공지 / 상단 고정

### 3. 사용자 기능
- 북마크 저장/삭제
- 관심 카테고리 설정
- 푸시 알림 설정

### 4. 크롤링
- 주기적인 공지사항 수집
- 중복 방지 (외부 ID 기반)
- 실시간 업데이트

### 5. 푸시 알림
- Firebase Cloud Messaging (FCM)
- 새 공지사항 알림
- 카테고리별 맞춤 알림

## 설치 및 실행

### 1. 사전 요구사항

- **Docker Desktop** 설치 (필수)
  - Windows/Mac: https://www.docker.com/products/docker-desktop/
  - Docker가 설치되어 있으면 Java, Python 등을 별도로 설치할 필요 없습니다!

### 2. 프로젝트 클론

```bash
git clone <repository-url>
cd incheon-notice-backend
```

### 3. Docker로 전체 시스템 실행

```bash
# 모든 서비스 시작 (처음 실행 시 이미지 빌드로 시간 소요)
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 보기
docker-compose logs -f backend
docker-compose logs -f crawler
```

### 4. 서비스 접속

실행 후 다음 URL에서 서비스에 접속할 수 있습니다:

- **Swagger API 문서**: http://localhost:8080/swagger-ui.html ⭐ **API 테스트 추천**
- **백엔드 API**: http://localhost:8080/api
- **크롤러 API**: http://localhost:8000
- **pgAdmin (데이터베이스 관리)**: http://localhost:5050 (admin@admin.com/admin) ⭐ DB 관리 추천
- **Grafana 대시보드**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

> 💡 **Swagger UI 사용법**:
> 1. `/api/auth/login`으로 로그인하여 `accessToken` 획득
> 2. 우측 상단 **"Authorize"** 버튼 클릭
> 3. `Bearer {accessToken}` 입력 후 인증 완료
> 4. 모든 API를 브라우저에서 바로 테스트 가능!

> 💡 **pgAdmin 사용법**: 자세한 설정 방법은 `PGADMIN_GUIDE.md` 파일을 참고하세요!

### 5. 중지 및 삭제

```bash
# 서비스 중지
docker-compose stop

# 서비스 중지 및 컨테이너 삭제
docker-compose down

# 데이터까지 모두 삭제
docker-compose down -v
```

## 로컬 개발 (IDE 사용)

Docker 대신 로컬 환경에서 개발하려면:

### 1. 필요한 소프트웨어 설치

- **Java 17**: https://adoptium.net/
- **PostgreSQL 16**: https://www.postgresql.org/download/
- **Redis**: https://redis.io/download/
- **IntelliJ IDEA** (추천): https://www.jetbrains.com/idea/download/

### 2. 데이터베이스 생성

```sql
-- PostgreSQL에 접속하여 실행
CREATE DATABASE incheon_notice_dev;
```

### 3. 애플리케이션 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 정보를 확인하고 필요시 수정합니다.

### 4. Spring Boot 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 IncheonNoticeApplication.java 실행
```

### 5. FastAPI 크롤러 실행

```bash
cd crawler-service

# 가상환경 생성 (권장)
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 실행
uvicorn main:app --reload
```

## API 사용 예시

### 1. 회원가입

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "202012345",
    "email": "student@inu.ac.kr",
    "password": "password123",
    "name": "홍길동"
  }'
```

### 2. 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@inu.ac.kr",
    "password": "password123"
  }'
```

응답에서 `accessToken`을 복사하여 이후 요청에 사용합니다.

### 3. 공지사항 조회

```bash
curl -X GET http://localhost:8080/api/notices \
  -H "Authorization: Bearer <your-access-token>"
```

더 많은 API 예시는 Swagger 문서를 참고하세요!

## 🧪 API 테스트

### 자동화된 테스트 스크립트

프로젝트에는 3가지 테스트 스크립트가 포함되어 있습니다:

#### 1. 간단한 API 테스트 (5분)
```bash
chmod +x test_apis.sh
./test_apis.sh
```
기본적인 회원가입, 로그인, 북마크, 카테고리 구독 등을 테스트합니다.

#### 2. 종합 API 테스트 (15개 테스트)
```bash
chmod +x comprehensive_api_test.sh
./comprehensive_api_test.sh
```
모든 핵심 API를 체계적으로 테스트하고 결과를 출력합니다.

#### 3. HTTP 상태 코드 검증 테스트
```bash
chmod +x test_http_status_codes.sh
./test_http_status_codes.sh
```
RESTful API 표준에 맞게 HTTP 상태 코드가 반환되는지 검증합니다.

### 테스트 결과 (2024-11-15)

✅ **전체 15개 API 테스트: 100% PASS**

| 테스트 | HTTP 상태 | 결과 |
|--------|----------|------|
| 회원가입 | 201 Created | ✅ |
| 중복 회원가입 | 409 Conflict | ✅ |
| 로그인 | 200 OK | ✅ |
| 잘못된 비밀번호 로그인 | 401 Unauthorized | ✅ |
| 내 정보 조회 (인증 O) | 200 OK | ✅ |
| 내 정보 조회 (인증 X) | 403 Forbidden | ✅ |
| 사용자 설정 변경 | 200 OK | ✅ |
| 카테고리 목록 조회 | 200 OK | ✅ |
| 카테고리 구독 | 201 Created | ✅ |
| 내 구독 목록 조회 | 200 OK | ✅ |
| 북마크 목록 조회 | 200 OK | ✅ |
| 공지사항 목록 조회 | 200 OK | ✅ |
| 공지사항 검색 | 200 OK | ✅ |
| 토큰 갱신 | 200 OK | ✅ |
| Swagger UI 접근 | 302 Redirect | ✅ |

**상세 결과**: `API_TEST_RESULTS.md` 파일 참고

## 📝 주요 개선 사항

### HTTP 상태 코드 RESTful 표준 준수

비즈니스 로직 에러를 적절한 HTTP 상태 코드로 반환하도록 개선했습니다:

- **409 Conflict**: 중복 리소스 (이메일, 학번)
- **401 Unauthorized**: 인증 실패 (잘못된 비밀번호)
- **400 Bad Request**: 잘못된 요청 (Validation 실패, 비즈니스 규칙 위반)
- **403 Forbidden**: 권한 없음 (미인증 접근)

**Before (개선 전):**
```
중복 이메일로 회원가입 → HTTP 500 Internal Server Error ❌
```

**After (개선 후):**
```
중복 이메일로 회원가입 → HTTP 409 Conflict ✅
```

**상세 내용**: `HTTP_STATUS_CODES_FIX.md` 파일 참고

### Custom Exception 클래스 추가

```java
DuplicateResourceException.java   // 409 Conflict
InvalidCredentialsException.java  // 401 Unauthorized
BusinessException.java            // 400 Bad Request
```

### GlobalExceptionHandler 개선

모든 예외를 체계적으로 처리하여 일관된 에러 응답을 제공합니다.

## 크롤링 서버 사용법

### 특정 카테고리 크롤링

```bash
curl -X POST http://localhost:8000/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "category_code": "CS",
    "category_url": "https://www.incheon.ac.kr/...",
    "max_pages": 5
  }'
```

### 전체 카테고리 크롤링

```bash
curl -X POST http://localhost:8000/crawl/all
```

## 인천대학교 실제 URL 설정

`crawler-service/crawler.py` 파일에서 실제 인천대학교 공지사항 페이지의 HTML 구조를 분석하여 수정이 필요합니다:

1. 인천대학교 각 학과/부서의 공지사항 URL 확인
2. HTML 구조 분석 (개발자 도구 F12 사용)
3. `_parse_notice_row()` 메서드의 셀렉터 수정

```python
# 예시: 실제 HTML 구조에 맞게 수정
title_elem = cells[1].find('a')  # 제목이 있는 셀
author = cells[2].get_text(strip=True)  # 작성자가 있는 셀
```

## 환경변수 설정 (운영 환경)

운영 환경에서는 다음 환경변수를 설정해야 합니다:

```bash
# 데이터베이스
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/incheon_notice
export DATABASE_USERNAME=your-username
export DATABASE_PASSWORD=your-password

# Redis
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-password

# JWT (최소 256비트 이상의 안전한 키 사용)
export JWT_SECRET=your-super-secret-jwt-key-minimum-256-bits

# Firebase
export FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

## GCP 배포 가이드

### 1. Google Cloud Platform 설정

1. GCP 계정 생성: https://cloud.google.com/
2. 프로젝트 생성
3. 결제 계정 연결 (무료 크레딧 $300 사용 가능)

### 2. Cloud SQL (PostgreSQL) 생성

```bash
gcloud sql instances create incheon-notice-db \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region=asia-northeast3
```

### 3. Cloud Run 배포

```bash
# Docker 이미지 빌드
docker build -t gcr.io/your-project-id/incheon-notice-backend .

# Google Container Registry에 푸시
docker push gcr.io/your-project-id/incheon-notice-backend

# Cloud Run에 배포
gcloud run deploy incheon-notice-backend \
  --image gcr.io/your-project-id/incheon-notice-backend \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated
```

## 트러블슈팅

### Docker 빌드 실패

```bash
# 캐시 없이 다시 빌드
docker-compose build --no-cache
```

### 포트가 이미 사용중인 경우

```bash
# 사용중인 포트 확인 (Mac/Linux)
lsof -i :8080

# 프로세스 종료
kill -9 <PID>
```

### 데이터베이스 연결 실패

- Docker Compose 사용 시: 서비스가 모두 정상 실행되었는지 확인
- 로컬 개발 시: PostgreSQL이 실행중인지, 데이터베이스가 생성되었는지 확인

## 라이선스

MIT License

## 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.

---

**비전공자를 위한 추가 팁:**

1. **IntelliJ IDEA 사용**을 강력히 추천합니다. 코드 자동완성, 오류 감지 등이 매우 편리합니다.
2. **Swagger UI**를 활용하면 코드 없이 API를 테스트할 수 있습니다.
3. **Docker Desktop**을 사용하면 복잡한 설정 없이 전체 시스템을 실행할 수 있습니다.
4. 실제 운영 전에는 반드시 **보안 설정**(JWT 키, 데이터베이스 비밀번호 등)을 변경하세요!
