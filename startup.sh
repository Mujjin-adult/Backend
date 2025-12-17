#!/bin/bash
# VM 부팅 시 자동으로 실행됩니다.
# APT 잠금이 풀릴 때까지 대기
while fuser /var/lib/dpkg/lock >/dev/null 2>&1 || \
      fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || \
      fuser /var/lib/apt/lists/lock >/dev/null 2>&1; do
  echo "Waiting for apt/dpkg lock to be released..."
  sleep 5
done

# Docker 설치
sudo apt-get update
sudo apt-get install -y docker.io

# 1. Artifact Registry에서 이미지 다운로드
IMAGE_URL="asia-northeast3-docker.pkg.dev/project-f2639456-81ab-4842-bc5/docker-registry/backend:latest"
docker pull ${IMAGE_URL}

# 2. Secret Manager에서 비밀 가져오기
PROJECT_ID="project-f2639456-81ab-4842-bc5"
DB_PASSWORD=$(gcloud secrets versions access latest --secret="db-password" --project=${PROJECT_ID})
REDIS_PASSWORD=$(gcloud secrets versions access latest --secret="redis-password" --project=${PROJECT_ID})
JWT_SECRET=$(gcloud secrets versions access latest --secret="jwt-secret" --project=${PROJECT_ID})

# 3. DB VM의 Private IP (DB VM 생성 후 업데이트 필요)
# TODO: DB VM 생성 후 이 IP를 실제 Private IP로 변경하세요
DB_HOST="10.0.1.5"  # 예: 10.178.0.5

# 4. 컨테이너 실행
#    -d: 백그라운드 실행
#    -p 80:8080: VM의 80포트(LB가 연결할 포트)를 컨테이너의 8080포트(Spring Boot)로 연결
#    --restart=always: VM 재부팅 시 컨테이너 자동 재시작
#    -e ...: 환경 변수 주입
docker run -d -p 8080:8080 --restart=always --name backend-app \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -e DATABASE_URL="jdbc:postgresql://${DB_HOST}:5432/incheon_notice" \
  -e DATABASE_USERNAME="postgres" \
  -e DATABASE_PASSWORD="${DB_PASSWORD}" \
  -e REDIS_HOST="${DB_HOST}" \
  -e REDIS_PORT="6379" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  #-e SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
  ${IMAGE_URL}