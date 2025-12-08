package com.kicksafe.auth.service;

import com.kicksafe.auth.domain.RefreshToken;
import com.kicksafe.auth.dto.TokenRefreshRequestDTO;
import com.kicksafe.auth.dto.TokenResponseDTO;
import com.kicksafe.auth.repository.RefreshTokenRepository;
import com.kicksafe.global.security.JwtTokenProvider;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * [토큰 재발급 로직]
     * 1. 리프레시 토큰 자체가 유효한지 검사 (위조/만료 체크)
     * 2. 토큰 주인(User)이 맞는지 DB 확인
     * 3. DB에 저장된 리프레시 토큰과 일치하는지 확인
     * 4. 다 통과하면 새 토큰 발급 후 DB 업데이트
     */
    @Override
    @Transactional
    public TokenResponseDTO reissue(TokenRefreshRequestDTO requestDTO) {

        // 1. 프론트에서 보낸 Refresh Token 검증
        if (!jwtTokenProvider.validateToken(requestDTO.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 이 유효하지 않습니다."); // 만료되었거나 위조됨
        }

        // 2. 토큰에서 유저 ID(providerUserId) 가져오기
        Authentication authentication = jwtTokenProvider.getAuthentication(requestDTO.getRefreshToken());

        // 3. DB 에서 유저 찾기
        Member member = memberRepository.findByProviderUserId(authentication.getName())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 4. DB 에서 유저의 리프레시 토큰 가져오기
        RefreshToken refreshToken = refreshTokenRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다.")); // DB에 없으면 이미 로그아웃한 것

        // 5. 토큰 일치 여부 확인 (탈취된 토큰일 수도 있으니)
        if (!refreshToken.getValue().equals(requestDTO.getRefreshToken())) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        // 6. 새로운 토큰 생성 (Access + Refresh 둘 다 새로 만듦)
        TokenResponseDTO tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 7. DB에 저장된 토큰 값을 새 걸로 교체 (Dirty Checking 으로 자동 저장됨)
        refreshToken.updateValue(tokenDto.getRefreshToken());

        return tokenDto;
    }

    /**
     * [로그아웃 로직]
     * DB에 저장된 리프레시 토큰을 지워버림
     * 그러면 나중에 재발급 요청이 와도 "DB에 없는데?" 하고 거절
     */
    @Override
    @Transactional
    public void logout(String providerUserId) {
        Member member = memberRepository.findByProviderUserId(providerUserId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 해당 유저의 리프레시 토큰 삭제
        refreshTokenRepository.deleteById(member.getId());
        log.info("로그아웃 완료 - 사용자: {}", providerUserId);
    }
}