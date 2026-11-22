package com.visafe.domain.lookup.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 대조 요청 DTO")
public record FileCheckRequest(
        @Schema(
                description = "브라우저가 계산한 파일 바이트 전체의 keccak256 (0x + 소문자 16진수 64자리)",
                example = "0x51d0e9b4c2a1f87d3e6b90a4c5d2e1f0b3a7c8d9e0f1a2b3c4d5e6f7a72f0e3b")
        String fileHash) {}
