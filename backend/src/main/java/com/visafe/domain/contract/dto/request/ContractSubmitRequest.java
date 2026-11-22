package com.visafe.domain.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계약 제출 요청 DTO")
public record ContractSubmitRequest(
        @Schema(description = "\"원본 계약서의 내용과 같음을 확인했습니다\" 체크 (true 필수)", example = "true")
        Boolean confirmedOriginalMatch) {}
