package com.kicksafe.auth.info;

/**
 * [소셜 로그인 정보 표준화 인터페이스]
 * 구글, 카카오, 네이버 등 소셜마다 제공하는 데이터 이름이 다 다름.
 * (예: 구글은 "sub", 네이버는 "id", 카카오는 "id"...)
 * 그래서 이것들을 하나로 통일해서 다루기 위해 만든 "공통 규격"
 */
public interface OAuth2UserInfo {
    String getProvider();   // 어떤 소셜인지? (google, kakao, naver)
    String getProviderId(); // 해당 소셜에서의 고유 식별자 (ID)
    String getEmail();      // 이메일
    String getName();       // 사용자 이름 (닉네임)
}