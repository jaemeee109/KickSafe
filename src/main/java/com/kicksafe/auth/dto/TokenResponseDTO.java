package com.kicksafe.auth.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDTO {

    private String grantType;  // Bearer
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;
}
