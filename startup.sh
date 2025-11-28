#!/bin/bash
# VM 부팅 시 자동으로 실행됩니다.

# (참고: Container-Optimized OS를 사용하면 Docker 설치가 필요 없습니다)
# Ubuntu/Debian의 경우 Docker 설치
# apt-get update
# apt-get install -y docker.io
apt-get update
apt-get install -y docker.io

# 1. Artifact Registry에서 이미지 다운로드
docker pull asia-northeast3-docker.pkg.dev/project-f2639456-81ab-4842-bc5/docker-registry/backend:latest

# 2. 'docker run'으로 컨테이너 실행
#    -d: 백그라운드 실행
#    -p 80:8080: VM의 80포트(LB가 연결할 포트)를 컨테이너의 8080포트(Spring Boot)로 연결
#    --restart=always: VM 재부팅 시 컨테이너 자동 재시작
#    -e ...: 환경 변수 주입 (★매우 중요★)
docker run -d -p 80:8080 --restart=always \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://10.0.1.2:5432/incheon_notice" \
  -e SPRING_DATASOURCE_USERNAME="postgres" \
  -e SPRING_DATASOURCE_PASSWORD="postgres" \
  -e SPRING_REDIS_HOST="10.0.1.2" \
  -e SPRING_REDIS_PORT="6379" \
  asia-northeast3-docker.pkg.dev/project-f2639456-81ab-4842-bc5/docker-registry/backend:latest