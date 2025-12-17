# GCP 배포 가이드

### VM
1. 백엔드용 인스턴스 템플릿 생성
   1. 태그에 springboot-backend 
   2. 방화벽 http, https, lb 트래픽 사용
   3. 맞춤 메타 데이터에 startup.sh 넣기 (ip, 프로젝트명, 저장소명 변경)
2. db용 인스턴스 생성 
3. 모니터링용 인스턴스 생성 (저사양)
4. 크롤링용 인스턴스 (보류)
5. 인스턴스 그룹 생성
   1. lb 연결


### VPC
1. pub, pri
2. 방화벽 규칙
   1. backend, 8080
   2. fastapi, 8001
   3. flower, 5555
   4. grafana, 3000
   5. pgadmin, 5050
   6. postgre, 5432
   7. prometheus, 9090
   8. redis, 6379

### IAM
1. compute@developer.~
   1. Artifact Registry Writer
   2. compute viewer
   3. Secret Manager Secret Accessor

### Artifact Registry
1. docker-registry
   1. backend

