package com.visafe.domain.review.dto.request;

import com.visafe.domain.review.enums.ReportType;
import com.visafe.domain.review.enums.ReviewAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "근로자 확인 응답 요청 DTO")
public record ReviewResponseCreateRequest(
        @Schema(description = "확인한 번역본 ID (W1 응답 값)", example = "57") @NotNull(message = "확인한 번역본을 보내 주세요.")
        Long translationId,

        @Schema(description = "UNDERSTOOD(이해했습니다) | REPORTED(원본과 다릅니다)", example = "UNDERSTOOD")
        @NotNull(message = "응답 종류를 보내 주세요.")
        ReviewAction action,

        @Schema(description = "\"모두 읽었고 이해했습니다\" 체크 (UNDERSTOOD일 때 true 필수)", example = "true", nullable = true)
        Boolean readAllChecked,

        @Schema(description = "원본과 다른 항목 (REPORTED일 때 1개 이상)", nullable = true)
        List<@Valid ReportItem> reportItems,

        @Schema(description = "신고 사유 (선택, 모국어 가능)", example = "Trong bản gốc ghi 30 phút.", nullable = true)
        String reason) {

    @Schema(description = "신고 항목")
    public record ReportItem(
            @Schema(description = "신고 종류", example = "ITEM_MISMATCH") @NotNull(message = "신고 종류를 보내 주세요.")
            ReportType type,

            @Schema(description = "항목 키", example = "breakMinutes") @NotNull(message = "신고할 항목 키를 보내 주세요.")
            String key) {}
}
