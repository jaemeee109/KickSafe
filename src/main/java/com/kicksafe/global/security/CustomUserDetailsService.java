package com.kicksafe.global.security;

import com.kicksafe.member.constant.MemberStatus; // [추가] 상태 확인용
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [명세표]
 * 1. 역할: 시큐리티가 "이 유저 유효해?"라고 물을 때 확인해주는 곳
 * 2. 정책: 탈퇴한 회원(WITHDRAWN)은 절대 출입 금지 (재가입 불가)
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 회원 찾기
        Member member = memberRepository.findByProviderUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 2. [철벽 방어] 탈퇴한 회원인지 검사
        // "재가입 물어보기" 같은 거 없습니다. 그냥 바로 에러 던져서 쫓아냅니다.
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new UsernameNotFoundException("탈퇴한 회원입니다. 재가입이 불가능합니다.");
        }

        return UserPrincipal.create(member);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("ID를 찾을 수 없습니다: " + id));

        // 여기도 똑같이 철벽 방어
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new UsernameNotFoundException("탈퇴한 회원입니다. 재가입이 불가능합니다.");
        }

        return UserPrincipal.create(member);
    }
}