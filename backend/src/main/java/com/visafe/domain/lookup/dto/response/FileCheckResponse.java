package com.visafe.domain.lookup.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 파일 대조 결과 (L2 응답). */
@Schema(description = "파일 대조 응답 DTO")
public record FileCheckResponse(
        @Schema(description = "기록된 원본과 같은지", example = "false")
        boolean match,

        @Schema(description = "요청으로 받은 파일 해시 (화면의 '올린 파일 해시')")
        String uploadedFileHash,

        @Schema(description = "확정 기록의 파일 해시") String recordedFileHash) {}
