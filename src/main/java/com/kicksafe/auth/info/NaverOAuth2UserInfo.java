package com.kicksafe.auth.info;

import java.util.Map;

/**
 * [네이버 유저 정보 변환 클래스]
 * 네이버 로그인 성공 후 넘어온 사용자 정보(JSON)를 우리 시스템에서 쓰기 편하게 변환하는 역할입니다.
 * * [주의 사항]
 * 네이버는 유저 정보를 바로 주지 않고, {"resultcode":..., "response": { "id":..., "email":... }} 처럼
 * 'response' 라는 키 안에 실제 정보를 담아서 줍니다.
 * 그래서 데이터를 꺼낼 때 항상 'response' 맵을 먼저 꺼내야 합니다.
 */
@SuppressWarnings("unchecked")
public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    // 생성자: 로그인 후 받아온 원본 데이터(attributes)를 부모에게 전달
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

    /**
     * [소셜 고유 ID 가져오기]
     * 네이버는 "response" 안에 "id"가 들어있습니다.
     */
    @Override
    public String getProviderId() {
        // 1. "response" 라는 보따리를 먼저 풉니다.
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) return null; // 혹시 비어있으면 null

        // 2. 보따리 안에서 진짜 ID를 꺼냅니다.
        return (String) response.get("id");
    }

    /**
     * [제공자 이름]
     * "naver" 라고 명확하게 리턴해줍니다. (DB에 저장될 값)
     */
    @Override
    public String getProvider() {
        return "naver";
    }

    /**
     * [이메일 가져오기]
     * 마찬가지로 "response" 보따리 안에서 "email"을 꺼냅니다.
     */
    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) return null;

        return (String) response.get("email");
    }

    /**
     * [이름(닉네임) 가져오기]
     * "response" 보따리 안에서 "name"을 꺼냅니다.
     * (네이버 개발자 센터 설정에 따라 name 대신 nickname 을 써야 할 수도 있습니다.)
     */
    @Override
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) return null;

        return (String) response.get("name");
    }
}