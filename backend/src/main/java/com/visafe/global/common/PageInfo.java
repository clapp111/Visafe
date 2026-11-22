package com.visafe.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * 목록 응답의 페이지 정보. 모든 목록 API가 {@code page: {number, size, totalElements, totalPages}}로 같이 쓴다.
 *
 * @param number 현재 페이지 (0부터)
 * @param size 페이지 크기
 * @param totalElements 전체 항목 수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "페이지 정보")
public record PageInfo(
        @Schema(description = "현재 페이지 (0부터)", example = "0") int number,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "5") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
