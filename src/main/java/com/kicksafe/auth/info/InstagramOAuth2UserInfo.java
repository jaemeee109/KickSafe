package com.kicksafe.auth.info;

import java.util.Map;

public class InstagramOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public InstagramOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "instagram";
    }

    @Override
    public String getEmail() {
        // [중요] 인스타그램은 API 정책상 이메일을 절대 주지 않습니다.
        // 하지만 우리 DB는 이메일이 필수(Unique Key)이므로, 임시 이메일을 생성합니다.
        // 예: "my_nickname@instagram.auth"
        return getName() + "@instagram.auth";
    }

    @Override
    public String getName() {
        return (String) attributes.get("username");
    }
}