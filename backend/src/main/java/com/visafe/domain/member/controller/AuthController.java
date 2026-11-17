package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.LoginRequest;
import com.visafe.domain.member.service.AuthService;
import com.visafe.global.jwt.JwtProperties;
import com.visafe.global.jwt.RefreshTokenCookie;
import com.visafe.global.jwt.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "로그인·토큰 재발급·로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인한다. 리프레시 토큰은 HttpOnly 쿠키로 내려준다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request.email(), request.password()));
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰 쿠키로 액세스·리프레시 토큰을 새로 발급한다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@CookieValue(RefreshTokenCookie.NAME) String refreshToken) {
        return withRefreshCookie(authService.reissue(refreshToken));
    }

    @Operation(summary = "로그아웃", description = "저장된 리프레시 토큰을 지우고 쿠키를 만료시킨다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.expire().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(TokenResponse tokens) {
        String cookie = RefreshTokenCookie.create(tokens.getRefreshToken(), jwtProperties.refreshTokenValidity())
                .toString();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie).body(tokens);
    }
}
