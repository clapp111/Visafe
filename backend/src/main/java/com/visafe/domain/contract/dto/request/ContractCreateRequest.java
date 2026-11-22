package com.visafe.domain.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "계약 생성 요청 DTO")
public record ContractCreateRequest(
        @Schema(description = "근로자 회원 ID (근로자 검색 결과의 memberId)", example = "12") @NotNull(message = "근로자를 선택해 주세요.")
        Long workerMemberId,

        @Schema(description = "대체할 기존 확정 계약 ID (조건 변경 시에만)", example = "null", nullable = true)
        Long previousContractId) {}
