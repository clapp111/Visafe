package com.visafe.domain.authority.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기관 반려 요청 DTO")
public record AuthorityRejectRequest(
        @Schema(description = "반려 사유 (필수)", example = "월 기본급이 최저임금에 미달합니다. 원본과 함께 다시 확인해 주세요.")
        String reason) {}
