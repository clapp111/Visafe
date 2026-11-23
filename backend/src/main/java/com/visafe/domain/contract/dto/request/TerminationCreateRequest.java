package com.visafe.domain.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "종료 요청 DTO")
public record TerminationCreateRequest(
        @Schema(description = "종료일 (마지막 근무일, 오늘 이후·계약 종료일 이전)", example = "2027-06-30")
        @NotNull(message = "종료일을 보내 주세요.")
        LocalDate endDate,

        @Schema(description = "종료 사유 (선택)", example = "귀국 예정", nullable = true)
        String reasonText) {}
