package com.visafe.global.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 (application.yml의 jwt.*).
 *
 * @param secret HS256 서명 키. UTF-8 기준 32바이트 이상이어야 한다.
 * @param accessTokenValidity 액세스 토큰 유효 시간
 * @param refreshTokenValidity 리프레시 토큰 유효 시간 (쿠키 만료와 같음)
 */
@ConfigurationProperties("jwt")
public record JwtProperties(String secret, Duration accessTokenValidity, Duration refreshTokenValidity) {}
