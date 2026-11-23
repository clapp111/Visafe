package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.LoginRequest;
import com.visafe.global.exception.ErrorResponse;
import com.visafe.global.jwt.RefreshTokenCookie;
import com.visafe.global.jwt.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "로그인·토큰 재발급·로그아웃")
public interface AuthControllerDocument {

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인한다. 리프레시 토큰은 HttpOnly 쿠키로 내려준다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "로그인 성공 (Set-Cookie로 리프레시 토큰 발급)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(이메일·비밀번호가 비었거나 이메일 형식이 잘못됨)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "AUTH_LOGIN_FAILED(이메일 또는 비밀번호가 틀림)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request);

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰 쿠키로 액세스·리프레시 토큰을 새로 발급한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "재발급 성공 (Set-Cookie로 새 리프레시 토큰 발급)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "JWT_REFRESH_NOT_MATCH(이미 바뀌었거나 다른 기기에서 로그인한 리프레시 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description =
                        "JWT_REFRESH_MISSING(쿠키 없음) · JWT_REFRESH_NOT_FOUND(저장된 리프레시 토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> reissue(@CookieValue(RefreshTokenCookie.NAME) String refreshToken);

    @Operation(summary = "로그아웃", description = "저장된 리프레시 토큰을 지우고 쿠키를 만료시킨다.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "로그아웃 성공 (Set-Cookie로 쿠키 만료)")})
    ResponseEntity<Void> logout(@CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken);
}
