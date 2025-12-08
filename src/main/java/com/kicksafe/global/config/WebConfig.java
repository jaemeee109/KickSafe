package com.kicksafe.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * [Web 설정 파일]
 * 1. CORS 설정: 프론트엔드(React 등)가 백엔드 서버에 접근할 수 있게 허용함.
 * 2. 리소스 핸들러: 서버 하드디스크의 특정 폴더를 웹 URL 로 접근할 수 있게 연결함.
 */
@Configuration // 스프링에게 "이건 설정 파일이야"라고 알림
public class WebConfig implements WebMvcConfigurer {

    // application.properties 에서 설정한 "file.dir" 값을 가져옴 (예: C:/kicksafe-files/)
    @Value("${file.dir}")
    private String fileDir;

    /**
     * [1. CORS 설정]
     * 프론트엔드(보통 3000번 포트)에서 백엔드(8080번 포트)로 데이터를 달라고 할 때,
     * "보안상 다른 출처(Origin)는 안 돼!"라고 막히는 것을 풀어주는 역할.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 우리 서버의 모든 URL 에 대해
                .allowedOrigins("http://localhost:3000") // 이 주소에서 오는 요청은 허용하겠다 (리액트 개발 서버)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 쿠키나 인증 정보 포함 허용
    }

    /**
     * [2. 이미지 경로 매핑 (핵심!)]
     * 브라우저에서 "http://localhost:8080/images/사진.jpg" 라고 요청하면
     * 실제 내 컴퓨터의 "C:/kicksafe-files/사진.jpg" 파일을 보여주도록 연결합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // (1) 웹 브라우저에서 접근할 경로 패턴
        // "/images/**"는 "/images/로 시작하는 모든 요청"이라는 뜻
        registry.addResourceHandler("/images/**")

                // (2) 실제 파일이 있는 내 컴퓨터 물리 경로
                // "file:///" 접두어를 꼭 붙여야 "이건 파일 시스템 경로야"라고 인식함
                .addResourceLocations("file:///" + fileDir);
    }
}