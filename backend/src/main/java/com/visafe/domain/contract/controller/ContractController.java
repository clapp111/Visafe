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

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController implements ContractControllerDocument {

    private final ContractRegistrationService contractRegistrationService;
    private final ContractFileService contractFileService;
    private final ContractDraftService contractDraftService;
    private final ContractSubmissionService contractSubmissionService;
    private final ContractRevisionService contractRevisionService;
    private final ContractQueryService contractQueryService;

    @Override
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

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PutMapping(value = "/{contractId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractStageResponse> replaceFile(
            @PathVariable Long contractId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.replaceFile(contractId, file, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/extraction/retry")
    public ResponseEntity<ContractStageResponse> retryExtraction(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.retryExtraction(contractId, member));
    }

    @Override
    @GetMapping("/{contractId}/files/{fileId}/download-url")
    public ResponseEntity<FileDownloadUrlResponse> getDownloadUrl(
            @PathVariable Long contractId,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractFileService.getDownloadUrl(contractId, fileId, inline, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping("/{contractId}/draft")
    public ResponseEntity<ContractDraftResponse> getDraft(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractDraftService.getDraft(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PatchMapping("/{contractId}/draft/terms")
    public ResponseEntity<ContractDraftResponse> updateTerms(
            @PathVariable Long contractId,
            @Valid @RequestBody TermsUpdateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractDraftService.updateTerms(contractId, request, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/submit")
    public ResponseEntity<ContractStageResponse> submit(
            @PathVariable Long contractId,
            @RequestBody ContractSubmitRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractSubmissionService.submit(contractId, request, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/translations/retry")
    public ResponseEntity<ContractStageResponse> retryTranslation(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractSubmissionService.retryTranslation(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/revisions")
    public ResponseEntity<ContractStageResponse> startRevision(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractRevisionService.startRevision(contractId, member));
    }

    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @GetMapping
    public ResponseEntity<ContractListResponse> list(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractQueryService.list(status, page, size, member));
    }

    @Override
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractDetailResponse> detail(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractQueryService.detail(contractId, member));
    }
}
