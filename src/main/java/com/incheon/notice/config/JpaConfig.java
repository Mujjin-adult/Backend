package com.incheon.notice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정
 * @EnableJpaAuditing: BaseEntity의 생성일시, 수정일시 자동 관리 활성화
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
