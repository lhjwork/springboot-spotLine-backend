package com.spotline.api.config;

import com.spotline.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public — 읽기 전용
                        .requestMatchers(HttpMethod.GET, "/api/v2/spots/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v2/spotlines/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v2/places/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v2/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v2/comments/**").permitAll()
                        // Swagger UI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Public — QR 스캔 로그 + 조회수 증가 (POST이지만 인증 불필요)
                        .requestMatchers(HttpMethod.POST, "/api/v2/qr/*/scan").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v2/spots/*/view", "/api/v2/spotlines/*/view").permitAll()
                        // Admin 전용 — ROLE_ADMIN 필요
                        .requestMatchers("/api/v2/admin/**").hasRole("ADMIN")
                        // 인증 필요 — 쓰기 작업
                        .requestMatchers(HttpMethod.POST, "/api/v2/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v2/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v2/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v2/**").authenticated()
                        // 나머지 GET은 허용
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
