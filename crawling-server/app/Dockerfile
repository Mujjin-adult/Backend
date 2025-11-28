FROM python:3.12-slim

# 메타데이터
LABEL maintainer="dev-team@example.com"
LABEL description="College Notice Crawler - Production Ready"
LABEL version="2.0"

# 비루트 사용자 생성 (보안 강화)
RUN groupadd -r appuser && useradd -r -g appuser -m appuser

# 시스템 의존성 설치 (Playwright용)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libxss1 \
    libxtst6 \
    xdg-utils \
    libx11-xcb1 \
    libxcb-dri3-0 \
    libxcb1 \
    libxcursor1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrender1 \
    libglib2.0-0 \
    libpango-1.0-0 \
    libcairo2 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

WORKDIR /app

# requirements.txt 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && pip install --no-cache-dir debugpy \
    && rm -rf ~/.cache/pip

# 애플리케이션 코드 복사
COPY --chown=appuser:appuser . /app

# 환경 변수
ENV PYTHONPATH=/:/app \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8000/health', timeout=5)" || exit 1

# 비루트 사용자로 전환
USER appuser

# Playwright 브라우저 설치 (appuser로 설치하여 권한 문제 해결)
RUN playwright install chromium

# 포트 노출
EXPOSE 8000

# 기본 실행 명령
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
