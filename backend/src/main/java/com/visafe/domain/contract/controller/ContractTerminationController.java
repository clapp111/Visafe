package com.visafe.domain.contract.controller;

import com.visafe.domain.contract.dto.request.TerminationCreateRequest;
import com.visafe.domain.contract.dto.request.TerminationRespondRequest;
import com.visafe.domain.contract.dto.response.TerminationResponse;
import com.visafe.domain.contract.service.ContractTerminationService;
import com.visafe.domain.member.entity.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Termination", description = "계약 합의 종료 요청·응답·취소")
@RestController
@RequestMapping("/api/v1/contracts/{contractId}/terminations")
@RequiredArgsConstructor
public class ContractTerminationController {

    private final ContractTerminationService contractTerminationService;

    @Operation(
            summary = "종료 요청",
            description = "확정된 계약을 기간보다 일찍 끝내자고 상대방에게 요청한다. 종료일은 마지막 근무일이며 오늘부터 계약 종료일까지 고를 수 있다.")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @PostMapping
    public ResponseEntity<TerminationResponse> request(
            @PathVariable Long contractId,
            @Valid @RequestBody TerminationCreateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        TerminationResponse response = contractTerminationService.request(contractId, request, member);
        return ResponseEntity.created(
                        URI.create("/api/v1/contracts/" + contractId + "/terminations/" + response.terminationId()))
                .body(response);
    }

    @Operation(summary = "종료 동의·거절", description = "요청한 사람의 상대 당사자가 동의(AGREE)하거나 거절(DECLINE)한다. 동의하면 종료일 다음 날 계약이 끝난다.")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @PostMapping("/{terminationId}/response")
    public TerminationResponse respond(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @Valid @RequestBody TerminationRespondRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return contractTerminationService.respond(contractId, terminationId, request, member);
    }

    @Operation(summary = "종료 요청 취소", description = "요청한 사람이 응답을 기다리는 종료 요청을 취소한다.")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @PostMapping("/{terminationId}/cancel")
    public TerminationResponse cancel(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @AuthenticationPrincipal MemberDetails member) {
        return contractTerminationService.cancel(contractId, terminationId, member);
    }
}
