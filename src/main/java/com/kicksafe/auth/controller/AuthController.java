package com.kicksafe.auth.controller;

import com.kicksafe.auth.dto.TokenRefreshRequestDTO;
import com.kicksafe.auth.dto.TokenResponseDTO;
import com.kicksafe.auth.service.AuthService;
import com.kicksafe.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * [토큰 재발급 API]
     * URL: POST /auth/reissue
     * Body: { "refreshToken": "ey..." }
     * 만료된 Access Token 대신 Refresh Token 을 주면, 새로운 토큰 세트를 줌
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDTO> reissue(@RequestBody TokenRefreshRequestDTO requestDTO) {
        log.info("토큰 재발급 요청");
        return ResponseEntity.ok(authService.reissue(requestDTO));
    }

    /**
     * [로그아웃 API]
     * URL: POST /auth/logout
     * Header: Authorization: Bearer {Access_Token}
     * DB 에서 Refresh Token 을 삭제하여 로그아웃 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // [수정 포인트] 토큰이 만료되어서 userPrincipal 이 null 로 들어온 경우
        // (이미 만료됐으니 DB 에서 지울 필요도/방법도 없음 -> 그냥 통과)
        if (userPrincipal == null) {
            log.info("이미 만료된 토큰으로 로그아웃 요청 -> 별도 DB 처리 없이 성공 응답");
            return ResponseEntity.ok("로그아웃 되었습니다.");
        }

        // 정상 토큰인 경우 -> DB 에서 리프레시 토큰 삭제
        authService.logout(userPrincipal.getName());
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}