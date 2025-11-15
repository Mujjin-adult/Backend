# HTTP 상태 코드 개선 완료 ✅

## 문제 상황

사용자가 `/api/auth/signup` API에서 400 에러를 보고했으나, 실제로는 다음과 같은 문제가 있었습니다:

1. **RESTful하지 않은 HTTP 상태 코드**
   - 중복 이메일/학번 → 500 반환 (올바름: 409 Conflict)
   - 잘못된 이메일 도메인 → 500 반환 (올바름: 400 Bad Request)
   - 로그인 실패 → 500 반환 (올바름: 401 Unauthorized)

2. **400 에러는 정상 동작**
   - Validation 실패 시 400 반환은 올바른 동작
   - 필수 필드 누락, 잘못된 형식 등

## 해결 방법

### 1. Custom Exception 클래스 생성

**DuplicateResourceException.java** - HTTP 409 Conflict
```java
public class DuplicateResourceException extends RuntimeException {
    // 중복 리소스 (이메일, 학번 등)
}
```

**InvalidCredentialsException.java** - HTTP 401 Unauthorized
```java
public class InvalidCredentialsException extends RuntimeException {
    // 인증 실패 (잘못된 비밀번호 등)
}
```

**BusinessException.java** - HTTP 400 Bad Request
```java
public class BusinessException extends RuntimeException {
    // 비즈니스 로직 위반 (잘못된 이메일 도메인 등)
}
```

### 2. GlobalExceptionHandler 업데이트

```java
@ExceptionHandler(DuplicateResourceException.class)
public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
        DuplicateResourceException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)  // 409
            .body(ApiResponse.error(ex.getMessage()));
}

@ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(
        Exception ex) {
    return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)  // 401
            .body(ApiResponse.error("이메일 또는 비밀번호가 올바르지 않습니다"));
}

@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(
        BusinessException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)  // 400
            .body(ApiResponse.error(ex.getMessage()));
}
```

### 3. AuthService 수정

```java
// 변경 전
if (!request.getEmail().endsWith("@inu.ac.kr")) {
    throw new RuntimeException("인천대학교 이메일(@inu.ac.kr)만 사용 가능합니다");  // 500
}

if (userRepository.existsByEmail(request.getEmail())) {
    throw new RuntimeException("이미 사용중인 이메일입니다");  // 500
}

// 변경 후
if (!request.getEmail().endsWith("@inu.ac.kr")) {
    throw new BusinessException("인천대학교 이메일(@inu.ac.kr)만 사용 가능합니다");  // 400
}

if (userRepository.existsByEmail(request.getEmail())) {
    throw new DuplicateResourceException("이미 사용중인 이메일입니다");  // 409
}
```

## 테스트 결과

### HTTP 상태 코드 테스트 (7/7 PASS)

| 테스트 | 예상 코드 | 실제 코드 | 결과 |
|--------|----------|----------|------|
| 중복 이메일 | 409 Conflict | 409 | ✅ PASS |
| 잘못된 이메일 도메인 (@gmail.com) | 400 Bad Request | 400 | ✅ PASS |
| 필수 필드 누락 | 400 Bad Request | 400 | ✅ PASS |
| 비밀번호 길이 부족 (7자) | 400 Bad Request | 400 | ✅ PASS |
| 잘못된 비밀번호 로그인 | 401 Unauthorized | 401 | ✅ PASS |
| 정상 회원가입 | 201 Created | 201 | ✅ PASS |
| 정상 로그인 | 200 OK | 200 | ✅ PASS |

### 전체 API 테스트 (15/15 PASS)

```
✅ Test 1: 회원가입 API - HTTP 201
✅ Test 2: 중복 회원가입 시도 - HTTP 409 (개선됨! 이전: 500)
✅ Test 3: 로그인 API - HTTP 200
✅ Test 4: 잘못된 비밀번호로 로그인 - HTTP 401 (개선됨! 이전: 500)
✅ Test 5: 내 정보 조회 (인증 O) - HTTP 200
✅ Test 6: 내 정보 조회 (인증 X) - HTTP 403
✅ Test 7: 사용자 설정 변경 - HTTP 200
✅ Test 8: 카테고리 목록 조회 - HTTP 200
✅ Test 9: 카테고리 구독 - HTTP 201
✅ Test 10: 내 구독 목록 조회 - HTTP 200
✅ Test 11: 북마크 목록 조회 - HTTP 200
✅ Test 12: 공지사항 목록 조회 - HTTP 200
✅ Test 13: 공지사항 검색 - HTTP 200
✅ Test 14: 토큰 갱신 - HTTP 200
✅ Test 15: Swagger UI 접근 - HTTP 302
```

## HTTP 상태 코드 가이드

### 2xx 성공
- **200 OK** - 요청 성공 (조회, 수정 등)
- **201 Created** - 리소스 생성 성공 (회원가입, 생성 등)

### 4xx 클라이언트 에러
- **400 Bad Request** - 잘못된 요청
  - Validation 실패 (필수 필드 누락, 형식 오류)
  - 비즈니스 규칙 위반 (잘못된 이메일 도메인 등)
- **401 Unauthorized** - 인증 실패
  - 로그인 실패 (잘못된 비밀번호)
  - 유효하지 않은 토큰
- **403 Forbidden** - 권한 없음
  - 인증되지 않은 접근
- **409 Conflict** - 리소스 충돌
  - 중복 이메일, 중복 학번

### 5xx 서버 에러
- **500 Internal Server Error** - 예상치 못한 서버 오류

## 변경된 파일

1. **src/main/java/com/incheon/notice/exception/**
   - `DuplicateResourceException.java` (신규)
   - `InvalidCredentialsException.java` (신규)
   - `BusinessException.java` (신규)
   - `GlobalExceptionHandler.java` (수정)

2. **src/main/java/com/incheon/notice/service/**
   - `AuthService.java` (수정)

3. **테스트 스크립트**
   - `test_http_status_codes.sh` (신규)

## 사용자 입장에서의 개선점

### Before (개선 전)
```bash
# 중복 이메일로 회원가입
HTTP 500 Internal Server Error
{"success":false,"message":"이미 사용중인 이메일입니다",...}
```

### After (개선 후)
```bash
# 중복 이메일로 회원가입
HTTP 409 Conflict
{"success":false,"message":"이미 사용중인 이메일입니다",...}
```

**장점:**
1. **명확한 에러 구분**: HTTP 코드만 봐도 에러 원인 파악 가능
2. **RESTful API**: 표준 HTTP 상태 코드 준수
3. **디버깅 용이**: 클라이언트 에러(4xx)와 서버 에러(5xx) 명확히 구분
4. **자동화 가능**: HTTP 코드 기반 에러 처리 로직 구현 가능

## 테스트 실행 방법

### 1. HTTP 상태 코드 테스트
```bash
./test_http_status_codes.sh
```

### 2. 전체 API 테스트
```bash
./comprehensive_api_test.sh
```

### 3. 간단한 API 테스트
```bash
./test_apis.sh
```

## 결론

✅ **모든 HTTP 상태 코드가 RESTful 표준에 맞게 개선되었습니다**

- 400 에러는 정상적인 Validation 에러입니다
- 중복 리소스는 409 Conflict를 반환합니다
- 인증 실패는 401 Unauthorized를 반환합니다
- 비즈니스 규칙 위반은 400 Bad Request를 반환합니다

**모든 API가 정상 작동하며, Swagger UI를 통해 테스트할 수 있습니다!**

Swagger UI: http://localhost:8080/swagger-ui.html
