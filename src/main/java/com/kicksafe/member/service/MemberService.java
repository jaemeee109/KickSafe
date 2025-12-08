package com.kicksafe.member.service;

import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.dto.MemberUpdateDTO;

/**
 * 1. 역할: 회원 관련 기능(메뉴판) 정의
 * 2. 기능: 내 정보 조회, 내 정보 수정, 회원 탈퇴
 */
public interface MemberService {

    // 내 정보 조회
    MemberResponseDTO getMyInfo(Long memberId);

    // 닉네임 중복 여부
    boolean checkNicknameDuplicate(String nickname);

    // 내 정보 수정 (닉네임 변경)
    MemberResponseDTO updateMember(Long memberId, MemberUpdateDTO updateDTO);

    // 회원 탈퇴 (상태 변경: ACTIVE -> WITHDRAWN)
    void withdrawMember(Long memberId);
}