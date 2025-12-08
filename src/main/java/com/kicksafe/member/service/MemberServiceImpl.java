package com.kicksafe.member.service;

import com.kicksafe.auth.repository.RefreshTokenRepository;
import com.kicksafe.member.constant.MemberRole;
import com.kicksafe.member.constant.MemberStatus;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.dto.MemberUpdateDTO;
import com.kicksafe.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 성능 최적화 (기본값)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

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
}