package com.kicksafe.auth.service;

import com.kicksafe.auth.dto.TokenRefreshRequestDTO;
import com.kicksafe.auth.dto.TokenResponseDTO;

public interface AuthService {

    // 토큰 재발급 (헌 토큰 줄게 새 토큰 다오)
    TokenResponseDTO reissue(TokenRefreshRequestDTO requestDTO);

    // 로그아웃 (DB 에서 리프레시 토큰 삭제)
    void logout(String providerUserId);
}