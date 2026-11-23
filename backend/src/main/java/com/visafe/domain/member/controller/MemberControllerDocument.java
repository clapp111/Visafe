package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.MemberUpdateRequest;
import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Member", description = "회원")
public interface MemberControllerDocument {

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = MemberResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description =
                        "AUTH_UNAUTHORIZED(토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "MEMBER_NOT_FOUND(회원 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MemberResponse> getMe(@AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "화면 언어 변경", description = "화면 언어(ko, vi)를 저장한다. 응답은 내 정보 조회와 같다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "변경 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = MemberResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(화면 언어가 비었거나 ko·vi가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description =
                        "AUTH_UNAUTHORIZED(토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "MEMBER_NOT_FOUND(회원 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MemberResponse> updateMe(
            @AuthenticationPrincipal MemberDetails member, @Valid @RequestBody MemberUpdateRequest request);
}
