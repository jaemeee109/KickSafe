package com.kicksafe.auth.info;

import java.util.Map;

/**
 * [구글 유저 정보 번역기]
 * 구글에서 보내준 JSON 데이터(Map)를 받아서,
 * 우리 시스템의 표준 규격(OAuth2UserInfo)에 맞게 꺼내주는 클래스.
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    // 소셜 로그인에서 받아온 원본 데이터들(JSON)을 담는 그릇
    private Map<String, Object> attributes;

    // 생성자: 구글 데이터를 받아서 저장
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        // 구글은 고유 ID를 "sub"라는 이름으로 줌
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        // 구글은 이메일을 "email"이라는 이름으로 줌
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        // 구글은 이름을 "name"이라는 이름으로 줌
        return (String) attributes.get("name");
    }
}