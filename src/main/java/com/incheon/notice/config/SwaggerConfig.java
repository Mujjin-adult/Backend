package com.incheon.notice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Swagger/OpenAPI 설정
 * API 문서 자동 생성
 * 접속 URL: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("인천대학교 공지사항 API")
                        .description("인천대학교 공지사항 앱 백엔드 API 문서")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("Firebase")
                                        .description("Firebase 커스텀 토큰 또는 ID 토큰을 입력하세요")))
                .tags(Arrays.asList(
                        new Tag().name("인증 및 회원관리").description("회원가입, 로그인, 아이디/비밀번호 찾기 API"),
                        new Tag().name("사용자 정보").description("사용자 프로필 조회 및 관리 API"),
                        new Tag().name("공지사항").description("공지사항 조회 및 검색 API"),
                        new Tag().name("북마크").description("공지사항 저장 및 관리 API"),
                        new Tag().name("알림 설정").description("카테고리 구독 및 알림 관리 API"),
                        new Tag().name("카테고리").description("카테고리 목록 조회 API"),
                        new Tag().name("검색").description("검색 및 최근 검색어 관리 API")
                ));
    }
}
