package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.LoginRequest;
import com.visafe.domain.member.service.AuthService;
import com.visafe.global.jwt.JwtProperties;
import com.visafe.global.jwt.RefreshTokenCookie;
import com.visafe.global.jwt.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocument {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Override
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request.email(), request.password()));
    }

    @Override
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@CookieValue(RefreshTokenCookie.NAME) String refreshToken) {
        return withRefreshCookie(authService.reissue(refreshToken));
    }

    @Override
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
