package com.kicksafe.member.dto;

import com.kicksafe.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

/**
 * [응답용 DTO]
 * 내 정보 조회 API를 호출했을 때, 화면(프론트)에 돌려줄 데이터 가방입니다.
 * 비밀번호 같은 민감한 정보는 빼고, 보여줄 것만 담습니다.
 */
@Getter
@Builder
public class MemberResponseDTO {
    private Long id;
    private String email;     // providerUserId (소셜 ID)
    private String nickname;
    private String role;
    private String profileImage; // 나중에 프로필 이미지 URL 이 있다면 추가

    // Member 엔티티를 DTO 로 변환하는 정적 메서드 (팩토리 메서드 패턴)
    public static MemberResponseDTO from(Member member) {
        return MemberResponseDTO.builder().id(member.getId()).email(member.getProviderUserId()) // 우리는 이메일 대신 이걸 식별자로 씀
                .nickname(member.getNickname()).role(member.getRole().name()) // Enum -> String 변환
                .build();
    }
}