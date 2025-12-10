package com.kicksafe.member.service;

import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.dto.MemberUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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


    // =========================================================
    // [관리자용 기능 추가]
    // =========================================================

    // 5. 전체 회원 목록 조회 (페이징)
    Page<MemberResponseDTO> getAllMembers(Pageable pageable, String keyword);

    // 6. 회원 강제 정지 (Ban)
    // [수정] adminId 파라미터 추가 (누가 정지시켰는지 기록하기 위해)
    void banMember(Long memberId, Long adminId);

    // 정지 해제
    void unbanMember(Long memberId); // [추가] 정지 해제

    // [★추가★] 관리자용: 회원에게 관리자 권한 부여 (승격)
    void promoteToAdmin(Long memberId);
}