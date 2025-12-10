package com.kicksafe.member.service;

import com.kicksafe.auth.repository.RefreshTokenRepository;
import com.kicksafe.member.constant.BlacklistType;
import com.kicksafe.member.constant.MemberRole;
import com.kicksafe.member.constant.MemberStatus;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.domain.MemberBlacklist;
import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.dto.MemberUpdateDTO;
import com.kicksafe.member.repository.MemberBlacklistRepository;
import com.kicksafe.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 성능 최적화 (기본값)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberBlacklistRepository memberBlacklistRepository;

    /**
     * [1. 내 정보 조회]
     * DB 에서 회원 정보를 찾아서 DTO(응답용 상자)에 담음
     */
    @Override
    public MemberResponseDTO getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        return MemberResponseDTO.from(member);
    }

    /**
     * [닉네임 중복 체크]
     * 프론트엔드에서 "중복확인" 버튼 눌렀을 때 호출됨.
     *
     * @return true(이미 있음 - 사용불가), false(없음-사용가능)
     */
    @Override
    public boolean checkNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    /**
     * [2. 내 정보 수정]
     * 닉네임을 변경하고, 변경된 정보를 다시 반환
     */
    @Override
    @Transactional
    public MemberResponseDTO updateMember(Long memberId, MemberUpdateDTO updateDTO) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        String newNickname = updateDTO.getNickname();

        // 닉네임 변경 로직
        if (newNickname != null && !newNickname.isEmpty()) {
            if (!member.getNickname().equals(newNickname)
                    && memberRepository.existsByNickname(newNickname)) {
                throw new RuntimeException("이미 사용 중인 닉네임입니다.");
            }
            member.changeNickname(newNickname);

            // [추가] 닉네임을 바꿨으니 GUEST -> USER 로 승격!
            if (member.getRole() == MemberRole.GUEST) {
                member.setRole(MemberRole.USER);
            }
        }
        return MemberResponseDTO.from(member);
    }

    /**
     * [3. 회원 탈퇴]
     * '상태'만 '탈퇴(WITHDRAWN)'로 바꿔서 로그인을 막는 방식(Soft Delete)을 사용
     * 리프레시 토큰도 삭제
     */
    @Override
    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        // 1. 상태를 '탈퇴'로 변경
        member.changeStatus(MemberStatus.WITHDRAWN);

        // 2. 리프레시 토큰 '강제' 삭제
        refreshTokenRepository.deleteByKey(memberId);

        log.info("회원 탈퇴 및 토큰 삭제 완료: ID {}", memberId);
    }

    // =========================================================
    // [관리자용 기능 구현]
    // =========================================================

    /**
     * 전체 회원 목록 조회
     * DB의 모든 회원을 페이지 단위로 가져와서 DTO 로 변환
     */
    @Override
    public Page<MemberResponseDTO> getAllMembers(Pageable pageable, String keyword) {

        // 1. 검색어가 없으면 -> 기존처럼 전체 조회
        if (keyword == null || keyword.trim().isEmpty()) {
            return memberRepository.findAll(pageable)
                    .map(MemberResponseDTO::from);
        }

        // 2. 검색어가 있으면 -> 닉네임 검색 조회
        return memberRepository.findByNicknameContaining(keyword, pageable)
                .map(MemberResponseDTO::from);
    }

    /**
     * 회원 강제 정지 (Ban)
     * 상태를 BLACKLISTED 로 변경하고, 강제 로그아웃(토큰 삭제) 시킴
     */
    @Override
    @Transactional
    public void banMember(Long memberId, Long adminId) { // 파라미터 adminId 추가됨
        // 1. 정지 대상 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("정지 대상을 찾을 수 없습니다."));

        // 2. 관리자 정보 조회 (기록용)
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자 정보를 찾을 수 없습니다."));

        // 3. 상태 변경 (ACTIVE -> BLACKLISTED)
        member.changeStatus(MemberStatus.BLACKLISTED);

        // 4. 강제 로그아웃 (토큰 삭제)
        refreshTokenRepository.deleteByKey(memberId);

        // 5. [★핵심] 블랙리스트 이력 테이블에 저장
        MemberBlacklist blacklist = MemberBlacklist.builder()
                .member(member)
                .admin(admin)
                .type(BlacklistType.OTHER) // 사유가 딱히 없으면 OTHER, 나중에 파라미터로 받아도 됨
                .reason("관리자에 의한 강제 정지")
                .activeYn(true) // 현재 유효한 정지임
                .blockedAt(LocalDateTime.now())
                .build();

        memberBlacklistRepository.save(blacklist);

        log.info("회원 정지 처리 및 DB 기록 완료: 대상 ID {}, 처리자 ID {}", memberId, adminId);
    }

    /**
     * [관리자 기능] 정지 해제 (기록 업데이트 포함)
     */
    @Override
    @Transactional
    public void unbanMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 1. 상태 복구
        member.changeStatus(MemberStatus.ACTIVE);

        // 2. [★핵심] 기존 블랙리스트 기록 '비활성화' 처리
        List<MemberBlacklist> activeBlacklists = memberBlacklistRepository.findByMemberIdAndActiveYnTrue(memberId);
        for (MemberBlacklist b : activeBlacklists) {
            b.setActiveYn(false); // 더 이상 유효하지 않음 (Y -> N)
            b.setUnblockedAt(LocalDateTime.now()); // 해제 시간 기록
        }

        log.info("회원 정지 해제 완료: ID {}", memberId);
    }

    /**
     * [관리자 기능 3] 관리자 권한 승격
     * 해당 회원의 ROLE 을 ADMIN 으로 변경하고,
     * 토큰을 삭제하여 재로그인(권한 갱신)을 유도합니다.
     */
    @Override
    @Transactional
    public void promoteToAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 1. 역할 변경 (USER -> ADMIN)
        member.setRole(MemberRole.ADMIN);

        // 2. 중요: 권한이 바뀌었으니 기존 토큰 무효화 (재로그인 필수)
        refreshTokenRepository.deleteByKey(memberId);

        log.info("관리자 권한 부여 완료: ID {}", memberId);
    }
}