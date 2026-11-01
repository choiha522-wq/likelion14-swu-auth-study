package com.example.authstudy.security;

import com.example.authstudy.jwt.JwtAccessDeniedHandler;
import com.example.authstudy.jwt.JwtAuthenticationEntryPoint;
import com.example.authstudy.jwt.JwtAuthenticationFilter;
import com.example.authstudy.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class JwtApiSecurityConfig {
    /** JWT 전용 API 경로만 무상태로 처리하고, 기존 세션 기반 학습 경로는 그대로 둡니다. */
    @Bean
    @Order(1)
    SecurityFilterChain jwtApiFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                          JwtAuthenticationEntryPoint entryPoint,
                                          JwtAccessDeniedHandler deniedHandler) throws Exception {
        http.securityMatcher("/api/**")
                // 이 체인은 Authorization Bearer 헤더 전용이며 브라우저 쿠키 인증을 사용하지 않습니다.
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/verify").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
