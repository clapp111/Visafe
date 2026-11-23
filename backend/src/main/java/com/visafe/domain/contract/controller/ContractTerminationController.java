package com.visafe.domain.contract.controller;

import com.visafe.domain.contract.dto.request.TerminationCreateRequest;
import com.visafe.domain.contract.dto.request.TerminationRespondRequest;
import com.visafe.domain.contract.dto.response.TerminationResponse;
import com.visafe.domain.contract.service.ContractTerminationService;
import com.visafe.domain.member.entity.MemberDetails;
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

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/terminations")
@RequiredArgsConstructor
public class ContractTerminationController implements ContractTerminationControllerDocument {

    private final ContractTerminationService contractTerminationService;

    @Override
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

    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @PostMapping("/{terminationId}/response")
    public ResponseEntity<TerminationResponse> respond(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @Valid @RequestBody TerminationRespondRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractTerminationService.respond(contractId, terminationId, request, member));
    }

    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'WORKER')")
    @PostMapping("/{terminationId}/cancel")
    public ResponseEntity<TerminationResponse> cancel(
            @PathVariable Long contractId,
            @PathVariable Long terminationId,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractTerminationService.cancel(contractId, terminationId, member));
    }
}
