# Spring Boot 백엔드 Dockerfile
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Gradle 파일 복사
COPY build.gradle settings.gradle ./
COPY src ./src

# 애플리케이션 빌드
RUN gradle clean build -x test --no-daemon

# 실행 이미지
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
