package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 종료 요청·응답·취소 API(M4, M5, M6)의 응답. */
@Schema(description = "종료 응답 DTO")
public record TerminationResponse(
        @Schema(description = "종료 ID", example = "3") Long terminationId,

        @Schema(description = "EXPIRED | MUTUAL | REPLACED", example = "MUTUAL")
        TerminationReasonType reasonType,

        @Schema(description = "사유", example = "귀국 예정", nullable = true)
        String reasonText,

        @Schema(description = "REQUESTED | AGREED | DECLINED | CANCELED", example = "REQUESTED")
        TerminationStatus status,

        @Schema(description = "종료일", example = "2027-06-30") LocalDate endDate) {

    public static TerminationResponse of(ContractTermination termination) {
        return new TerminationResponse(
                termination.getId(),
                termination.getReasonType(),
                termination.getReasonText(),
                termination.getStatus(),
                termination.getEndDate());
    }
}
