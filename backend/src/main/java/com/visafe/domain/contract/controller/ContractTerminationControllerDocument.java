package com.visafe.domain.contract.controller;

import com.visafe.domain.contract.dto.request.TerminationCreateRequest;
import com.visafe.domain.contract.dto.request.TerminationRespondRequest;
import com.visafe.domain.contract.dto.response.TerminationResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Termination", description = "계약 합의 종료 요청·응답·취소")
public interface ContractTerminationControllerDocument {

    @Operation(
            summary = "종료 요청",
            description = "확정된 계약을 기간보다 일찍 끝내자고 상대방에게 요청한다. 종료일은 마지막 근무일이며 오늘부터 계약 종료일까지 고를 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "종료 요청 생성 성공 (Location 헤더에 종료 요청 주소)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TerminationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(요청 값이 잘못됨) · TERMINATION_DATE_INVALID(종료일이 오늘 이전이거나 계약 종료일 이후)",
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
                responseCode = "403",
                description = "AUTH_FORBIDDEN(EMPLOYER·WORKER 권한 없음) · TERMINATION_NOT_ALLOWED(계약 당사자가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약이 없거나 볼 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "CONTRACT_STAGE_CONFLICT(확정된 계약이 아니거나 대체된 계약) · TERMINATION_ALREADY_ACTIVE(진행 중인 종료 요청이 있음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TerminationResponse> request(
            @PathVariable Long contractId,
            @Valid @RequestBody TerminationCreateRequest request,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "종료 동의·거절", description = "요청한 사람의 상대 당사자가 동의(AGREE)하거나 거절(DECLINE)한다. 동의하면 종료일 다음 날 계약이 끝난다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "응답 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TerminationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(요청 값이 잘못됨)",
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
                responseCode = "403",
                description = "AUTH_FORBIDDEN(EMPLOYER·WORKER 권한 없음) · TERMINATION_NOT_ALLOWED(요청한 본인이거나 계약 당사자가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약 또는 종료 요청이 없거나 볼 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "TERMINATION_STATUS_CONFLICT(이미 처리된 종료 요청)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TerminationResponse> respond(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @Valid @RequestBody TerminationRespondRequest request,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "종료 요청 취소", description = "요청한 사람이 응답을 기다리는 종료 요청을 취소한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "취소 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TerminationResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description =
                        "AUTH_UNAUTHORIZED(토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "AUTH_FORBIDDEN(EMPLOYER·WORKER 권한 없음) · TERMINATION_NOT_ALLOWED(요청한 사람이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약 또는 종료 요청이 없거나 볼 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "TERMINATION_STATUS_CONFLICT(이미 처리된 종료 요청)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TerminationResponse> cancel(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @AuthenticationPrincipal MemberDetails member);
}
