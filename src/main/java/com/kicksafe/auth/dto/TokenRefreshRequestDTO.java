package com.kicksafe.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequestDTO {

    private String refreshToken; // 프론트가 보내줄 리프레시 토큰
}