package com.kicksafe.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * [401 Unauthorized 처리]
 * 유효한 자격 증명(토큰) 없이 접근하려 할 때 "너 누구야? 돌아가!" 라고 응답하는 클래스
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // 유효한 자격증명을 제공하지 않고 접근하려 할 때 401 에러를 보냄
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}