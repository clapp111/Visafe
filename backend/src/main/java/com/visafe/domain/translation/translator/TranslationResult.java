package com.visafe.domain.translation.translator;

import com.visafe.domain.term.enums.TermField;
import java.util.List;
import java.util.Map;

/**
 * 번역 결과.
 *
 * @param clauses 조항 번역문 ({@link TranslationSource#clauses()}와 같은 순서·개수)
 * @param items 항목별 번역 ({@link TranslationSource#terms()}의 항목마다 하나)
 */
public record TranslationResult(List<String> clauses, Map<TermField, Item> items) {

    public TranslationResult {
        clauses = List.copyOf(clauses);
        items = Map.copyOf(items);
    }

    /**
     * 항목 하나.
     *
     * @param translatedValue 번역된 값
     * @param easyExplanation 근로자 언어로 쓴 쉬운 설명
     * @param easyExplanationKo 고용주 검토용 한국어 설명
     */
    public record Item(String translatedValue, String easyExplanation, String easyExplanationKo) {}
}
