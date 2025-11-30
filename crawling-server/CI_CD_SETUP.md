# CI/CD 파이프라인 설정 가이드

## 목차
- [개요](#개요)
- [CI 파이프라인](#ci-파이프라인)
- [CD 파이프라인](#cd-파이프라인)
- [GitHub Secrets 설정](#github-secrets-설정)
- [배포 프로세스](#배포-프로세스)
- [문제 해결](#문제-해결)

---

## 개요

이 프로젝트는 GitHub Actions를 사용한 CI/CD 파이프라인을 구축했습니다.

### CI/CD 흐름

```
코드 Push
    ↓
CI Pipeline (자동)
├── 테스트 실행
├── 코드 린팅
├── 보안 스캔
└── Docker 이미지 빌드
    ↓
CD Pipeline (자동)
├── Staging 배포 (main 브랜치)
└── Production 배포 (태그 생성)
```

---

## CI 파이프라인

### 트리거 조건

```yaml
on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main, dev ]
```

### Jobs

#### 1. Test Job

**실행 내용**:
- PostgreSQL, Redis 서비스 시작
- Python 3.12 설정
- 의존성 설치
- 데이터베이스 마이그레이션
- pytest 실행
- 코드 커버리지 측정

**서비스**:
```yaml
postgres:
  image: postgres:15
  port: 5432

redis:
  image: redis:7-alpine
  port: 6379
```

**테스트 실행**:
```bash
pytest tests/ -v --cov=. --cov-report=xml
```

#### 2. Lint Job

**실행 내용**:
- Black (코드 포맷터)
- isort (import 정렬)
- Flake8 (린터)

**명령어**:
```bash
black --check --diff .
isort --check-only --diff .
flake8 . --max-line-length=127
```

#### 3. Security Job

**실행 내용**:
- Safety (의존성 취약점 검사)
- Bandit (보안 린터)

**명령어**:
```bash
safety check --file=requirements.txt
bandit -r . -ll
```

#### 4. Build Job

**실행 내용**:
- Docker 이미지 빌드
- 이미지 테스트

**조건**: Test, Lint Job 성공 후 실행

---

## CD 파이프라인

### Staging 배포

**트리거**: `main` 브랜치에 push

**프로세스**:
1. Docker 이미지 빌드
2. Docker Registry에 push
3. Staging 서버에 SSH 접속
4. docker-compose로 서비스 재시작
5. Health check 수행

**이미지 태그**: `staging-{commit-sha}`

### Production 배포

**트리거**: `v*.*.*` 형식의 태그 생성

**프로세스**:
1. 버전 추출 (예: v1.0.0 → 1.0.0)
2. Docker 이미지 빌드
3. 두 개의 태그로 push:
   - `{version}` (예: 1.0.0)
   - `latest`
4. GitHub Release 생성
5. Production 서버에 배포
6. Health check 수행
7. Slack 알림 전송

---

## GitHub Secrets 설정

### 필수 Secrets

Repository Settings → Secrets and variables → Actions에서 설정:

#### Docker Registry
```
DOCKER_REGISTRY=registry.your-domain.com
DOCKER_USERNAME=your-username
DOCKER_PASSWORD=your-password
```

#### Staging Environment
```
STAGING_HOST=staging.your-domain.com
STAGING_USER=deploy
STAGING_SSH_KEY=<SSH private key>
STAGING_URL=https://staging.your-domain.com
```

#### Production Environment
```
PROD_HOST=prod.your-domain.com
PROD_USER=deploy
PROD_SSH_KEY=<SSH private key>
PROD_URL=https://your-domain.com
```

#### Notifications
```
SLACK_WEBHOOK=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### SSH Key 생성

```bash
# 1. SSH 키 생성
ssh-keygen -t ed25519 -C "github-actions-deploy"

# 2. 공개키를 서버에 추가
ssh-copy-id -i ~/.ssh/id_ed25519.pub deploy@your-server.com

# 3. 개인키를 GitHub Secret에 추가
cat ~/.ssh/id_ed25519
# 전체 내용을 복사하여 STAGING_SSH_KEY에 붙여넣기
```

---

## 배포 프로세스

### Staging 배포

```bash
# 1. 코드 수정 및 커밋
git add .
git commit -m "feat: add new feature"

# 2. main 브랜치에 push
git push origin main

# 3. GitHub Actions에서 자동 배포
# https://github.com/your-repo/actions
```

**결과**: 약 5-10분 후 Staging 환경에 배포됨

### Production 배포

```bash
# 1. 버전 태그 생성
git tag v1.0.0

# 2. 태그 push
git push origin v1.0.0

# 3. GitHub Actions에서 자동 배포
```

**결과**:
- GitHub Release 자동 생성
- Production 환경에 배포
- Slack 알림 전송

### 버전 관리 규칙

[Semantic Versioning](https://semver.org/) 사용:

- `v1.0.0` - Major 릴리스 (호환성 깨지는 변경)
- `v1.1.0` - Minor 릴리스 (새 기능 추가)
- `v1.1.1` - Patch 릴리스 (버그 수정)

---

## 로컬에서 CI 테스트

### 1. 테스트 실행

```bash
cd app
pytest tests/ -v
```

### 2. 린팅 실행

```bash
# Black
black --check .

# isort
isort --check-only .

# Flake8
flake8 . --max-line-length=127
```

### 3. 보안 스캔

```bash
# Safety
safety check --file=requirements.txt

# Bandit
bandit -r . -ll
```

### 4. Docker 빌드 테스트

```bash
cd app
docker build -t college-notice-crawler:test .
docker run --rm college-notice-crawler:test python --version
```

---

## 서버 설정

### Staging/Production 서버 준비

#### 1. Docker 설치

```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

#### 2. 배포 사용자 생성

```bash
# 배포 전용 사용자 생성
sudo useradd -m -s /bin/bash deploy

# Docker 그룹 추가
sudo usermod -aG docker deploy

# sudo 권한 추가 (선택사항)
sudo visudo
# 추가: deploy ALL=(ALL) NOPASSWD: /usr/local/bin/docker-compose
```

#### 3. 애플리케이션 디렉토리 생성

```bash
sudo mkdir -p /app/college-notice-crawler
sudo chown deploy:deploy /app/college-notice-crawler

# 설정 파일 배치
cd /app/college-notice-crawler
# docker-compose.yml, .env 파일 등 배치
```

#### 4. 환경 변수 설정

```bash
# /app/college-notice-crawler/.env
DATABASE_URL=postgresql://...
CELERY_BROKER_URL=redis://...
API_KEY=production-api-key
SENTRY_DSN=https://...
```

---

## 모니터링

### GitHub Actions 모니터링

**Actions 탭 확인**:
- https://github.com/your-repo/actions

**빌드 상태 확인**:
```bash
# 최근 workflow 실행 확인
gh run list

# 특정 workflow 로그 확인
gh run view <run-id>
```

### 배포 상태 확인

**Staging**:
```bash
curl https://staging.your-domain.com/health
```

**Production**:
```bash
curl https://your-domain.com/health
```

---

## 롤백 프로세스

### Docker 이미지 롤백

```bash
# 1. 서버에 SSH 접속
ssh deploy@prod.your-domain.com

# 2. 이전 버전 이미지로 변경
cd /app/college-notice-crawler
docker-compose down
docker-compose pull registry.your-domain.com/college-notice-crawler:1.0.0
docker-compose up -d

# 3. Health check
curl http://localhost:8000/health
```

### Git 태그 롤백

```bash
# 1. 이전 버전 태그 다시 생성
git tag -f v1.0.0 <previous-commit-sha>
git push origin v1.0.0 --force

# 2. CD 파이프라인이 자동으로 재배포
```

---

## 문제 해결

### CI 파이프라인 실패

#### 테스트 실패
```bash
# 로컬에서 테스트 실행
pytest tests/ -v

# 특정 테스트만 실행
pytest tests/test_crawlers.py::test_function_name -v
```

#### 린팅 에러
```bash
# 자동 수정
black .
isort .
```

#### Docker 빌드 실패
```bash
# 로컬에서 빌드 테스트
docker build -t test .

# 빌드 캐시 없이 재시도
docker build --no-cache -t test .
```

### CD 파이프라인 실패

#### SSH 연결 실패
- SSH Key가 올바르게 설정되었는지 확인
- 서버 방화벽에서 GitHub Actions IP 허용

#### Health Check 실패
```bash
# 서버에서 직접 확인
curl http://localhost:8000/health

# 로그 확인
docker-compose logs fastapi
```

#### Docker Registry 인증 실패
- DOCKER_USERNAME, DOCKER_PASSWORD 확인
- Docker Registry 접근 권한 확인

---

## 모범 사례

### 1. 브랜치 전략

```
main (production)
  ↑
dev (staging)
  ↑
feature/* (개발)
```

**워크플로우**:
1. `feature/new-feature` 브랜치 생성
2. PR을 `dev`로 생성
3. CI 통과 후 `dev`에 머지 → Staging 배포
4. 테스트 후 `main`으로 PR → Production 태그 생성

### 2. 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/) 사용:

```
feat: add new crawler for scholarship notices
fix: resolve database connection timeout
docs: update README with deployment instructions
test: add tests for bulk insert optimization
```

### 3. PR 체크리스트

- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] 문서 업데이트
- [ ] CHANGELOG 업데이트
- [ ] 마이그레이션 스크립트 추가 (필요시)

### 4. 보안

- 절대 Secrets을 코드에 포함하지 않음
- `.env` 파일을 `.gitignore`에 추가
- 정기적으로 의존성 업데이트
- Dependabot 활성화

---

## 참고 자료

- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Docker Hub](https://hub.docker.com/)
- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

**작성일**: 2025-11-03
**버전**: 1.0
**작성자**: Claude Code Assistant
