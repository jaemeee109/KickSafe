package com.kicksafe.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [보안 검색대]
 * 모든 요청(Request)이 들어올 때마다 이 필터를 거침.
 * 헤더에 있는 토큰을 꺼내서 -> 유효한지 검사하고 -> 유효하면 "통과(인증 완료)"
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    // 실제 필터링 로직이 들어가는 곳
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Request Header 에서 토큰을 꺼냄
        String jwt = resolveToken(request);

        // 2. 토큰이 있고 && 유효하다면 (조폐공사에서 검증)
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            // 3. 토큰에서 인증 정보(Authentication)를 가져와서
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

            // 4. 스프링 시큐리티 저장소(SecurityContext)에 "이 사람 인증됨!" 하고 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 다음 필터(혹은 컨트롤러)로 넘겨줌
        filterChain.doFilter(request, response);
    }

    // Request Header 에서 토큰 정보 꺼내오기
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7); // "Bearer " 이후의 문자열만 자름
        }
        return null;
    }
}