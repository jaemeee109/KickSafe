package com.kicksafe.member.controller;

import com.kicksafe.global.security.UserPrincipal;
import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.dto.MemberUpdateDTO;
import com.kicksafe.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController // JSON 데이터를 반환하는 컨트롤러
@RequestMapping("/members")
@RequiredArgsConstructor // final 이 붙은 필드(MemberService)를 자동으로 주입
public class MemberController {

    private final MemberService memberService;

    /**
     * [1. 내 정보 조회 API]
     * URL: GET ../members/readOne
     * 설명: 현재 로그인한 사용자의 상세 정보를 가져옴
     */
    @GetMapping("/readOne")
    public ResponseEntity<MemberResponseDTO> readOne(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 1. 로그 남기기 (누가 요청했는지 확인)
        log.info("내 정보 조회 요청(readOne) - 사용자 ID: {}", userPrincipal.getId());

        // 2. 서비스에게 "이 사람 정보 좀 가져와줘"라고 시키기
        MemberResponseDTO result = memberService.getMyInfo(userPrincipal.getId());

        // 3. 결과 반환 (200 OK)
        return ResponseEntity.ok(result);
    }

    /**
     * [2. 내 정보 수정 API]
     * URL: PUT ../members/update
     * Body: { "nickname": "변경할닉네임" }
     * 설명: 사용자의 정보를 수정
     */
    @PutMapping("/update")
    public ResponseEntity<MemberResponseDTO> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody MemberUpdateDTO updateDTO) { // @RequestBody: JSON 데이터를 객체로 받음

        log.info("내 정보 수정 요청(update) - 사용자 ID: {}", userPrincipal.getId());

        // 서비스에게 수정 요청 (ID와 변경할 데이터 전달)
        MemberResponseDTO updatedMember = memberService.updateMember(userPrincipal.getId(), updateDTO);

        return ResponseEntity.ok(updatedMember);
    }

    /**
     * [3. 회원 탈퇴 API]
     * URL: DELETE ../members/delete
     * 설명: 회원을 탈퇴 처리(상태 변경)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("회원 탈퇴 요청(delete) - 사용자 ID: {}", userPrincipal.getId());

        // 서비스에게 탈퇴 처리 요청
        memberService.withdrawMember(userPrincipal.getId());

        // 탈퇴는 돌려줄 데이터가 딱히 없으니 메시지만 전달
        return ResponseEntity.ok("성공적으로 탈퇴(삭제) 처리되었습니다.");
    }

    /**
     * [4. 닉네임 중복 확인 API]
     * URL: GET /members/check-nickname?nickname=멋쟁이
     * 설명: 해당 닉네임이 이미 사용 중인지 확인
     * 리턴: true(중복/사용불가), false(사용가능)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {

        log.info("닉네임 중복 확인 요청: {}", nickname);

        boolean isDuplicate = memberService.checkNicknameDuplicate(nickname);

        // true 면 "중복됨(나쁜거)", false 면 "중복안됨(좋은거)"
        return ResponseEntity.ok(isDuplicate);
    }
}