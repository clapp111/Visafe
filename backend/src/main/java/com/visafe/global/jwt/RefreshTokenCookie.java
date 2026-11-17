package com.visafe.global.jwt;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * 리프레시 토큰 쿠키. JavaScript에서 읽을 수 없고(HttpOnly), 인증 API(/api/v1/auth)로만 전송된다.
 */
public final class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    private static final String PATH = "/api/v1/auth";

    private RefreshTokenCookie() {}

    public static ResponseCookie create(String refreshToken, Duration maxAge) {
        return build(refreshToken, maxAge);
    }

    public static ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private static ResponseCookie build(String refreshToken, Duration maxAge) {
        return ResponseCookie.from(NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path(PATH)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
