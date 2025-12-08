package com.kicksafe.member.domain;

import com.kicksafe.global.common.BooleanToYNConverter;
import com.kicksafe.member.constant.BlacklistType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER_BLACKLIST")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BLACKLIST_ID")
    private Long id;

    // 제재 대상 회원 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    // 조치한 관리자 (FK, NULL 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID")
    private Member admin;

    // 제재 유형 (WITHDRAW, ABUSE, SPAM, OTHER)
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private BlacklistType type;

    // 사유 요약
    @Column(name = "REASON", nullable = false)
    private String reason;

    // 상세 사유
    @Column(name = "DETAIL", columnDefinition = "TEXT")
    private String detail;

    // 현재 유효 여부 (기본값 Y -> true)
    @Convert(converter = BooleanToYNConverter.class) // [수정] 컨버터 적용
    @Column(name = "ACTIVE_YN", nullable = false, length = 1)
    @Builder.Default
    private boolean activeYn = true; // [수정] String("Y") -> boolean(true)로 변경

    // 블랙 처리 시각
    @Column(name = "BLOCKED_AT", nullable = false)
    private LocalDateTime blockedAt;

    // 블랙 해제 시각
    @Column(name = "UNBLOCKED_AT")
    private LocalDateTime unblockedAt;

    // 저장 전 자동 실행: blockedAt이 비어있으면 현재 시간으로 채움
    @PrePersist
    public void prePersist() {
        if (this.blockedAt == null) {
            this.blockedAt = LocalDateTime.now();
        }
    }
}
