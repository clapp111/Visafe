package com.visafe.domain.contract.dto.request;

import com.visafe.domain.contract.enums.TerminationDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "종료 동의·거절 요청 DTO")
public record TerminationRespondRequest(
        @Schema(description = "AGREE | DECLINE", example = "AGREE") @NotNull(message = "동의 여부를 보내 주세요.")
        TerminationDecision decision) {}
