package com.visafe.global.config;

import com.visafe.domain.member.enums.MemberRole;
import com.visafe.global.exception.ErrorResponseWriter;
import com.visafe.global.jwt.JwtAccessDeniedHandler;
import com.visafe.global.jwt.JwtAuthenticationEntryPoint;
import com.visafe.global.jwt.JwtAuthenticationFilter;
import com.visafe.global.jwt.JwtExceptionFilter;
import com.visafe.global.jwt.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

@Configuration
@EnableWebSecurity
// 계약 API는 경로(/api/v1/contracts/**)를 역할끼리 나눠 쓰므로, 역할 검사는 컨트롤러의 @PreAuthorize로 한다.
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        // Swagger
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs",
        "/v3/api-docs/**",
        // 로그인·토큰 재발급·로그아웃
        "/api/v1/auth/**",
        // 확인번호 공개 조회·파일 대조
        "/api/v1/lookup/**",
        // 헬스 체크
        "/actuator/health",
        "/actuator/health/**",
        // 에러 응답 (없으면 404·500이 401로 바뀜)
        "/error"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final ErrorResponseWriter errorResponseWriter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 필터는 빈으로 등록하지 않고 여기서 만든다. (빈이면 서블릿 필터로도 자동 등록되어 두 번 실행될 수 있음)
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        JwtExceptionFilter jwtExceptionFilter = new JwtExceptionFilter(errorResponseWriter);

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole(MemberRole.ADMIN.name())
                        .anyRequest()
                        .authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 로그인 시 이메일로 회원을 찾고(MemberDetailsService) 비밀번호를 BCrypt로 비교한다. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
