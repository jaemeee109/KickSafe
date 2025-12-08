package com.kicksafe.member.repository;

import com.kicksafe.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 소셜 로그인 ID(예: "kakao_12345")로 회원 찾기
    Optional<Member> findByProviderUserId(String providerUserId);

    // 닉네임 중복 여부 확인
    // 결과가 true 면 이미 존재(사용 불가), false 면 없음(사용 가능)
    boolean existsByNickname(String nickname);
}
