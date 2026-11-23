package com.visafe.domain.contract.controller;

import com.visafe.domain.contract.dto.request.ContractCreateRequest;
import com.visafe.domain.contract.dto.request.ContractSubmitRequest;
import com.visafe.domain.contract.dto.request.TermsUpdateRequest;
import com.visafe.domain.contract.dto.response.ContractDetailResponse;
import com.visafe.domain.contract.dto.response.ContractDraftResponse;
import com.visafe.domain.contract.dto.response.ContractListResponse;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.dto.response.FileDownloadUrlResponse;
import com.visafe.domain.contract.enums.ContractStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Contract", description = "계약 등록·원본 파일·제출·내 계약")
public interface ContractControllerDocument {

    @Operation(
            summary = "계약 생성·원본 업로드",
            description =
                    "계약을 만들고 원본(PDF·JPG·PNG, 10MB 이하)을 올린다. multipart의 file과 data(application/json) 파트로 보내며, 응답 후 AI 추출이 비동기로 진행된다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "계약 생성 성공 (Location 헤더에 계약 주소), AI 추출은 비동기로 진행",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(data 값이 잘못됨) · COMMON_INVALID_PARAMETER(file·data 파트 없음)",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "WORKER_NOT_FOUND(근로자가 없거나 WORKER가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "PREVIOUS_CONTRACT_INVALID(대체할 계약이 확정 상태가 아니거나 사업주·근로자가 다름)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "413",
                description = "FILE_TOO_LARGE(파일이 10MB 초과)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "415",
                description = "FILE_UNSUPPORTED_TYPE(PDF·JPG·PNG가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> create(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("data") ContractCreateRequest data,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "원본 다시 올리기",
            description = "원본을 다시 올리고 항목을 비운 뒤 다시 추출한다. stage가 UPLOADED·REVIEWING일 때만 할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "원본 교체 성공, AI 추출은 비동기로 진행",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_PARAMETER(file 파트 없음)",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 UPLOADED·REVIEWING이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "413",
                description = "FILE_TOO_LARGE(파일이 10MB 초과)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "415",
                description = "FILE_UNSUPPORTED_TYPE(PDF·JPG·PNG가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> replaceFile(
            @PathVariable Long contractId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "AI 추출 다시 시도", description = "실패한 AI 추출을 다시 시도한다. stage가 UPLOADED이고 마지막 추출이 실패했을 때만 할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "재시도 시작, AI 추출은 비동기로 진행",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 UPLOADED가 아니거나 마지막 추출이 실패가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> retryExtraction(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "원본 파일 URL 발급",
            description = "브라우저가 S3에서 원본을 직접 받는 Presigned GET URL(5분 유효)을 발급한다. 미리보기는 inline=true로 호출한다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "URL 발급 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = FileDownloadUrlResponse.class))),
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
                description = "CONTRACT_NOT_FOUND(계약이 없거나 볼 권한 없음) · FILE_NOT_FOUND(파일이 없거나 다른 계약의 파일)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FileDownloadUrlResponse> getDownloadUrl(
            @PathVariable Long contractId,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "검토할 항목 조회",
            description = "2단계 검토 화면의 19개 항목을 AI가 읽은 값, 값 출처, 경고, 근거 조항과 함께 조회한다. 추출 중이면 항목이 비어 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "항목 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractDraftResponse.class))),
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 UPLOADED·REVIEWING·RETURNED가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractDraftResponse> getDraft(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(summary = "항목 수정", description = "바꾼 항목만 저장하고 경고를 다시 계산해 검토 화면 데이터를 돌려준다. stage가 REVIEWING일 때만 할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "항목 수정 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractDraftResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description =
                        "COMMON_INVALID_REQUEST(요청 값이 잘못됨) · TERM_KEY_UNKNOWN(알 수 없는 항목 키) · TERM_VALUE_INVALID(항목 값이 잘못됨, 이유는 message에 담음)",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 REVIEWING이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractDraftResponse> updateTerms(
            @PathVariable Long contractId,
            @Valid @RequestBody TermsUpdateRequest request,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "제출하고 확인 요청 보내기",
            description =
                    "원본 일치 확인 체크와 함께 제출한다. 필수 항목이 비어 있으면 거절하고, 경고는 제출을 막지 않는다. 응답 후 근로자 언어로 번역이 비동기로 진행된다. stage가 REVIEWING일 때만 할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "제출 성공, 번역은 비동기로 진행",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractStageResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "CONFIRMATION_REQUIRED(원본 일치 확인 체크 안 함) · COMMON_INVALID_REQUEST_TYPE(본문 형식이 잘못됨)",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 REVIEWING이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "TERMS_REQUIRED_MISSING(비어 있는 필수 항목, 항목 키는 message에 담음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> submit(
            @PathVariable Long contractId,
            @RequestBody ContractSubmitRequest request,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "번역 다시 시도",
            description = "3회 모두 실패한 번역을 새 번역본으로 다시 시도한다. stage가 TRANSLATING이고 마지막 번역이 최종 실패했을 때만 할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "재시도 시작, 번역은 비동기로 진행",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 TRANSLATING이 아니거나 마지막 번역이 실패가 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> retryTranslation(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "돌아온 계약 수정 시작",
            description = "stage가 RETURNED면 이전 회차를 복사한 새 회차를 만들고 REVIEWING으로 바꾼다. 이미 REVIEWING이면 현재 회차를 그대로 돌려준다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "수정 시작 성공",
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
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
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
                description = "CONTRACT_STAGE_CONFLICT(stage가 RETURNED·REVIEWING이 아님)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractStageResponse> startRevision(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "내 계약 목록",
            description = "고용주는 같은 사업주의 모든 계약, 근로자는 본인 계약을 본다. 할 일이 있는 계약이 먼저이고 그다음 최근 수정 순이다. status를 빼면 전체를 본다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractListResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_REQUEST(page·size 범위 밖) · COMMON_INVALID_REQUEST_TYPE(status 값이 잘못됨)",
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
                description = "AUTH_FORBIDDEN(EMPLOYER·WORKER 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractListResponse> list(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member);

    @Operation(
            summary = "계약 상세",
            description = "계약 정보, 항목과 설명, 진행 이력, 블록체인 기록, 지금 할 수 있는 일을 조회한다. 진행 상황 확인(2~3초 간격)에도 쓴다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "상세 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ContractDetailResponse.class))),
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
                description = "CONTRACT_NOT_FOUND(계약이 없거나 볼 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ContractDetailResponse> detail(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member);
}
