package com.visafe.global.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT 토큰 발급 응답 DTO")
public class TokenResponse {

    @Schema(description = "인증 타입", example = "Bearer")
    private String grantType;

    @Schema(description = "Access 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJ.....")
    private String accessToken;

    @Schema(description = "Access 토큰 유효 시간(초)", example = "10800")
    private long expiresIn;

    /** 응답 본문에는 넣지 않고 HttpOnly 쿠키로만 내려준다. ({@link RefreshTokenCookie}) */
    @JsonIgnore
    private String refreshToken;

    public static TokenResponse of(String accessToken, Duration accessTokenValidity, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, accessTokenValidity.toSeconds(), refreshToken);
    }
}
