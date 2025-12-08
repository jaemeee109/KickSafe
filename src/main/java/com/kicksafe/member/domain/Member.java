package com.kicksafe.member.domain;

import com.kicksafe.global.common.BaseEntity;
import com.kicksafe.member.constant.MemberRole;
import com.kicksafe.member.constant.MemberStatus;
import com.kicksafe.member.constant.Provider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MEMBER")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEMBER_ID")
    private Long id;

    // 소셜 제공자 (KAKAO, NAVER, GOOGLE, INSTAGRAM, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false)
    private Provider provider;

    // 소셜 제공자 내 유저 식별자
    @Column(name = "PROVIDER_USER_ID", nullable = false, length = 100)
    private String providerUserId;

    // 관리자용 비밀번호 (소셜 회원은 NULL)
    @Column(name = "PASSWORD")
    private String password;

    // 닉네임
    @Column(name = "NICKNAME", length = 50)
    private String nickname;

    // 권한 (기본값 USER)
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    // 계정 상태 (기본값 ACTIVE)
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    // == 비즈니스 로직 메서드 ==
    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changeStatus(MemberStatus newStatus) {
        this.status = newStatus;
    }

    // [추가] 닉네임 업데이트 편의 메서드
    // 변경된 자기 자신(this)을 반환해야 체이닝(.map)이 가능함
    public Member update(String nickname) {
        this.nickname = nickname;
        return this;
    }
}

// (access = AccessLevel.PROTECTED)
// JPA(데이터베이스)는 쓰게 해주고, 개발자가 실수로 쓰는 건 막기 위해서