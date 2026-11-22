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
import com.visafe.domain.contract.service.ContractDraftService;
import com.visafe.domain.contract.service.ContractFileService;
import com.visafe.domain.contract.service.ContractQueryService;
import com.visafe.domain.contract.service.ContractRegistrationService;
import com.visafe.domain.contract.service.ContractRevisionService;
import com.visafe.domain.contract.service.ContractSubmissionService;
import com.visafe.domain.member.entity.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Contract", description = "계약 등록·원본 파일·제출·내 계약")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractRegistrationService contractRegistrationService;
    private final ContractFileService contractFileService;
    private final ContractDraftService contractDraftService;
    private final ContractSubmissionService contractSubmissionService;
    private final ContractRevisionService contractRevisionService;
    private final ContractQueryService contractQueryService;

    @Operation(
            summary = "계약 생성·원본 업로드",
            description =
                    "계약을 만들고 원본(PDF·JPG·PNG, 10MB 이하)을 올린다. multipart의 file과 data(application/json) 파트로 보내며, 응답 후 AI 추출이 비동기로 진행된다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractStageResponse> create(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("data") ContractCreateRequest data,
            @AuthenticationPrincipal MemberDetails member) {
        ContractStageResponse response = contractRegistrationService.create(data, file, member);
        return ResponseEntity.created(URI.create("/api/v1/contracts/" + response.contractId()))
                .body(response);
    }

    @Operation(
            summary = "원본 다시 올리기",
            description = "원본을 다시 올리고 항목을 비운 뒤 다시 추출한다. stage가 UPLOADED·REVIEWING일 때만 할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PutMapping(value = "/{contractId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractStageResponse> replaceFile(
            @PathVariable Long contractId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.replaceFile(contractId, file, member));
    }

    @Operation(summary = "AI 추출 다시 시도", description = "실패한 AI 추출을 다시 시도한다. stage가 UPLOADED이고 마지막 추출이 실패했을 때만 할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/extraction/retry")
    public ResponseEntity<ContractStageResponse> retryExtraction(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.retryExtraction(contractId, member));
    }

    @Operation(
            summary = "원본 파일 URL 발급",
            description = "브라우저가 S3에서 원본을 직접 받는 Presigned GET URL(5분 유효)을 발급한다. 미리보기는 inline=true로 호출한다.")
    @GetMapping("/{contractId}/files/{fileId}/download-url")
    public FileDownloadUrlResponse getDownloadUrl(
            @PathVariable Long contractId,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal MemberDetails member) {
        return contractFileService.getDownloadUrl(contractId, fileId, inline, member);
    }

    @Operation(
            summary = "검토할 항목 조회",
            description = "2단계 검토 화면의 19개 항목을 AI가 읽은 값, 값 출처, 경고, 근거 조항과 함께 조회한다. 추출 중이면 항목이 비어 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping("/{contractId}/draft")
    public ContractDraftResponse getDraft(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return contractDraftService.getDraft(contractId, member);
    }

    @Operation(summary = "항목 수정", description = "바꾼 항목만 저장하고 경고를 다시 계산해 검토 화면 데이터를 돌려준다. stage가 REVIEWING일 때만 할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PatchMapping("/{contractId}/draft/terms")
    public ContractDraftResponse updateTerms(
            @PathVariable Long contractId,
            @Valid @RequestBody TermsUpdateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return contractDraftService.updateTerms(contractId, request, member);
    }

    @Operation(
            summary = "제출하고 확인 요청 보내기",
            description =
                    "원본 일치 확인 체크와 함께 제출한다. 필수 항목이 비어 있으면 거절하고, 경고는 제출을 막지 않는다. 응답 후 근로자 언어로 번역이 비동기로 진행된다. stage가 REVIEWING일 때만 할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/submit")
    public ResponseEntity<ContractStageResponse> submit(
            @PathVariable Long contractId,
            @RequestBody ContractSubmitRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractSubmissionService.submit(contractId, request, member));
    }

    @Operation(
            summary = "번역 다시 시도",
            description = "3회 모두 실패한 번역을 새 번역본으로 다시 시도한다. stage가 TRANSLATING이고 마지막 번역이 최종 실패했을 때만 할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/translations/retry")
    public ResponseEntity<ContractStageResponse> retryTranslation(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractSubmissionService.retryTranslation(contractId, member));
    }

    @Operation(
            summary = "돌아온 계약 수정 시작",
            description = "stage가 RETURNED면 이전 회차를 복사한 새 회차를 만들고 REVIEWING으로 바꾼다. 이미 REVIEWING이면 현재 회차를 그대로 돌려준다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/revisions")
    public ContractStageResponse startRevision(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return contractRevisionService.startRevision(contractId, member);
    }

    @Operation(
            summary = "내 계약 목록",
            description = "고용주는 같은 사업주의 모든 계약, 근로자는 본인 계약을 본다. 할 일이 있는 계약이 먼저이고 그다음 최근 수정 순이다. status를 빼면 전체를 본다.")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @GetMapping
    public ContractListResponse list(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member) {
        return contractQueryService.list(status, page, size, member);
    }

    @Operation(
            summary = "계약 상세",
            description = "계약 정보, 항목과 설명, 진행 이력, 블록체인 기록, 지금 할 수 있는 일을 조회한다. 진행 상황 확인(2~3초 간격)에도 쓴다.")
    @GetMapping("/{contractId}")
    public ContractDetailResponse detail(@PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return contractQueryService.detail(contractId, member);
    }
}
