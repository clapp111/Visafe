package com.visafe.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 응답 {@code {content, page}}. 목록 외에 다른 값(건수 요약 등)이 더 필요하면 각 DTO가 {@link PageInfo}를 직접 담는다.
 *
 * @param content 현재 페이지의 항목
 * @param page 페이지 정보
 */
@Schema(description = "목록 응답")
public record PageResponse<T>(List<T> content, PageInfo page) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, PageInfo.from(page));
    }
}
