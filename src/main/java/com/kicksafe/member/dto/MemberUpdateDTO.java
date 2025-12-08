package com.kicksafe.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1. 용도: 회원이 "내 정보 수정" 페이지에서 닉네임 등을 바꿀 때 보내는 데이터를 담음
 * 2. 내용: 변경할 닉네임
 */
@Data
@NoArgsConstructor
public class MemberUpdateDTO {

    // 변경할 닉네임
    // (만약 닉네임 중복 체크가 필요하면 컨트롤러나 서비스에서 추가 검사함)
    private String nickname;
}