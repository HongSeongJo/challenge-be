package com.challenge.backend.auth.security;

import com.challenge.backend.auth.jwt.JwtTokenProvider;
import com.challenge.backend.auth.oauth2.KakaoOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            KakaoOAuth2SuccessHandler kakaoOAuth2SuccessHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // 카카오 로그인의 authorization_code 흐름(state 저장)은 세션이 필요해서 STATELESS로는 못 쓴다.
                // JWT로 보호되는 API는 세션을 전혀 사용하지 않으므로 IF_REQUIRED로도 사실상 stateless하게 동작한다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> response.sendError(HttpStatus.UNAUTHORIZED.value())))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(kakaoOAuth2SuccessHandler)
                        .failureHandler((request, response, ex) -> response.sendError(HttpStatus.UNAUTHORIZED.value())))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
