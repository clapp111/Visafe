package com.visafe.domain.notification.controller;

import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.notification.dto.response.NotificationListResponse;
import com.visafe.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification", description = "알림 (선택 API)")
public interface NotificationControllerDocument {

    @Operation(summary = "알림 목록", description = "내 알림을 최근 순으로 조회한다. unreadOnly=true면 안 읽은 알림만 본다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = NotificationListResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(page·size 범위 밖)",
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
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<NotificationListResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "알림 읽음 처리", description = "내 알림 하나를 읽음으로 바꾼다. 이미 읽은 알림이면 그대로 둔다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
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
                description = "NOTIFICATION_NOT_FOUND(알림이 없거나 내 알림이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> markRead(@PathVariable Long notificationId, @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "알림 모두 읽음", description = "내 안 읽은 알림을 모두 읽음으로 바꾼다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "모두 읽음 처리 성공"),
        @ApiResponse(
                responseCode = "401",
                description =
                        "AUTH_UNAUTHORIZED(토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> markAllRead(@AuthenticationPrincipal MemberDetails member);
}
