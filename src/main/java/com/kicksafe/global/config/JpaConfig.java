package com.kicksafe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // [필수] JPA Auditing(자동 시간 감지) 기능 활성화
public class JpaConfig {
}
