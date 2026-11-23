package com.visafe.domain.authority.dto.response;

import com.visafe.domain.contract.dto.response.ContractListResponse.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 감독 기관 확인 대기 목록 (D1 응답). */
@Schema(description = "기관 확인 대기 목록 응답 DTO")
public record AdminContractListResponse(
        @Schema(description = "계약 목록") List<Item> content,
        @Schema(description = "페이지 정보") PageInfo page) {

    @Schema(description = "계약")
    public record Item(
            @Schema(description = "계약 ID", example = "31") Long contractId,

            @Schema(description = "내부 계약 번호", example = "C-2026-04817")
            String contractNo,

            @Schema(description = "사업체명", example = "한빛정밀") String companyName,

            @Schema(description = "근로자 이름", example = "Nguyễn Văn An")
            String workerName,

            @Schema(description = "번역 언어", example = "vi") String language,

            @Schema(description = "근로자 확인 일시 (검토한 회차의 '이해했습니다')", example = "2026-11-13T05:05:00Z", nullable = true)
            Instant workerConfirmedAt,

            @Schema(description = "'확인 필요' 건수 (최저임금 미달 + AI 확인 필요)", example = "1")
            int warningCount,

            @Schema(description = "'고용주 입력' 건수 (값 출처가 AI가 아닌 항목)", example = "2")
            int employerInputCount) {}
}
