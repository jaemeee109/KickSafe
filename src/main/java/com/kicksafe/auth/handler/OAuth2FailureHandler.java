package com.kicksafe.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        // 1. 에러 로그를 서버에 남김
        log.error("소셜 로그인 실패: {}", exception.getMessage());

        // 2. 프론트엔드 로그인 페이지로 리다이렉트 (에러 메시지 포함)
        // 한글 메시지가 깨지지 않게 인코딩
        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
                .queryParam("error", errorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}