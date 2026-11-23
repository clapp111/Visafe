package com.visafe.domain.review.controller;

import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.review.dto.request.QuestionCreateRequest;
import com.visafe.domain.review.dto.request.ReviewResponseCreateRequest;
import com.visafe.domain.review.dto.response.ContractReviewResponse;
import com.visafe.domain.review.dto.response.QuestionResponse;
import com.visafe.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Review", description = "근로자 계약 확인·응답·질문")
public interface ReviewControllerDocument {

    @Operation(
            summary = "확인할 계약 내용 조회",
            description =
                    "현재 회차의 마지막 완료 번역본을 기준으로 항목 번역·쉬운 설명, 원문·번역 조항, 꼭 확인할 항목, 경고를 조회한다. stage가 AWAITING_WORKER가 아니면 canRespond가 false다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractReviewResponse.class))),
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
                description = "AUTH_FORBIDDEN(WORKER 권한 없음)",
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
                description = "TRANSLATION_NOT_READY(완료된 번역본 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractReviewResponse> getReview(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "이해함 · 원본과 다름 응답",
            description =
                    "UNDERSTOOD면 확정 기록을 시작하고(기관 검토가 필요하면 AUTHORITY_REVIEW), REPORTED면 계약을 고용주에게 돌려보낸다. translationId는 W1에서 받은 값을 보낸다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "응답 성공, UNDERSTOOD면 블록체인 기록은 비동기로 진행",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description =
                        "COMMON_INVALID_REQUEST(요청 값이 잘못됨) · READ_ALL_REQUIRED(모두 읽음 체크 안 함) · REPORT_ITEMS_REQUIRED(다른 항목을 고르지 않음) · TERM_KEY_UNKNOWN(알 수 없는 항목 키)",
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
                description = "AUTH_FORBIDDEN(WORKER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 AWAITING_WORKER가 아님) · TRANSLATION_OUTDATED(새 번역본이 나옴)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> respond(
            @PathVariable Long contractId,
            @Valid @RequestBody ReviewResponseCreateRequest request,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "질문 목록", description = "현재 회차에서 내가 한 질문을 오래된 순으로 답변·근거 조항과 함께 조회한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
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
                description = "AUTH_FORBIDDEN(WORKER 권한 없음)",
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
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<QuestionResponse>> listQuestions(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "질문하기",
            description = "이 계약서 조항만 근거로 AI가 바로 답한다(최대 20초). 실패하거나 시간이 초과되면 질문만 저장하고 answer가 null이다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "질문 저장 성공 (AI 답변 실패 시 answer는 null)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = QuestionResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(빈 질문이거나 500자 초과)",
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
                description = "AUTH_FORBIDDEN(WORKER 권한 없음)",
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
                description = "TRANSLATION_NOT_READY(완료된 번역본 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<QuestionResponse> ask(
            @PathVariable Long contractId,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal MemberDetails member);
}
