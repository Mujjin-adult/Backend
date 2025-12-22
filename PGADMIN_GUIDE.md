# pgAdmin으로 PostgreSQL 데이터베이스 확인하기

## 📌 pgAdmin 접속

1. **웹 브라우저 열기** (Chrome, Safari 등)

2. **주소창에 입력:**
   ```
   http://localhost:5050
   ```

3. **로그인 정보:**
   - **Email**: `admin@admin.com`
   - **Password**: `admin`
x
---

## 🗄️ 데이터베이스 서버 연결 설정

### 1단계: 서버 추가

pgAdmin에 처음 접속하면 왼쪽에 "Servers"가 보입니다.

1. 왼쪽 사이드바에서 **"Servers"** 우클릭
2. **"Register" → "Server..."** 클릭

### 2단계: General 탭 설정

**"Create - Server" 창이 열립니다**

**General 탭:**
- **Name**: `인천대학교 공지사항 DB` (원하는 이름)

### 3단계: Connection 탭 설정

**Connection 탭 클릭 후 다음 정보 입력:**

- **Host name/address**: `postgres` ⭐ (중요! localhost가 아님)
- **Port**: `5432`
- **Maintenance database**: `incheon_notice`
- **Username**: `postgres`
- **Password**: `postgres`
- ☑️ **Save password** 체크 (선택사항 - 비밀번호 저장)

### 4단계: 저장

- **"Save"** 버튼 클릭

---

## 📊 데이터베이스 테이블 확인하기

### 테이블 찾기

연결이 완료되면 왼쪽 트리 구조에서:

```
Servers
└── 인천대학교 공지사항 DB
    └── Databases
        └── incheon_notice
            └── Schemas
                └── public
                    └── Tables  ← 여기!
```

### 주요 테이블

- **users** - 사용자 정보
- **categories** - 카테고리 (학과, 대학 등)
- **notices** - 공지사항
- **bookmarks** - 북마크
- **user_preferences** - 사용자 알림 설정
- **notification_history** - 푸시 알림 이력

### 테이블 데이터 보기

1. 원하는 테이블 (예: `users`) 우클릭
2. **"View/Edit Data" → "All Rows"** 클릭
3. 오른쪽에 데이터가 표시됩니다!

### SQL 쿼리 실행하기

1. 왼쪽에서 `incheon_notice` 데이터베이스 클릭
2. 상단 메뉴에서 **"Tools" → "Query Tool"** 클릭 (또는 ⚡ 번개 아이콘)
3. SQL 쿼리 입력 후 실행 (▶️ 버튼)

**예시 쿼리:**

```sql
-- 모든 사용자 조회
SELECT * FROM users;

-- 사용자 수 확인
SELECT COUNT(*) FROM users;

-- 최근 가입한 사용자 5명
SELECT id, email, name, created_at
FROM users
ORDER BY created_at DESC
LIMIT 5;

-- 특정 사용자 검색
SELECT * FROM users WHERE email = 'test@inu.ac.kr';

-- 모든 테이블 목록
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public';
```

---

## 🎯 자주 사용하는 기능

### 1. 데이터 편집

- 테이블에서 데이터 보기 상태에서
- 셀을 더블클릭하면 편집 가능
- 상단의 **"Save"** 버튼으로 저장

### 2. 데이터 추가

- 데이터 보기 화면 상단의 **"+"** 버튼 클릭
- 새 행이 추가되며 값 입력 가능
- **"Save"** 버튼으로 저장

### 3. 데이터 삭제

- 삭제할 행 선택
- 상단의 **"🗑️ (휴지통)"** 버튼 클릭
- **"Save"** 버튼으로 저장

### 4. 데이터 필터링

- 컬럼 헤더의 🔽 버튼 클릭
- 필터 조건 입력

### 5. 데이터 정렬

- 컬럼 헤더 클릭하면 오름차순/내림차순 정렬

---

## 💡 유용한 팁

### 테이블 구조 보기

테이블 우클릭 → **"Properties"** → **"Columns"** 탭
- 각 컬럼의 데이터 타입, NULL 허용 여부 등 확인 가능

### 백업 만들기

1. `incheon_notice` 데이터베이스 우클릭
2. **"Backup..."** 클릭
3. 파일명 지정 후 **"Backup"** 클릭

### 테이블 새로고침

- 테이블 우클릭 → **"Refresh"**
- 또는 F5 키

---

## 🚨 문제 해결

### 연결 실패: "could not translate host name"

**원인:** Host name을 `localhost`로 입력했을 경우

**해결:** Host name을 `postgres`로 변경
- Docker 네트워크 내에서는 서비스 이름을 사용해야 합니다

### 비밀번호 오류

**확인사항:**
- Username: `postgres`
- Password: `postgres`
- Database: `incheon_notice`

### pgAdmin이 열리지 않음

**확인:**
```bash
# 서비스 상태 확인
docker-compose ps

# pgAdmin 로그 확인
docker-compose logs pgadmin

# 재시작
docker-compose restart pgadmin
```

---

## 📸 스크린샷 가이드 (참고)

### 1. 서버 등록 화면
```
Connection 탭에서:
Host: postgres
Port: 5432
Database: incheon_notice
Username: postgres
Password: postgres
```

### 2. 테이블 데이터 보기
```
Tables → users → 우클릭 → View/Edit Data → All Rows
```

### 3. SQL 쿼리 실행
```
상단 Tools → Query Tool
또는 번개 아이콘 ⚡ 클릭
```

---

## 🎓 추가 학습 자료

- **pgAdmin 공식 문서**: https://www.pgadmin.org/docs/
- **PostgreSQL 공식 문서**: https://www.postgresql.org/docs/

---

## 📝 요약

1. **접속**: http://localhost:5050
2. **로그인**: admin@admin.com / admin
3. **서버 추가**: Host=`postgres`, Database=`incheon_notice`, User=`postgres`, Password=`postgres`
4. **테이블 확인**: Servers → DB → Schemas → public → Tables
5. **데이터 보기**: 테이블 우클릭 → View/Edit Data → All Rows

---

**즐거운 데이터베이스 관리 되세요! 🚀**
