package com.incheon.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 인천대학교 공지사항 백엔드 애플리케이션
 *
 * @SpringBootApplication: Spring Boot 애플리케이션임을 선언
 * @EnableScheduling: 스케줄링 기능 활성화 (주기적으로 FastAPI 크롤링 서버에 요청)
 * @EnableAsync: 비동기 처리 기능 활성화 (푸시 알림 등을 비동기로 처리)
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class IncheonNoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncheonNoticeApplication.class, args);
    }
}
