package com.visafe.domain.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "항목 수정 요청 DTO")
public record TermsUpdateRequest(
        @Schema(
                description = "바꾼 항목만 보낸다. 키는 항목 키(camelCase), 값을 비우려면 null",
                example = "{\"breakMinutes\": 60, \"jobDescription\": \"금속 부품 가공 및 포장\"}")
        @NotNull(message = "바꿀 항목을 보내 주세요.")
        Map<String, Object> values) {}
