package com.kicksafe.global.security;

import com.kicksafe.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User; // [추가됨] 소셜 로그인용 인터페이스

import java.util.Collection;
import java.util.Collections;
import java.util.Map; // [추가됨] 소셜 데이터 담을 그릇

/**
 * [회원 보안 명찰 (여권)]
 * 1. UserDetails: 일반 로그인할 때 쓰는 신분증 인터페이스
 * 2. OAuth2User: 소셜 로그인할 때 쓰는 신분증 인터페이스 (추가됨!)
 * <p>
 * -> 이 두 가지를 모두 구현해서, 어떤 방식으로 로그인하든 다 처리할 수 있는 "만능 신분증"으로 만듭니다.
 */
@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    // 1. 기존 필드들 (필수 정보)
    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // 2. [추가됨] 소셜 로그인에서 받은 원본 데이터 (구글이 준 JSON 통째로 저장)
    // 일반 로그인할 때는 비어있을 수 있어서 final을 뺍니다.
    private Map<String, Object> attributes;

    // [수정] 생성자 (Lombok @RequiredArgsConstructor 대신 직접 만듦)
    // 이유: 필드가 복잡해져서 우리가 직접 초기화 순서를 정하는 게 안전함
    public UserPrincipal(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    // =================================================================
    // [1] 일반 로그인용 팩토리 메서드 (기존과 동일)
    // =================================================================
    public static UserPrincipal create(Member member) {
        return new UserPrincipal(
                member.getId(),
                member.getProviderUserId(),
                member.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );
    }

    // =================================================================
    // [2] 소셜 로그인용 팩토리 메서드 (여기가 없어서 에러 났었음!)
    // =================================================================
    public static UserPrincipal create(Member member, Map<String, Object> attributes) {
        // 1. 위에서 만든 일반용 create()를 재사용해서 기본 틀을 만듦
        UserPrincipal userPrincipal = UserPrincipal.create(member);

        // 2. 소셜 정보(attributes)를 추가로 장착함
        userPrincipal.setAttributes(attributes);

        return userPrincipal;
    }

    // =================================================================
    // [OAuth2User 인터페이스 구현] 소셜 로그인 관련 메서드
    // =================================================================

    // 소셜 정보를 달라고 할 때 줌
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 소셜 정보를 저장하는 세터 (create 메서드에서 씀)
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    // 소셜에서의 이름(ID)을 달라고 할 때 -> 우리는 email(providerUserId)을 식별자로 씀
    @Override
    public String getName() {
        return email;
    }

    // =================================================================
    // [UserDetails 인터페이스 구현] 일반 로그인 관련 메서드 (기존 유지)
    // =================================================================

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}