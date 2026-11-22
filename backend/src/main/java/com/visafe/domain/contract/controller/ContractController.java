package com.visafe.domain.contract.controller;

import com.visafe.domain.contract.dto.request.ContractCreateRequest;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.dto.response.FileDownloadUrlResponse;
import com.visafe.domain.contract.service.ContractFileService;
import com.visafe.domain.contract.service.ContractRegistrationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Contract", description = "계약 등록·원본 파일")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractRegistrationService contractRegistrationService;
    private final ContractFileService contractFileService;

    @Operation(
            summary = "계약 생성·원본 업로드",
            description = "multipart: file(PDF·JPG·PNG, 10MB 이하), data(application/json). 응답 후 AI 추출이 비동기로 진행된다.")
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

    @Operation(summary = "원본 다시 올리기", description = "stage UPLOADED·REVIEWING에서만. 항목을 비우고 다시 추출한다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PutMapping(value = "/{contractId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractStageResponse> replaceFile(
            @PathVariable Long contractId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.replaceFile(contractId, file, member));
    }

    @Operation(summary = "AI 추출 다시 시도", description = "stage UPLOADED이고 마지막 추출이 실패했을 때만.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @PostMapping("/{contractId}/extraction/retry")
    public ResponseEntity<ContractStageResponse> retryExtraction(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractRegistrationService.retryExtraction(contractId, member));
    }

    @Operation(summary = "원본 파일 URL 발급", description = "S3 Presigned GET URL (5분). 미리보기는 inline=true.")
    @GetMapping("/{contractId}/files/{fileId}/download-url")
    public FileDownloadUrlResponse getDownloadUrl(
            @PathVariable Long contractId,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal MemberDetails member) {
        return contractFileService.getDownloadUrl(contractId, fileId, inline, member);
    }
}
