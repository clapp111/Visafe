package com.visafe.domain.authority.controller;

import com.visafe.domain.authority.dto.request.AdminContractView;
import com.visafe.domain.authority.dto.request.AuthorityRejectRequest;
import com.visafe.domain.authority.dto.response.AdminContractDetailResponse;
import com.visafe.domain.authority.dto.response.AdminContractListResponse;
import com.visafe.domain.authority.service.AuthorityQueryService;
import com.visafe.domain.authority.service.AuthorityReviewService;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contracts")
@RequiredArgsConstructor
public class AuthorityController implements AuthorityControllerDocument {

    private final AuthorityQueryService authorityQueryService;
    private final AuthorityReviewService authorityReviewService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<AdminContractListResponse> list(
            @RequestParam(defaultValue = "PENDING") AdminContractView view,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(authorityQueryService.list(view, q, page, size));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{contractId}")
    public ResponseEntity<AdminContractDetailResponse> detail(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(authorityQueryService.detail(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<ContractStageResponse> approve(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(authorityReviewService.approve(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/reject")
    public ResponseEntity<ContractStageResponse> reject(
            @PathVariable Long contractId,
            @RequestBody AuthorityRejectRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(authorityReviewService.reject(contractId, request, member));
    }
}
