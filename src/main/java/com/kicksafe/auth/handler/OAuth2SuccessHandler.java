package com.kicksafe.auth.handler;

import com.kicksafe.auth.domain.RefreshToken;
import com.kicksafe.auth.dto.TokenResponseDTO;
import com.kicksafe.auth.repository.RefreshTokenRepository;
import com.kicksafe.global.security.JwtTokenProvider;
import com.kicksafe.global.security.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * [로그인 성공 핸들러]
 * 소셜 로그인이 성공하면 자동으로 실행되는 클래스
 * 여기서 JWT 토큰을 만들어서 프론트엔드(localhost:3000)로 보냄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. 로그인 성공한 유저 정보 가져오기
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // 2. JWT 토큰 생성 (Access, Refresh)
        TokenResponseDTO tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 3. Refresh Token DB 저장
        RefreshToken refreshToken = refreshTokenRepository.findById(userPrincipal.getId())
                .orElse(RefreshToken.builder()
                        .key(userPrincipal.getId())
                        .build());

        refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(refreshToken);

        // 4. 프론트엔드로 리다이렉트 (토큰 전달)
        String targetUrl = UriComponentsBuilder.fromUriString("http://34.50.13.223.nip.io:3000/oauth/callback")
                .queryParam("accessToken", tokenDto.getAccessToken())
                .queryParam("refreshToken", tokenDto.getRefreshToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}