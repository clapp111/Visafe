package com.visafe.domain.authority.controller;

import com.visafe.domain.authority.dto.request.AdminContractView;
import com.visafe.domain.authority.dto.request.AuthorityRejectRequest;
import com.visafe.domain.authority.dto.response.AdminContractDetailResponse;
import com.visafe.domain.authority.dto.response.AdminContractListResponse;
import com.visafe.domain.authority.service.AuthorityQueryService;
import com.visafe.domain.authority.service.AuthorityReviewService;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Authority", description = "감독 기관 검토 (선택 단계, ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/contracts")
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityQueryService authorityQueryService;
    private final AuthorityReviewService authorityReviewService;

    @Operation(
            summary = "확인 대기 목록",
            description =
                    "PENDING(기본)은 기관 검토 중인 계약, APPROVED·REJECTED는 그 결정의 검토, TERMINATED는 승인한 뒤 종료된 계약이다. q는 계약 번호·사업체명·근로자 이름을 함께 찾는다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public AdminContractListResponse list(
            @RequestParam(defaultValue = "PENDING") AdminContractView view,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return authorityQueryService.list(view, q, page, size);
    }

    @Operation(
            summary = "계약 검토",
            description = "19개 항목(AI 값, 값 출처, 경고, 근거 조항), 당사자·원본, 항목 수정 이력, 진행 이력을 조회한다. 번역은 검토하지 않는다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{contractId}")
    public AdminContractDetailResponse detail(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return authorityQueryService.detail(contractId, member);
    }

    @Operation(summary = "확정", description = "기관 검토 중인 계약을 승인하고 블록체인 기록을 시작한다. 본문 없이 호출한다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<ContractStageResponse> approve(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(authorityReviewService.approve(contractId, member));
    }

    @Operation(summary = "반려", description = "기관 검토 중인 계약을 사유와 함께 고용주에게 돌려보낸다. 고용주는 돌아온 계약 수정으로 다시 고친다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{contractId}/reject")
    public ContractStageResponse reject(
            @PathVariable Long contractId,
            @RequestBody AuthorityRejectRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return authorityReviewService.reject(contractId, request, member);
    }
}
