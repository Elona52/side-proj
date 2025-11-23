package com.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter() {
        return new SessionAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 운영 환경 여부 확인
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = activeProfiles.length > 0 && 
                              java.util.Arrays.asList(activeProfiles).contains("prod");
        
        if (activeProfiles.length == 0) {
            log.warn("⚠️ 프로파일이 설정되지 않았습니다. 개발 환경 모드로 동작합니다.");
        } else {
            log.info("🔒 현재 프로파일: {}, 운영 모드: {}", 
                    String.join(", ", activeProfiles), isProduction ? "ON" : "OFF");
        }

        if (isProduction) {
            // ============================================================
            // 운영 환경: 보안 강화 설정
            // ============================================================
            log.info("🔒 운영 환경 보안 설정 적용");
            
            http
                // CSRF 보호 활성화
                .csrf(csrf -> {
                    org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository csrfTokenRepository = 
                        new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository();
                    csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
                    csrf.csrfTokenRepository(csrfTokenRepository)
                        // 일부 읽기 전용 API는 CSRF 예외
                        .ignoringRequestMatchers(
                            "/api/auction-info/**",
                            "/api/favorites/check"
                        );
                })
                // 인가 설정
                .authorizeHttpRequests(authorize -> authorize
                    // 공개 경로 (인증 불필요)
                    .requestMatchers(
                        "/", "/main", "/memberLogin", "/login", "/logout",
                        "/memberJoin", "/idCheck.ajax",
                        "/findId", "/findPassword", "/resetPassword",
                        "/auction/**", "/board/**",
                        "/swagger-ui/**", "/api-docs/**",
                        "/payment/callback", "/payment/success", "/payment/fail",
                        "/kamco-items/**",
                        // 공개 API 경로 (인증 불필요 - 읽기 전용)
                        "/api/auction-info/**",
                        "/api/discount-50",
                        "/api/sido/**",
                        "/api/search",
                        "/api/favorites/check",
                        "/api/all",
                        "/api/new",
                        "/api/today-closing",
                        "/api/main-data",
                        "/api-item-detail",
                        "/css/**", "/js/**", "/img/**", "/static/**",
                        "/error", "/favicon.ico"
                    ).permitAll()
                    // 나머지 /api/** 경로는 인증 필요 (기존 세션의 isLogin=true 확인)
                    .requestMatchers("/api/**").authenticated()
                    // 기타 모든 요청은 인증 필요
                    .anyRequest().authenticated()
                )
                // 예외 처리: 인증 실패 시 401, 권한 없음 시 403
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.warn("인증 실패: {} - {}", request.getRequestURI(), authException.getMessage());
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"인증이 필요합니다.\"}");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.warn("접근 거부: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"접근 권한이 없습니다.\"}");
                    })
                )
                // 기존 로그인 로직 사용 (MemberController의 /login)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 기존 세션 인증 필터 추가
                .addFilterBefore(sessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
                
            log.info("✅ 운영 환경 설정 완료: /api/** 경로 보호, CSRF 활성화");
            
        } else {
            log.info("🔓 개발 환경 설정 적용 (모든 경로 허용)");
            
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                    .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        }
        
        return http.build();
    }

    public static class SessionAuthenticationFilter 
            extends org.springframework.web.filter.OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                // 기존 로그인 시스템에서 저장한 세션 정보 확인
                Boolean isLogin = (Boolean) session.getAttribute("isLogin");
                String loginId = (String) session.getAttribute("loginId");
                String type = (String) session.getAttribute("type");
                
                // 로그인되어 있으면 Spring Security 인증 객체 생성
                if (isLogin != null && isLogin && loginId != null) {
                    org.springframework.security.core.Authentication authentication = 
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            loginId,  // principal (사용자 식별자)
                            null,     // credentials (비밀번호는 저장하지 않음)
                            java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_" + (type != null ? type : "USER")
                                )
                            )
                        );
                    
                    // SecurityContext에 인증 정보 저장
                    org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
                }
            }
            
            filterChain.doFilter(request, response);
        }
    }
}

