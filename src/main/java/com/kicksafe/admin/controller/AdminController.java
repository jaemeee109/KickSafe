package com.kicksafe.admin.controller;

import com.kicksafe.global.security.UserPrincipal;
import com.kicksafe.member.dto.MemberResponseDTO;
import com.kicksafe.member.repository.MemberBlacklistRepository;
import com.kicksafe.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin") // 모든 URL 이 /admin 으로 시작
@RequiredArgsConstructor
public class AdminController {

    // 기존 MemberService 를 주입받아 사용 (로직 재사용)
    private final MemberService memberService;
    private final MemberBlacklistRepository memberBlacklistRepository;

    /**
     * 1. 회원 전체 목록 조회
     * URL: GET /admin/members
     */
    @GetMapping("/members")
    public ResponseEntity<Page<MemberResponseDTO>> getAllMembers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword) { // [추가] 검색어 받기 (필수 아님)

        return ResponseEntity.ok(memberService.getAllMembers(pageable, keyword));
    }

    /**
     * 2. 회원 강제 추방 (Ban)
     * URL: POST /admin/members/ban/{memberId}
     */
    @PostMapping("/members/ban/{memberId}")
    public ResponseEntity<String> banMember(
            @AuthenticationPrincipal UserPrincipal userPrincipal, // 현재 관리자 정보
            @PathVariable Long memberId) {

        // 서비스에 (정지당할 사람 ID, 정지시키는 관리자 ID) 둘 다 전달
        memberService.banMember(memberId, userPrincipal.getId());

        return ResponseEntity.ok("해당 회원을 정지 처리하고 기록을 저장했습니다.");
    }

    /**
     * 3. 회원 정지 해제 (Unban)
     * URL: POST /admin/members/unban/{memberId}
     */
    @PostMapping("/members/unban/{memberId}")
    public ResponseEntity<String> unbanMember(@PathVariable Long memberId) {

        memberService.unbanMember(memberId);
        return ResponseEntity.ok("해당 회원의 정지를 해제했습니다.");
    }

    /**
     * 4. 회원 관리자 승격
     * URL: POST /admin/members/promote/{memberId}
     */
    @PostMapping("/members/promote/{memberId}")
    public ResponseEntity<String> promoteMember(@PathVariable Long memberId) {

        memberService.promoteToAdmin(memberId);
        return ResponseEntity.ok("해당 회원을 관리자로 승격시켰습니다.");
    }
}