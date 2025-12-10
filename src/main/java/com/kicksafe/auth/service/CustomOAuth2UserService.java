package com.kicksafe.auth.service;

import com.kicksafe.auth.info.*;
import com.kicksafe.global.security.UserPrincipal;
import com.kicksafe.member.constant.MemberRole;
import com.kicksafe.member.constant.MemberStatus; // [필수] 상태 확인용
import com.kicksafe.member.constant.Provider;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * [소셜 로그인 정보 처리 서비스]
 * 1. 목적:
 * 구글/카카오 같은 소셜 서비스(Provider)로부터 "인증된 유저 정보"를 건네받음
 * 이 정보를 바탕으로 우리 DB에 '회원가입' 시키거나, 기존 회원이면 '정보 업데이트'를 수행
 * 2. 작동 시점:
 * 사용자가 소셜 로그인 팝업에서 "동의"를 누르고 인증이 완료된 직후,
 * 스프링 시큐리티 필터가 자동으로 이 클래스의 loadUser() 메서드를 실행
 * 3. 주의사항:
 * 이 클래스는 'DefaultOAuth2UserService' 를 상속받아야만 시큐리티와 연결됨
 * AuthService 와는 별개로 동작하는 "소셜 전용" 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    /**
     * [loadUser 메서드]
     * 소셜 서버(구글 등)에서 유저 정보를 받아오는 핵심 메서드
     *
     * @param userRequest 소셜 서비스에서 보내준 유저 정보가 담긴 요청 객체
     * @return OAuth2User (시큐리티가 이해할 수 있는 유저 정보 객체 = UserPrincipal)
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. 소셜 서비스(구글)에서 유저 정보를 가져옵니다. (부모 클래스인 super 가 처리)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 현재 로그인 중인 서비스가 어디인지 확인합니다. (예: "google", "naver", "kakao")
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 데이터 표준화 (서로 다른 소셜 JSON 모양을 하나로 통일)
        OAuth2UserInfo userInfo = null;
        if (registrationId.equals("google")) {
            log.info("구글 로그인 요청 확인");
            userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            log.info("카카오 로그인 요청");
            userInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("naver".equals(registrationId)) {
            log.info("네이버 로그인 요청");
            userInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("instagram".equals(registrationId)) {
            log.info("인스타그램 로그인 요청");
            userInfo = new InstagramOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            // [Fix] 구글/카카오가 아닌 이상한 요청이 들어왔을 때를 대비한 안전장치 (else 추가)
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 소셜 로그인입니다: " + registrationId
            );
        }

        // 4. [핵심 로직 추가] 로그인 시도자가 '탈퇴한 회원'인지 먼저 확인!
        // 저장(saveOrUpdate)하러 가기 전에 미리 검사해서 막아야 합니다.
        Optional<Member> memberOptional = memberRepository.findByProviderUserId(userInfo.getProviderId());

        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            // 만약 상태가 '탈퇴(WITHDRAWN)'라면?
            if (member.getStatus() == MemberStatus.WITHDRAWN) {
                // "어딜 들어오려고! 돌아가!" -> 로그인 실패 예외 발생 (재가입 불가)
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("WITHDRAWN_MEMBER"),
                        "탈퇴한 회원입니다. 재가입이 불가능합니다."
                );
            }
            // =========================================================
            // [★추가됨] (2) 정지(블랙리스트) 회원 체크
            // =========================================================
            if (member.getStatus() == MemberStatus.BLACKLISTED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("BLACKLISTED_MEMBER"),
                        "정지된 계정입니다. 관리자에게 문의하세요."
                );
            }

        }

        // 5. [비즈니스 로직] 통과한 사람만 우리 DB에 저장하거나 업데이트
        Member member = saveOrUpdate(userInfo);

        // 6. UserPrincipal(여권)을 만들어서 리턴
        return UserPrincipal.create(member, oAuth2User.getAttributes());
    }

    /**
     * [내부 메서드: 회원가입 및 업데이트]
     * 소셜 ID(이메일 등)로 DB를 조회해서
     * 있으면: 닉네임 등이 변경되었을 수 있으니 업데이트 (Update)
     * 없으면: 새로운 회원으로 자동 가입 (Insert)
     */
    private Member saveOrUpdate(OAuth2UserInfo userInfo) {
        Member member = memberRepository.findByProviderUserId(userInfo.getProviderId()).orElse(Member.builder()
                .providerUserId(userInfo.getProviderId())
                .provider(Provider.valueOf(userInfo.getProvider().toUpperCase()))
                .nickname(userInfo.getName()) // 처음 가입할 때만 소셜 이름 사용
                .role(MemberRole.GUEST)
                .status(MemberStatus.ACTIVE)
                .build());

        return memberRepository.save(member);
    }
}