// 리액트네이티브에서 API 호출 시 CORS 문제를 해결하기 위한 설정

package com.incheon.notice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins("http://localhost:8081", "http://10.0.2.2:8081", "http://192.168.0.x:8081") // *React Native 앱이 실행되는 주소를 허용합니다.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 인증 정보(쿠키 등) 허용
    }
}


/*
참고사항

llowedOrigins에 있는 주소들은 리액트 네이티브 앱이 실행되는 환경에 따라 달라집니다.
http://localhost:8081: 웹 브라우저에서 테스트할 경우 (Expo Web 등)
http://10.0.2.2:8081: 안드로이드 에뮬레이터에서 백엔드를 호출할 때의 주소 (10.0.2.2는 에뮬레이터 내부에서 호스트 PC를 가리킵니다.)
http://192.168.0.x:8081: 실제 장치나 iOS 에뮬레이터에서 호출할 때의 주소 (여러분의 컴퓨터의 실제 로컬 IP 주소로 바꿔야 합니다.)
개발 초기에는 allowedOrigins("*")로 설정할 수도 있지만, 보안을 위해 특정 출처를 명시하는 것을 권장합니다.
 */