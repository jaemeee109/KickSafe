package com.kicksafe.auth.info;

import java.util.Map;

/**
 * 카카오 유저 정보 번역기]
 * 1. 역할: 카카오 로그인 서버에서 보내준 복잡한 JSON 데이터를 우리가 쓰기 편하게 정리하는 클래스
 * 2. 특징: 구글은 정보가 한 곳에 다 있지만, 카카오는 상자 안에 상자가 들어있는 구조(중첩)입니다.
 * - 전체 데이터 (attributes)
 * ㄴ id (회원번호)
 * ㄴ kakao_account (계정 정보 상자)
 * ㄴ email
 * ㄴ profile (프로필 정보 상자)
 * ㄴ nickname
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    // 1. 카카오에서 보내준 원본 데이터 전체를 담는 큰 가방
    private Map<String, Object> attributes;

    // 2. 'kakao_account' 라는 이름의 중간 상자 (이메일 등이 들어있음)
    private Map<String, Object> kakaoAccount;

    // 3. 'profile' 이라는 이름의 작은 상자 (닉네임, 프사 등이 들어있음)
    private Map<String, Object> profile;

    /**
     * [생성자: 데이터 분해 및 정리]
     * 카카오에서 받은 전체 데이터를 쪼개서 각 변수에 담아두는 작업을 진행
     */
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        // (1) 전체 데이터 저장
        this.attributes = attributes;

        // (2) 중간 상자 꺼내기 (kakao_account)
        // attributes.get("kakao_account")의 결과는 Object 타입이라서,
        // (Map<String, Object>)를 붙여서 "이건 맵(Map)이야!" 라고 강제로 형변환(Casting) 해줌
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        // (3) 작은 상자 꺼내기 (profile)
        // kakaoAccount 상자 안에서 또 "profile"을 꺼냅니다.
        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }

    /**
     * 제공자 이름 반환 (우리는 "kakao"라고 부르기로 함)
     */
    @Override
    public String getProvider() {
        return "kakao";
    }

    /**
     * 제공자 ID 반환 (카카오 회원번호)
     * 카카오는 ID를 숫자(Long)로 줌 (예: 123456789)
     * 하지만 우리는 DB에 문자로 저장하기로 했으니 String.valueOf()로 숫자를 문자로 바꿈
     */
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    /**
     * 이메일 반환
     * 이메일은 중간 상자(kakaoAccount) 안에 들어있음
     */
    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }

    /**
     * 닉네임 반환
     * 닉네임은 가장 안쪽 상자(profile) 안에 들어있음
     */
    @Override
    public String getName() {
        return (String) profile.get("nickname");
    }
}