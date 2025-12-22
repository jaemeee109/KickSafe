package com.kicksafe.global.security;

import com.kicksafe.auth.handler.OAuth2FailureHandler;
import com.kicksafe.auth.handler.OAuth2SuccessHandler;
import com.kicksafe.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration // "이것은 설정 파일입니다"라고 스프링에게 알림
@EnableWebSecurity // 스프링 시큐리티의 보안 기능을 활성화함
@RequiredArgsConstructor
public class SecurityConfig {

    // 우리가 만든 보안 관련 부품들 주입
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    /**
     * [1. 비밀번호 암호화 기계 등록]
     * DB에 비밀번호를 저장할 때 "1234"라고 그대로 저장하면 큰일 납니다.
     * 이 기계를 사용해서 "$2a$10$..." 같은 알 수 없는 문자로 변환해서 저장합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * [2. 보안 필터 체인 (핵심 설정)]
     * 스프링 시큐리티의 모든 보안 규칙을 여기서 정합니다.
     * "이 주소는 통과시켜!", "로그인은 이렇게 해!", "에러 나면 저리로 가!" 등을 설정합니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // =========================================================
                // [추가됨] CORS 설정 (가장 먼저 적용)
                // 시큐리티가 "너 누구야!" 하고 막기 전에
                // "잠깐, 얘는 리액트(3000번)에서 온 친구니까 예비요청(OPTIONS)은 통과시켜줘"라고 설정함
                // =========================================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // =========================================================
                // [기본 설정 비활성화]
                // 우리는 REST API 서버(백엔드)라서 브라우저 로그인 화면이나 세션이 필요 없습니다.
                // =========================================================
                .httpBasic(AbstractHttpConfigurer::disable) // 팝업창 뜨는 기본 로그인 방식 끄기
                .csrf(AbstractHttpConfigurer::disable)      // 위조 요청 방지 끄기 (JWT 쓸 때는 필요 없음)

                // [중요] 폼 로그인 비활성화 (No static resource login 에러 해결!)
                // 이걸 끄지 않으면 스프링이 자꾸 "/login.html"을 찾으려고 해서 에러가 납니다.
                .formLogin(AbstractHttpConfigurer::disable)

                // =========================================================
                // [세션 설정: STATELESS]
                // "서버야, 기억력 끄고 살아라!"
                // JWT 토큰 안에 유저 정보가 다 들어있으므로, 서버가 굳이 세션에 기억할 필요가 없습니다.
                // =========================================================
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // =========================================================
                // [에러 처리 설정]
                // =========================================================
                .exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 에러 (로그인 안 됨) 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403 에러 (권한 없음) 처리
                )

                // =========================================================
                // [의심스러운 링크 삭제됨]
                // =========================================================
                .authorizeHttpRequests(request -> request
                        // 1. "누구나 들어와도 되는 곳" (로그인, 소셜연동, 이미지, 에러페이지)
                        // "/oauth2/**"가 있어야 구글/카카오 로그인이 작동합니다.
                        .requestMatchers("/auth/**", "/error", "/images/**", "/login/**", "/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                        // =========================================================
                        // [★추가★] 관리자 페이지 보안 설정
                        // "/admin"으로 시작하는 모든 주소는 DB의 ROLE 이 "ADMIN"인 사람만 가능
                        // (스프링 시큐리티가 자동으로 "ROLE_ADMIN"인지 검사합니다)
                        // =========================================================
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 2. 그 외 모든 곳은 "출입증(토큰)"이 있어야 함
                        .anyRequest().authenticated())

                // =========================================================
                // [소셜 로그인(OAuth2) 설정]
                // =========================================================
                .oauth2Login(oauth2 -> oauth2
                        // 유저 정보(이메일, 이름 등)를 가져오는 서비스 연결
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 로그인이 성공하면 토큰을 발급해주는 핸들러 연결
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))

                // =========================================================
                // [필터 배치]
                // =========================================================
                // 우리가 만든 'JwtAuthenticationFilter' 를 기본 로그인 필터보다 *앞에* 세워둡니다.
                // 그래야 들어오는 요청의 주머니(Header)를 뒤져서 토큰이 있는지 먼저 검사할 수 있습니다.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * [3. CORS(교차 출처 리소스 공유) 설정 상세 정의]
     * 프론트엔드(localhost:3000)가 백엔드(localhost:8080)로 요청을 보낼 때,
     * 브라우저가 "보안상 위험해!" 하고 막는 것을 풀어주는 허가증 발급처입니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 리액트 개발 서버 주소 허용
        configuration.addAllowedOrigin("http://34.50.13.223.nip.io:3000");

        // 2. 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE, OPTIONS 등)
        configuration.addAllowedMethod("*");

        // 3. 모든 헤더 허용 (Authorization, Content-Type 등)
        configuration.addAllowedHeader("*");

        // 4. 자격 증명(쿠키/토큰) 허용
        configuration.setAllowCredentials(true);

        // 5. 위 설정을 모든 주소(/**)에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// PasswordEncoder (비밀번호 암호화):
// 비밀번호는 절대 그대로 저장하면 안 됨
// BCryptPasswordEncoder 는 비밀번호를 알아볼 수 없는 외계어 문자열로 바꿔주는 기계

//SessionCreationPolicy.STATELESS:
// "서버야, 기억력 끄고 살아라!" 라는 뜻.
// JWT 토큰 안에 정보가 다 있으니까, 서버가 굳이 사용자 정보를 세션에 기억할 필요가 없음. 서버 부담을 확 줄여줘.

// .requestMatchers("/auth/**").permitAll():
// 이게 없으면 로그인하기도 전에 "로그인하세요!"라고 막혀버림.
// /auth/login, /auth/signup 같은 주소는 프리패스 시켜주는 설정.