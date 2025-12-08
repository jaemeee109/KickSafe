package com.kicksafe.global.security;

import com.kicksafe.auth.dto.TokenResponseDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * [클래스 설명: JWT 토큰 관리자 (조폐공사 + 검문소)]
 * 1. 조폐공사 역할: 로그인한 유저에게 'Access Token' 과 'Refresh Token' 을 발급해줍니다.
 * 2. 검문소 역할: 들어온 토큰이 위조되지는 않았는지, 만료되지는 않았는지 검사합니다.
 * 3. 신원확인 역할: 토큰 안에 들어있는 정보를 꺼내서 "이 사람은 누구입니다"라고 알려줍니다.
 */
@Slf4j // 로그를 찍기 위한 어노테이션
@Component // 스프링이 시작될 때 이 클래스를 메모리에 띄워서 관리하게 함 (Bean 등록)
public class JwtTokenProvider {

    // 토큰 내부에서 권한 정보(예: ROLE_USER)를 저장할 때 사용할 키의 이름
    private static final String AUTHORITIES_KEY = "auth";
    // 토큰의 타입 (일반적으로 "Bearer"라는 단어를 앞에 붙여서 사용함)
    private static final String BEARER_TYPE = "Bearer";

    // Access Token의 수명 (30분 = 1000밀리초 * 60초 * 30분)
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30;
    // Refresh Token의 수명 (7일 = 1000밀리초 * 60초 * 60분 * 24시간 * 7일)
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;

    // 암호화/복호화에 사용할 비밀키를 저장할 변수
    private final Key key;

    // [수정 포인트 1] DB 에서 유저 정보를 가져오기 위해 '심부름꾼(Service)'을 선언
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * [생성자: 초기화 작업]
     * 1. application.properties 에서 'jwt.secret' 값을 가져옵니다.
     * 2. 가져온 비밀키를 암호화 알고리즘에 쓸 수 있는 형태로 바꿉니다.
     * 3. [수정 포인트 2] 유저 정보를 가져올 서비스를 주입받습니다.
     */
    public JwtTokenProvider(@Value("${jwt.secret:v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c9v8z2a3x7c91234567890}") String secretKey,
                            CustomUserDetailsService customUserDetailsService) {
        // 비밀키는 사람이 읽을 수 있는 문자열이므로, 컴퓨터가 쓰는 바이트 배열로 변환(디코딩)
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        // 그 바이트 배열로 'HMAC-SHA' 알고리즘용 암호화 키 객체 생성
        this.key = Keys.hmacShaKeyFor(keyBytes);
        // 주입받은 서비스를 멤버 변수에 저장 (나중에 쓰려고)
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * [기능 1: 토큰 발급]
     * 로그인에 성공한 유저 정보를 받아서 Access Token 과 Refresh Token 을 만들어줍니다.
     *
     * @param authentication 스프링 시큐리티가 인증한 유저 정보
     * @return TokenResponseDTO (토큰들이 담긴 가방)
     */
    public TokenResponseDTO generateTokenDto(Authentication authentication) {

        // 1. 유저의 권한들(ROLE_USER, ROLE_ADMIN 등)을 가져와서 콤마(,)로 연결된 문자열로 만듦
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 2. 현재 시간 가져오기 (만료 시간 계산용)
        long now = (new Date()).getTime();

        // 3. Access Token 생성 (출입증)
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME); // 30분 뒤
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())       // 토큰의 주인 ID (예: kakao_12345)
                .claim(AUTHORITIES_KEY, authorities)        // 권한 정보 넣기 ("auth": "ROLE_USER")
                .setExpiration(accessTokenExpiresIn)        // 유효기간 설정
                .signWith(key, SignatureAlgorithm.HS512)    // 비밀키로 서명 (도장 쾅!)
                .compact();                                 // 문자열로 변환

        // 4. Refresh Token 생성 (재발급용 티켓)
        // [수정 포인트] 여기가 문제였습니다! 재발급할 때도 권한 정보가 필요하므로 추가했습니다.
        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())       // [추가] 토큰 주인 ID (누구 건지는 알아야 하니까)
                .claim(AUTHORITIES_KEY, authorities)        // [추가] 권한 정보 ("auth": "ROLE_USER") -> 이게 없어서 에러 났음!
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_TIME)) // 7일 뒤
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        // 5. 만든 토큰들을 DTO(택배 상자)에 담아서 리턴
        return TokenResponseDTO.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * [기능 2: 인증 정보 조회 (가장 중요한 수정 부분!)]
     * 토큰을 해독해서 "이 토큰의 주인은 누구인가?"를 알아내고,
     * DB에서 그 사람의 진짜 정보를 가져와서 스프링 시큐리티에게 넘겨줍니다.
     * * [이전 문제] 그냥 껍데기만 있는 유저 객체를 만들어서 넘겼더니, 컨트롤러에서 UserPrincipal로 변환 못해서 에러 남.
     * [해결 방법] DB에서 정보를 조회해오는 'CustomUserDetailsService'를 사용하여 진짜 'UserPrincipal'을 가져옴.
     */
    public Authentication getAuthentication(String accessToken) {
        // 1. 토큰을 복호화해서 안에 담긴 정보(Claims)를 꺼냄
        Claims claims = parseClaims(accessToken);

        // 2. 토큰 안에 권한 정보가 없으면 "이건 못 쓰는 토큰이야"라고 에러 발생
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // [수정 포인트 3] DB에서 유저의 진짜 정보를 조회해옵니다.
        // claims.getSubject()에는 유저의 ID(예: providerUserId)가 들어있습니다.
        // loadUserByUsername 메서드는 반환 타입이 UserDetails지만, 실제로는 우리가 만든 'UserPrincipal'이 들어있습니다.
        UserDetails principal = customUserDetailsService.loadUserByUsername(claims.getSubject());

        // 4. 찾아온 진짜 유저 정보(principal)를 담아서 Authentication 객체를 만듦
        // 이렇게 해야 컨트롤러의 @AuthenticationPrincipal에서 UserPrincipal을 꺼낼 수 있음!
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    /**
     * [기능 3: 토큰 유효성 검사]
     * 프론트엔드가 보낸 토큰이 위조되었거나 만료되지 않았는지 확인합니다.
     *
     * @param token 검사할 토큰 문자열
     * @return true(정상), false(문제 있음)
     */
    public boolean validateToken(String token) {
        try {
            // 비밀키로 토큰을 뜯어봅니다. (서명이 맞는지 확인)
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 에러가 안 나면 정상 토큰!
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다."); // 위조된 토큰
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다."); // 유효기간 지남 (로그아웃 시켜야 함)
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다."); // 토큰이 비어있거나 형식이 이상함
        }
        return false; // 문제가 있으면 false 리턴
    }

    /**
     * [내부 기능: 토큰 뜯어보기]
     * 토큰 문자열을 풀어서 그 안의 내용물(Claims)을 꺼내는 메서드입니다.
     * 만료된 토큰이라도 일단 안에 든 정보(ID 등)는 필요할 때가 있어서 예외 처리를 따로 함.
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            // 만료되어도 안에 있던 정보는 꺼내서 보여줌
            return e.getClaims();
        }
    }
}