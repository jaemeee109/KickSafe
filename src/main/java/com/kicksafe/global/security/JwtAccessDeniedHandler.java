package com.kicksafe.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * [403 Forbidden 처리]
 * 로그인은 했지만, 해당 페이지에 접근할 권한이 없을 때 (예: 일반 유저가 관리자 페이지 접근)
 * "거긴 못 들어갑니다" 라고 응답하는 클래스
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 필요한 권한이 없이 접근하려 할 때 403 에러를 보냄
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}