package com.kicksafe.member.repository;

import com.kicksafe.member.domain.MemberBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBlacklistRepository extends JpaRepository<MemberBlacklist, Long> {
}
