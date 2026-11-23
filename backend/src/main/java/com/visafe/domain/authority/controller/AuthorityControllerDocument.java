package com.visafe.domain.authority.controller;

import com.visafe.domain.authority.dto.request.AdminContractView;
import com.visafe.domain.authority.dto.request.AuthorityRejectRequest;
import com.visafe.domain.authority.dto.response.AdminContractDetailResponse;
import com.visafe.domain.authority.dto.response.AdminContractListResponse;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Authority", description = "감독 기관 검토 (선택 단계, ADMIN)")
public interface AuthorityControllerDocument {

    @Operation(
            summary = "확인 대기 목록",
            description =
                    "PENDING(기본)은 기관 검토 중인 계약, APPROVED·REJECTED는 그 결정의 검토, TERMINATED는 승인한 뒤 종료된 계약이다. q는 계약 번호·사업체명·근로자 이름을 함께 찾는다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AdminContractListResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(page·size 범위 밖) · COMMON_INVALID_REQUEST_TYPE(view 값이 잘못됨)",
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
                description = "AUTH_FORBIDDEN(ADMIN 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<AdminContractListResponse> list(
            @RequestParam(defaultValue = "PENDING") AdminContractView view,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(
            summary = "계약 검토",
            description = "19개 항목(AI 값, 값 출처, 경고, 근거 조항), 당사자·원본, 항목 수정 이력, 진행 이력을 조회한다. 번역은 검토하지 않는다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "계약 검토 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AdminContractDetailResponse.class))),
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
                description = "AUTH_FORBIDDEN(ADMIN 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<AdminContractDetailResponse> detail(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "확정", description = "기관 검토 중인 계약을 승인하고 블록체인 기록을 시작한다. 본문 없이 호출한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "승인 완료, 블록체인 기록은 비동기로 진행",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
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
                description = "AUTH_FORBIDDEN(ADMIN 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "CONTRACT_STAGE_CONFLICT(stage가 AUTHORITY_REVIEW가 아님) · AUTHORITY_ALREADY_REVIEWED(이미 검토한 회차)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> approve(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "반려", description = "기관 검토 중인 계약을 사유와 함께 고용주에게 돌려보낸다. 고용주는 돌아온 계약 수정으로 다시 고친다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "반려 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "REJECT_REASON_REQUIRED(반려 사유 없음) · COMMON_INVALID_REQUEST_TYPE(본문 형식이 잘못됨)",
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
                description = "AUTH_FORBIDDEN(ADMIN 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CONTRACT_NOT_FOUND(계약 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "CONTRACT_STAGE_CONFLICT(stage가 AUTHORITY_REVIEW가 아님) · AUTHORITY_ALREADY_REVIEWED(이미 검토한 회차)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> reject(
            @PathVariable Long contractId,
            @RequestBody AuthorityRejectRequest request,
            @AuthenticationPrincipal MemberDetails member);
}
