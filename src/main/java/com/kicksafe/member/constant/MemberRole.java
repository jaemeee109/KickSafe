package com.kicksafe.member.constant;

public enum MemberRole {
    GUEST, // [추가] 첫 가입 시 (닉네임 설정 전)
    USER, // 닉네임 변경 후 -> 일반 회원
    ADMIN   // 관리자
}
