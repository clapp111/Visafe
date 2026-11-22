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
}
