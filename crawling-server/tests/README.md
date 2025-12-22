# 테스트 가이드

인천대학교 공지사항 크롤러 테스트 스위트입니다.

## 테스트 실행 방법

### 전체 테스트 실행
```bash
PYTHONPATH=. pytest tests/ -v
```

### 단위 테스트만 실행
```bash
PYTHONPATH=. pytest tests/unit/ -v
```

### 통합 테스트만 실행
```bash
PYTHONPATH=. pytest tests/integration/ -v -m integration
```

### 커버리지와 함께 실행
```bash
PYTHONPATH=. pytest tests/ --cov=app --cov-report=html
```

커버리지 리포트는 `htmlcov/index.html`에서 확인할 수 있습니다.

### 특정 테스트만 실행
```bash
# 특정 클래스의 테스트만
PYTHONPATH=. pytest tests/unit/test_crawlers.py::TestCleanCategory -v

# 특정 테스트 메서드만
PYTHONPATH=. pytest tests/unit/test_crawlers.py::TestCleanCategory::test_clean_category_with_brackets -v
```

## 테스트 구조

```
tests/
├── __init__.py
├── conftest.py              # pytest 설정 및 fixture
├── unit/                    # 단위 테스트
│   ├── __init__.py
│   └── test_crawlers.py     # 크롤러 단위 테스트
└── integration/             # 통합 테스트
    ├── __init__.py
    └── test_api.py          # API 통합 테스트
```

## 주요 테스트 내용

### Unit Tests (tests/unit/test_crawlers.py)

1. **TestCleanCategory** - 카테고리 정리 함수 테스트
   - 대괄호 제거 기능
   - 빈 문자열 처리
   - None 처리

2. **TestCollegeCrawlerInit** - 크롤러 초기화 테스트
   - 기본값 (max_pages=5)
   - 커스텀 max_pages
   - 세션 헤더 설정

3. **TestParseRow** - HTML 행 파싱 테스트
   - 정상 파싱
   - 카테고리가 없는 경우
   - 링크가 없는 경우
   - 일부 필드가 없는 경우
   - 외부 URL 처리

4. **TestCrawlGeneric** - 다중 페이지 크롤링 테스트
   - 단일 페이지 크롤링
   - 다중 페이지 크롤링
   - 빈 페이지에서 조기 종료
   - 에러 처리

5. **TestVolunteerCrawler** - 봉사 크롤러 테스트
   - 정상 크롤링
   - 에러 처리

6. **TestJobCrawler** - 취업 크롤러 테스트
   - 다중 페이지
   - URL 검증

7. **TestScholarshipCrawler** - 장학금 크롤러 테스트
   - 기본 페이지 번호
   - 커스텀 페이지 번호

8. **TestCrawlAll** - 전체 크롤링 테스트
   - 모든 카테고리 크롤링 호출 확인

## 테스트 커버리지

현재 `college_crawlers.py` 모듈의 테스트 커버리지: **73%**

```
app/college_crawlers.py       179     49    73%
```

## Fixtures 설명

### `crawler`
- CollegeCrawler 인스턴스 제공
- max_pages=2로 설정 (테스트 속도 향상)

### `sample_html_row`
- 테스트용 HTML 행 샘플

### `sample_notice_data`
- 테스트용 공지사항 데이터 딕셔너리

### `mock_response_success`
- HTTP 성공 응답 모의 객체

### `mock_response_empty`
- 빈 페이지 응답 모의 객체

## 마커 (Markers)

- `@pytest.mark.unit` - 단위 테스트
- `@pytest.mark.integration` - 통합 테스트
- `@pytest.mark.slow` - 느린 테스트 (실제 HTTP 요청 등)

## 의존성

테스트에 필요한 패키지:
- pytest >= 7.4.3
- pytest-asyncio >= 0.21.1
- pytest-cov >= 4.1.0
- pytest-mock >= 3.12.0
- httpx >= 0.25.0

설치 방법:
```bash
pip install pytest pytest-asyncio pytest-cov pytest-mock httpx
```

## 테스트 작성 가이드

1. **테스트 파일 이름**: `test_*.py`
2. **테스트 클래스 이름**: `Test*`
3. **테스트 메서드 이름**: `test_*`
4. **Docstring 작성**: 각 테스트의 목적을 한글로 명시
5. **AAA 패턴 사용**: Arrange-Act-Assert
   ```python
   def test_example(self, crawler):
       # Arrange - 준비
       expected = "봉사"

       # Act - 실행
       result = clean_category("[봉사]")

       # Assert - 검증
       assert result == expected
   ```

## CI/CD 통합

향후 GitHub Actions를 통해 자동 테스트 실행 예정:
- PR 생성 시 자동 테스트
- main/dev 브랜치 push 시 자동 테스트
- 커버리지 리포트 생성

## 문제 해결

### ModuleNotFoundError 발생 시
```bash
# PYTHONPATH 설정 확인
PYTHONPATH=. pytest tests/ -v
```

### 테스트가 느린 경우
```bash
# 병렬 실행 (pytest-xdist 설치 필요)
pip install pytest-xdist
pytest tests/ -n auto
```

### 특정 테스트만 디버깅
```bash
pytest tests/unit/test_crawlers.py::TestCleanCategory -v --pdb
```

## 업데이트 내역

- **2025-11-02**: 초기 테스트 인프라 설정
  - conftest.py 생성
  - test_crawlers.py 23개 테스트 작성
  - pytest 설정 (pyproject.toml)
  - 테스트 커버리지: 73%
