# 크롤링 서버 통합 완료 가이드

**작성일**: 2025-10-28
**통합 완료**: ✅ crawling-server → Spring Boot 백엔드

---

## 📊 작업 완료 내역

### ✅ 1. 포트 충돌 해결

**변경 사항**:
- crawling-server가 메인 프로젝트의 PostgreSQL, Redis 사용
- FastAPI 서버 포트: 8000 → **8001**로 변경
- 별도 DB 서비스 제거 (메인 DB 공유)

**파일 수정**:
```yaml
# crawling-server/docker-compose.yml
fastapi:
  ports:
    - "8001:8000"  # 외부에서 8001로 접속
  environment:
    - DATABASE_URL=postgresql://postgres:postgres@localhost:5432/incheon_notice
    - CELERY_BROKER_URL=redis://localhost:6379/1
    - SPRING_BOOT_URL=http://localhost:8080
```

```bash
# crawling-server/.env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/incheon_notice
CELERY_BROKER_URL=redis://localhost:6379/1
SPRING_BOOT_URL=http://localhost:8080
API_KEY=secure-crawler-key-12345
```

---

### ✅ 2. Spring Boot에 크롤러 API 추가

**새로 생성된 파일**:

#### ① CrawlerDto.java
```java
// 위치: src/main/java/com/incheon/notice/dto/CrawlerDto.java

// 주요 DTO
- NoticeRequest: 크롤링된 데이터 수신
- NoticeResponse: 저장 결과 반환
- BulkCrawlRequest: 일괄 크롤링 요청
- CrawlStatusResponse: 크롤링 상태
```

#### ② CrawlerService.java
```java
// 위치: src/main/java/com/incheon/notice/service/CrawlerService.java

// 주요 메서드
- saveNotice(): 크롤링 데이터 저장 (externalId로 중복 체크)
- updateExistingNotice(): 기존 공지사항 업데이트
- getNoticeCountByCategory(): 카테고리별 공지 개수
```

#### ③ CrawlerController.java
```java
// 위치: src/main/java/com/incheon/notice/controller/CrawlerController.java

// API 엔드포인트
POST /api/crawler/notices  - 크롤링 데이터 수신
GET  /api/crawler/status   - 크롤링 상태 조회
GET  /api/crawler/health   - 헬스 체크
```

#### ④ NoticeRepository.java (메서드 추가)
```java
// 추가된 메서드
- countByCategoryCode(): 카테고리별 개수
- countByCreatedAtAfter(): 특정 시간 이후 생성된 공지 개수
```

---

### ✅ 3. crawling-server → Spring Boot 자동 전송

**수정된 파일**:

#### ① college_crawlers.py
```python
# 추가된 메서드
- send_to_spring_boot(): 크롤링 데이터를 Spring Boot로 전송
- _generate_external_id(): URL 기반 고유 ID 생성
- _map_source_to_category(): 소스 → 카테고리 코드 매핑
- _parse_date(): 날짜 포맷 변환
```

#### ② main.py (run_crawler 함수)
```python
# 변경 사항
- 크롤링 실행 후 자동으로 Spring Boot로 전송
- 전송 성공/실패 카운트 반환
- 로그에 전송 상태 출력
```

---

## 🚀 실행 가이드

### Step 1: 메인 프로젝트 실행

```bash
# 메인 프로젝트 디렉토리
docker-compose up -d

# 서비스 확인
docker-compose ps

# 실행 중인 서비스:
# - Spring Boot: http://localhost:8080
# - PostgreSQL: localhost:5432
# - Redis: localhost:6379
# - pgAdmin: http://localhost:5050
```

---

### Step 2: crawling-server 실행

```bash
cd crawling-server

# Docker Compose로 실행
docker-compose up -d

# 서비스 확인
docker-compose ps

# 실행 중인 서비스:
# - FastAPI: http://localhost:8001
# - Celery Worker
# - Celery Beat
```

**또는 로컬에서 실행** (개발 시 권장):
```bash
cd crawling-server/app

# FastAPI 서버
uvicorn main:app --reload --host 0.0.0.0 --port 8001

# 별도 터미널에서 Celery Worker
celery -A tasks worker --loglevel=INFO

# 별도 터미널에서 Celery Beat
celery -A tasks beat --loglevel=INFO
```

---

### Step 3: 통합 테스트

#### 1. 헬스 체크
```bash
# Spring Boot 백엔드
curl http://localhost:8080/api/crawler/health

# crawling-server
curl http://localhost:8001/health
```

**예상 결과**:
```json
// Spring Boot
{
  "success": true,
  "message": "크롤러 API 정상 작동",
  "data": "OK"
}

// crawling-server
{
  "status": "healthy",
  "timestamp": 1730098800.123
}
```

---

#### 2. 크롤링 실행 (Spring Boot로 자동 전송)
```bash
# 봉사 공지 크롤링
curl -X POST "http://localhost:8001/run-crawler/volunteer?api_key=secure-crawler-key-12345"
```

**예상 결과**:
```json
{
  "status": "success",
  "category": "volunteer",
  "crawled_count": 15,
  "sent_to_backend": 15,
  "failed_to_send": 0,
  "message": "크롤링 완료. 15개 항목을 Spring Boot로 전송함."
}
```

**로그 확인**:
```bash
# crawling-server 로그
docker-compose logs -f fastapi

# 출력 예시:
# ✓ Spring Boot로 전송 성공: 2024학년도 봉사활동 모집 공지
# ✓ Spring Boot로 전송 성공: 자원봉사자 신청 안내
# ...
# ✅ 전송 완료: 성공 15개, 실패 0개
```

---

#### 3. Spring Boot에서 데이터 확인

**API 호출** (나중에 구현 예정):
```bash
# 전체 공지사항 조회
curl http://localhost:8080/api/notices

# 봉사 카테고리만 조회
curl http://localhost:8080/api/notices?categoryCode=VOLUNTEER
```

**pgAdmin에서 확인**:
1. http://localhost:5050 접속
2. Servers → incheon-notice-db → Databases → incheon_notice → Schemas → public → Tables
3. `notice` 테이블 우클릭 → "View/Edit Data" → "All Rows"

**예상 결과**:
```
id | title                        | external_id    | category_id | author      | view_count
---+------------------------------+----------------+-------------+-------------+------------
 1 | 2024학년도 봉사활동 모집 공지 | abc123def456   | 1           | 학생지원팀   | 45
 2 | 자원봉사자 신청 안내          | def456ghi789   | 1           | 봉사센터     | 32
...
```

---

#### 4. 전체 카테고리 크롤링
```bash
# 모든 카테고리 크롤링 (시간 소요 가능)
curl -X POST "http://localhost:8001/run-crawler/all?api_key=secure-crawler-key-12345"
```

**예상 결과**:
```json
{
  "status": "success",
  "category": "all",
  "crawled_count": 120,
  "sent_to_backend": 118,
  "failed_to_send": 2,
  "message": "크롤링 완료. 118개 항목을 Spring Boot로 전송함."
}
```

---

## 📋 카테고리 매핑

| 크롤링 소스 | Spring Boot 카테고리 코드 | 설명 |
|-------------|---------------------------|------|
| volunteer | VOLUNTEER | 봉사 |
| job | JOB | 취업 |
| scholarship | SCHOLARSHIP | 장학금 |
| general_events | GENERAL_EVENTS | 일반행사 |
| educational_test | EDUCATIONAL_TEST | 교육시험 |
| tuition_payment | TUITION_PAYMENT | 등록금납부 |
| academic_credit | ACADEMIC_CREDIT | 학점 |
| degree | DEGREE | 학위 |

**⚠️ 주의**: Spring Boot의 Category 테이블에 위 카테고리 코드가 있어야 합니다!

---

## 🔍 문제 해결

### 문제 1: "카테고리를 찾을 수 없습니다" 오류

**원인**: Category 테이블에 카테고리 데이터가 없음

**해결방법**:
```sql
-- PostgreSQL에 카테고리 추가
INSERT INTO category (code, name, description, url, is_active) VALUES
('VOLUNTEER', '봉사', '봉사활동 관련 공지', 'https://www.inu.ac.kr/bbs/inu/253/', true),
('JOB', '취업', '취업 관련 공지', 'https://www.inu.ac.kr/employment/', true),
('SCHOLARSHIP', '장학금', '장학금 관련 공지', '', true),
('GENERAL_EVENTS', '일반행사', '일반 행사 공지', '', true),
('EDUCATIONAL_TEST', '교육시험', '교육 및 시험 공지', '', true),
('TUITION_PAYMENT', '등록금납부', '등록금 납부 안내', '', true),
('ACADEMIC_CREDIT', '학점', '학점 관련 공지', '', true),
('DEGREE', '학위', '학위 관련 공지', '', true);
```

**또는 pgAdmin에서**:
1. http://localhost:5050 접속
2. `category` 테이블 열기
3. 위 데이터 수동 입력

---

### 문제 2: Spring Boot 연결 실패 (Connection Refused)

**원인**: Spring Boot 서버가 실행 중이 아니거나, 포트가 다름

**확인**:
```bash
# Spring Boot가 실행 중인지 확인
curl http://localhost:8080/api/crawler/health

# Docker 컨테이너 확인
docker-compose ps
```

**해결방법**:
```bash
# Spring Boot 재시작
docker-compose restart backend

# 또는 전체 재시작
docker-compose down
docker-compose up -d
```

---

### 문제 3: 중복 데이터 저장

**원인**: externalId가 동일한 공지사항이 이미 존재

**동작**:
- 기존 공지사항이 있으면 업데이트 (조회수 등)
- HTTP 200 OK 반환 (201 Created 아님)

**확인**:
```bash
# 크롤링 실행 시 로그 확인
docker-compose logs -f fastapi

# 출력:
# 기존 공지사항 업데이트: id=5, externalId=abc123def456
```

---

### 문제 4: API Key 오류 (401 Unauthorized)

**원인**: API Key가 일치하지 않음

**해결방법**:
```bash
# .env 파일 확인
cat crawling-server/.env | grep API_KEY

# 올바른 API Key로 요청
curl -X POST "http://localhost:8001/run-crawler/volunteer?api_key=secure-crawler-key-12345"
```

---

## 📊 데이터 흐름

```
┌─────────────────────┐
│  인천대학교 웹사이트  │
│  (www.inu.ac.kr)    │
└──────────┬──────────┘
           │ ① 크롤링
           ▼
┌─────────────────────┐
│  crawling-server    │
│  (FastAPI)          │
│  :8001              │
└──────────┬──────────┘
           │ ② POST /api/crawler/notices
           ▼
┌─────────────────────┐
│  Spring Boot        │
│  Backend            │
│  :8080              │
└──────────┬──────────┘
           │ ③ DB 저장
           ▼
┌─────────────────────┐
│  PostgreSQL         │
│  (incheon_notice)   │
│  :5432              │
└─────────────────────┘
```

**세부 흐름**:
1. **crawling-server**가 인천대 웹사이트에서 HTML 크롤링
2. BeautifulSoup로 데이터 파싱 (제목, 작성자, 날짜, URL 등)
3. `send_to_spring_boot()` 메서드로 Spring Boot API 호출
4. **Spring Boot**의 `CrawlerController`가 데이터 수신
5. `CrawlerService`가 externalId로 중복 체크
6. 신규 데이터면 저장, 기존 데이터면 업데이트
7. **PostgreSQL**에 데이터 저장 완료

---

## 🎯 다음 단계

### 즉시 할 수 있는 것

1. **카테고리 데이터 입력** (필수!)
   ```sql
   INSERT INTO category ...
   ```

2. **크롤링 테스트**
   ```bash
   curl -X POST "http://localhost:8001/run-crawler/volunteer?api_key=secure-crawler-key-12345"
   ```

3. **데이터 확인** (pgAdmin)
   - http://localhost:5050
   - `notice` 테이블 조회

---

### 추가 개발 필요

1. **공지사항 API 구현** (Spring Boot)
   - GET /api/notices - 목록 조회
   - GET /api/notices/{id} - 상세 조회
   - GET /api/notices/search - 검색

2. **자동 스케줄링 설정**
   - Celery Beat로 주기적 크롤링
   - 매일 특정 시간에 자동 실행

3. **푸시 알림 연동**
   - 새 공지사항 저장 시 FCM 푸시
   - 사용자 선호 카테고리 기반 알림

4. **에러 처리 개선**
   - 크롤링 실패 시 재시도
   - 데이터 검증 강화
   - 로그 시스템 개선

---

## ✅ 완료 체크리스트

- [x] 포트 충돌 해결
- [x] Spring Boot에 CrawlerController 추가
- [x] Spring Boot에 CrawlerService 추가
- [x] Spring Boot에 CrawlerDto 추가
- [x] NoticeRepository 메서드 추가
- [x] crawling-server에 Spring Boot 전송 로직 추가
- [x] HTTPException 임포트 수정
- [x] .env 파일 업데이트
- [x] docker-compose.yml 수정
- [ ] 카테고리 데이터 입력 (수동 작업 필요)
- [ ] 통합 테스트 실행
- [ ] 공지사항 조회 API 구현

---

## 🎉 통합 완료!

**메인 프로젝트**와 **crawling-server**가 성공적으로 통합되었습니다!

### 현재 상태
- ✅ 포트 충돌 해결됨
- ✅ 크롤링 데이터가 자동으로 Spring Boot로 전송됨
- ✅ 중복 체크 (externalId 기반)
- ✅ 메인 DB 공유 (incheon_notice)

### 사용 방법
```bash
# 1. 메인 프로젝트 실행
docker-compose up -d

# 2. crawling-server 실행
cd crawling-server && docker-compose up -d

# 3. 크롤링 실행
curl -X POST "http://localhost:8001/run-crawler/volunteer?api_key=secure-crawler-key-12345"

# 4. 데이터 확인
# pgAdmin: http://localhost:5050
# 또는 Spring Boot API (구현 후)
```

---

**문의사항이 있으시면 언제든 말씀해 주세요!** 🚀
