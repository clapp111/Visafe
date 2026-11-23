package com.visafe.domain.authority.dto.response;

import com.visafe.domain.contract.dto.response.ContractDetailResponse;
import com.visafe.domain.contract.dto.response.ContractDraftResponse;
import com.visafe.domain.contract.enums.ContractStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 감독 기관 계약 검토 화면 (D2 응답). 검토 화면(R4)의 항목에 계약 상세(M2)의 당사자·원본·이력과 수정 이력을 더한다. */
@Schema(description = "기관 계약 검토 응답 DTO")
public record AdminContractDetailResponse(
        @Schema(description = "계약 ID", example = "31") Long contractId,

        @Schema(description = "내부 계약 번호", example = "C-2026-04817")
        String contractNo,

        @Schema(description = "stage", example = "AUTHORITY_REVIEW")
        ContractStage stage,

        @Schema(description = "현재 회차", example = "1") Integer versionNo,
        @Schema(description = "고용주") ContractDetailResponse.Employer employer,
        @Schema(description = "근로자") ContractDetailResponse.Worker worker,
        @Schema(description = "원본 파일") ContractDetailResponse.File file,

        @Schema(description = "19개 항목 (검토 화면과 같은 형식)") List<ContractDraftResponse.Field> fields,

        @Schema(description = "현재 회차의 항목 수정 이력 (수정한 순서)") List<EditLog> editLogs,

        @Schema(description = "진행 이력 (계약 상세와 같은 형식)") List<ContractDetailResponse.TimelineEntry> timeline) {

    @Schema(description = "항목 수정 이력")
    public record EditLog(
            @Schema(description = "항목 키", example = "breakMinutes")
            String key,

            @Schema(description = "수정 전 값", nullable = true) String before,

            @Schema(description = "수정 후 값", example = "60", nullable = true)
            String after,

            @Schema(description = "수정 일시", example = "2026-11-13T04:20:00Z")
            Instant editedAt) {}
}
