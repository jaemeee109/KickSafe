package com.kicksafe.member.repository;

import com.kicksafe.member.domain.MemberBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberBlacklistRepository extends JpaRepository<MemberBlacklist, Long> {

    // [추가] 특정 회원의 "활성화(Active)"된 블랙리스트 기록 찾기
    List<MemberBlacklist> findByMemberIdAndActiveYnTrue(Long memberId);
}
