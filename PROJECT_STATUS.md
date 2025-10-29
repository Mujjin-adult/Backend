# 프로젝트 현황 및 향후 작업 계획

인천대학교 공지사항 앱 백엔드 개발 현황을 정리한 문서입니다.

---

## ✅ 현재까지 완성된 것들

### 1. 인프라 및 기본 설정 (100% 완료)

#### Docker 환경
- ✅ Spring Boot 백엔드 컨테이너
- ✅ PostgreSQL 16 데이터베이스
- ✅ Redis 7 캐싱 서버
- ✅ FastAPI 크롤러 서버
- ✅ pgAdmin 4 (데이터베이스 관리 도구)
- ✅ Prometheus + Grafana (모니터링)

#### Spring Boot 설정
- ✅ Spring Security 설정 (JWT 인증)
- ✅ Swagger/OpenAPI 문서화
- ✅ Redis 캐시 설정
- ✅ JPA/Hibernate 설정
- ✅ CORS 설정
- ✅ Profile 설정 (dev/prod)

---

### 2. 데이터베이스 (100% 완료)

#### Entity 클래스 (6개 모두 완성)
- ✅ **User** - 사용자 (학번, 이메일, 비밀번호, FCM 토큰)
- ✅ **Category** - 카테고리 (학과/부서 구분)
- ✅ **Notice** - 공지사항 (제목, 내용, URL, 작성자, 조회수 등)
- ✅ **Bookmark** - 북마크 (사용자-공지사항 관계)
- ✅ **UserPreference** - 사용자 선호 카테고리
- ✅ **NotificationHistory** - 푸시 알림 전송 이력

#### Repository 인터페이스 (6개 모두 완성)
- ✅ UserRepository
- ✅ CategoryRepository
- ✅ NoticeRepository
- ✅ BookmarkRepository
- ✅ UserPreferenceRepository
- ✅ NotificationHistoryRepository

---

### 3. 인증 시스템 (100% 완료)

#### JWT 인증
- ✅ **JwtTokenProvider** - JWT 토큰 생성/검증
- ✅ **JwtAuthenticationFilter** - 요청마다 JWT 검증
- ✅ **CustomUserDetailsService** - Spring Security 연동
- ✅ Access Token (24시간) + Refresh Token (7일)

#### 인증 API
- ✅ **POST /api/auth/signup** - 회원가입
- ✅ **POST /api/auth/login** - 로그인 (FCM 토큰 선택적)
- ✅ **POST /api/auth/refresh** - 토큰 갱신

#### DTO
- ✅ AuthDto (SignUpRequest, LoginRequest, LoginResponse, RefreshTokenRequest, UserResponse)
- ✅ ApiResponse (통일된 응답 형식)

---

### 4. 크롤링 서버 (기본 구조 완료)

#### FastAPI 서버
- ✅ **GET /** - 헬스 체크
- ✅ **POST /crawl** - 특정 카테고리 크롤링
- ✅ **POST /crawl/all** - 전체 카테고리 크롤링
- ✅ BeautifulSoup 크롤러 기본 구조
- ✅ Spring Boot 백엔드로 데이터 전송 로직

---

### 5. 문서화 (100% 완료)

- ✅ **README.md** - 프로젝트 전체 설명
- ✅ **SWAGGER_GUIDE.md** - Swagger UI 사용법
- ✅ **FCM_GUIDE.md** - FCM 토큰 가이드
- ✅ **PGADMIN_GUIDE.md** - pgAdmin 사용법
- ✅ **test-all-apis.sh** - API 자동 테스트 스크립트

---

## 🚧 앞으로 만들어야 할 것들

### 1. 공지사항 API (우선순위: 높음 ⭐⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
├── controller/
│   └── NoticeController.java          [❌ 미구현]
├── service/
│   └── NoticeService.java              [❌ 미구현]
└── dto/
    └── NoticeDto.java                  [✅ 완료]
```

#### 구현해야 할 API

**공개 API (인증 불필요)**
- ❌ **GET /api/notices** - 공지사항 목록 조회 (페이징, 정렬)
  - Query Params: page, size, sort, categoryCode
- ❌ **GET /api/notices/{id}** - 공지사항 상세 조회
- ❌ **GET /api/notices/search** - 공지사항 검색
  - Query Params: keyword, categoryCode, startDate, endDate

**관리자 API (인증 필요, 향후 구현)**
- ❌ **PUT /api/notices/{id}** - 공지사항 수정
- ❌ **DELETE /api/notices/{id}** - 공지사항 삭제

#### 주요 기능
- 페이징 처리 (Spring Data JPA Pageable)
- 조회수 증가 (Redis 캐싱)
- 중요 공지 우선 표시
- 검색 (제목, 내용, 작성자)
- 카테고리별 필터링

---

### 2. 카테고리 API (우선순위: 높음 ⭐⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
├── controller/
│   └── CategoryController.java         [❌ 미구현]
├── service/
│   └── CategoryService.java            [❌ 미구현]
└── dto/
    └── CategoryDto.java                [✅ 완료]
```

#### 구현해야 할 API

- ❌ **GET /api/categories** - 전체 카테고리 목록
- ❌ **GET /api/categories/{code}** - 특정 카테고리 상세
- ❌ **GET /api/categories/active** - 활성화된 카테고리만 조회

**관리자 API (향후 구현)**
- ❌ **POST /api/categories** - 카테고리 생성
- ❌ **PUT /api/categories/{code}** - 카테고리 수정
- ❌ **DELETE /api/categories/{code}** - 카테고리 삭제

#### 초기 데이터 설정
- ❌ 인천대학교 학과/부서별 카테고리 데이터 입력
  - 컴퓨터공학과, 전자공학과, 경영학과 등
  - 학사공지, 장학공지, 취업공지 등
- ❌ 각 카테고리의 실제 공지사항 URL 설정

---

### 3. 북마크 API (우선순위: 중간 ⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
├── controller/
│   └── BookmarkController.java         [❌ 미구현]
├── service/
│   └── BookmarkService.java            [❌ 미구현]
└── dto/
    └── BookmarkDto.java                [✅ 완료]
```

#### 구현해야 할 API (모두 인증 필요)

- ❌ **GET /api/bookmarks** - 내 북마크 목록 조회
- ❌ **POST /api/bookmarks** - 북마크 추가
  - Body: `{ "noticeId": 123 }`
- ❌ **DELETE /api/bookmarks/{id}** - 북마크 삭제
- ❌ **GET /api/bookmarks/exists/{noticeId}** - 북마크 여부 확인

#### 주요 기능
- JWT 토큰으로 사용자 식별
- 중복 북마크 방지
- 북마크한 공지사항 삭제 시 처리

---

### 4. 사용자 선호 설정 API (우선순위: 중간 ⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
├── controller/
│   └── UserPreferenceController.java   [❌ 미구현]
├── service/
│   └── UserPreferenceService.java      [❌ 미구현]
└── dto/
    └── UserPreferenceDto.java          [❌ 미구현]
```

#### 구현해야 할 API (모두 인증 필요)

- ❌ **GET /api/preferences** - 내 선호 카테고리 조회
- ❌ **POST /api/preferences** - 선호 카테고리 추가
  - Body: `{ "categoryCode": "CS", "notificationEnabled": true }`
- ❌ **DELETE /api/preferences/{categoryCode}** - 선호 카테고리 삭제
- ❌ **PUT /api/preferences/{categoryCode}** - 알림 설정 변경

#### 주요 기능
- 관심 카테고리 설정
- 카테고리별 알림 on/off
- 맞춤형 공지사항 필터링

---

### 5. 크롤러 연동 API (우선순위: 높음 ⭐⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
├── controller/
│   └── CrawlerController.java          [❌ 미구현]
└── service/
    └── CrawlerService.java             [❌ 미구현]
```

#### 구현해야 할 API

- ❌ **POST /api/crawler/notices** - FastAPI에서 보낸 공지사항 저장
  - 중복 체크 (externalId 기준)
  - 새 공지사항일 경우 푸시 알림 트리거
- ❌ **GET /api/crawler/categories** - 활성 카테고리 목록 반환
  - FastAPI가 크롤링할 카테고리 정보 제공

#### 주요 기능
- externalId로 중복 방지
- 대량 데이터 저장 최적화
- 크롤링 실패 시 로그 기록

---

### 6. 푸시 알림 서비스 (우선순위: 중간 ⭐⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
└── service/
    └── FcmService.java                 [❌ 미구현]
```

#### 구현해야 할 기능

- ❌ Firebase Admin SDK 초기화
  - `firebase-credentials.json` 파일 읽기
- ❌ 개별 사용자에게 푸시 알림 전송
  - `sendNotification(String fcmToken, String title, String body)`
- ❌ 특정 카테고리 구독자 전체에게 푸시
  - `sendToCategory(String categoryCode, Notice notice)`
- ❌ 푸시 전송 성공/실패 로그
  - NotificationHistory에 기록

#### Firebase 설정 필요
- ❌ Firebase 프로젝트 생성
- ❌ Android/iOS 앱 등록
- ❌ 서비스 계정 키 다운로드 (`firebase-credentials.json`)
- ❌ 환경변수 설정: `FCM_CREDENTIALS_PATH`

---

### 7. 실제 크롤링 로직 구현 (우선순위: 높음 ⭐⭐⭐)

#### crawler-service/crawler.py 수정 필요

- ❌ **인천대학교 실제 공지사항 페이지 분석**
  - 각 학과/부서 공지사항 URL 확인
  - HTML 구조 분석 (Chrome 개발자 도구)
  - 테이블/리스트 구조 파악

- ❌ **`_parse_notice_row()` 메서드 수정**
  ```python
  # 현재: 예시 코드
  title_elem = cells[1].find('a')
  author = cells[2].get_text(strip=True)

  # 수정 필요: 실제 HTML 구조에 맞게
  title_elem = row.find('td', class_='title').find('a')
  author = row.find('td', class_='writer').get_text(strip=True)
  ```

- ❌ **페이징 처리**
  - 다음 페이지 버튼 찾기
  - 페이지 번호 파라미터 처리

- ❌ **상세 내용 크롤링**
  - 목록에서 URL만 가져오고, 상세 페이지 접속
  - 본문 내용 추출
  - 첨부파일 링크 추출

---

### 8. 스케줄링 (우선순위: 낮음 ⭐)

#### 필요한 파일
```
src/main/java/com/incheon/notice/
└── scheduler/
    └── NoticeScheduler.java            [❌ 미구현]
```

#### 구현해야 할 기능

- ❌ **주기적 크롤링**
  - `@Scheduled(cron = "0 0 * * * *")` - 매 시간마다
  - FastAPI 크롤러에게 크롤링 요청
- ❌ **오래된 공지사항 자동 삭제**
  - 90일 이상 지난 공지사항 삭제
- ❌ **통계 생성**
  - 일일 공지사항 수, 카테고리별 통계

---

### 9. 기타 개선사항 (우선순위: 낮음 ⭐)

#### 보안
- ❌ 비밀번호 정책 강화 (최소 길이, 특수문자 등)
- ❌ 이메일 인증 추가
- ❌ 비밀번호 재설정 기능
- ❌ 로그인 실패 횟수 제한

#### 성능
- ❌ Redis 캐싱 확대 (공지사항 목록, 카테고리)
- ❌ 데이터베이스 인덱스 최적화
- ❌ N+1 쿼리 문제 해결

#### 관리자 기능
- ❌ 관리자 전용 API
- ❌ 통계 대시보드
- ❌ 사용자 관리

---

## 📋 우선순위별 작업 순서 추천

### Phase 1: 핵심 기능 (1-2주)
1. ✅ ~~인증 API~~ (완료)
2. **카테고리 API** 구현
   - 초기 데이터 입력
3. **공지사항 API** 구현
   - 목록, 상세, 검색
4. **크롤러 연동 API** 구현

### Phase 2: 크롤링 (1주)
5. **실제 인천대 URL 분석**
6. **크롤링 로직 구현**
7. **테스트 및 디버깅**

### Phase 3: 사용자 기능 (1주)
8. **북마크 API** 구현
9. **사용자 선호 설정 API** 구현

### Phase 4: 푸시 알림 (1주)
10. **Firebase 프로젝트 설정**
11. **FCM 서비스 구현**
12. **Flutter 앱에서 테스트**

### Phase 5: 최적화 및 배포 (1주)
13. **스케줄링** 구현
14. **캐싱 최적화**
15. **GCP 배포**

---

## 🎯 다음 단계 시작하기

### 가장 먼저 만들어야 할 것: 카테고리 API + 공지사항 API

이 두 가지를 만들면:
- ✅ 공지사항을 조회할 수 있습니다
- ✅ 카테고리별로 필터링할 수 있습니다
- ✅ Flutter 앱에서 데이터를 받아 화면에 표시할 수 있습니다

### 시작 명령어

다음과 같이 요청하시면 됩니다:

```
"카테고리 API를 만들어줘"
또는
"공지사항 API를 만들어줘"
```

---

## 📊 전체 진행률

| 영역 | 완료 | 미완료 | 진행률 |
|------|------|--------|--------|
| 인프라 | 7 | 0 | 100% |
| 데이터베이스 | 12 | 0 | 100% |
| 인증 시스템 | 8 | 0 | 100% |
| 공지사항 API | 1 | 4 | 20% |
| 카테고리 API | 1 | 4 | 20% |
| 북마크 API | 1 | 4 | 20% |
| 선호설정 API | 0 | 5 | 0% |
| 크롤러 연동 | 1 | 3 | 25% |
| 푸시 알림 | 0 | 4 | 0% |
| 스케줄링 | 0 | 3 | 0% |

**전체 진행률: 약 35%**

---

현재까지 **인증 시스템과 인프라**는 완벽하게 구축되었습니다! 🎉

이제 **실제 서비스 기능**들을 하나씩 추가하면 됩니다.
