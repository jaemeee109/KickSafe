package com.kicksafe.global.security;

import io.jsonwebtoken.ExpiredJwtException; // ★ 추가됨: 만료된 토큰 에러를 잡기 위해 필요
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
 * [보안 검색대] - 수정됨
 * 설명: 모든 요청이 들어올 때 토큰을 검사합니다.
 * 추가 기능: 토큰이 만료되었을 때(Expired) 프론트엔드에 "401 에러 + JSON 메시지"를 보냅니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    // 실제 필터링 로직 (여기가 수정되었습니다)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 헤더에서 토큰 꺼내기
            String jwt = resolveToken(request);

            // 2. 토큰이 있고 && 유효한지 검사
            // (만약 토큰이 만료되었다면 여기서 에러(ExpiredJwtException)가 발생해서 catch로 넘어갑니다)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                
                // 3. 인증 정보 가져오기
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
                
                // 4. "이 사람 인증됨!" 도장 찍기
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 5. 다음 단계로 통과~
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // ★ [핵심] 토큰이 만료되었을 때 여기로 튕겨져 옵니다!
            // 프론트엔드가 알아들을 수 있게 직접 JSON 편지를 씁니다.
            
            // (1) 상태 코드: 401 (인증 실패)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            // (2) 편지 형식: JSON
            response.setContentType("application/json; charset=UTF-8");
            
            // (3) 내용: code를 "EXPIRED"로 보내서 프론트가 알아채게 함
            String jsonResponse = "{\"status\": 401, \"code\": \"EXPIRED\", \"message\": \"토큰이 만료되었습니다. 다시 로그인해주세요.\"}";
            
            // (4) 전송
            response.getWriter().write(jsonResponse);
            
        } catch (Exception e) {
            // 그 외 다른 에러가 났을 때 (혹시 몰라 안전장치)
            logger.error("보안 필터에서 알 수 없는 에러 발생", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "인증 처리 중 에러가 발생했습니다.");
        }
    }

    // (이 아래는 기존과 똑같습니다)
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
