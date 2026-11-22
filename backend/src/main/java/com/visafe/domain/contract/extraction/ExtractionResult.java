package com.visafe.domain.contract.extraction;

import com.visafe.domain.term.enums.TermField;
import java.util.List;
import java.util.Map;

/**
 * AI 추출 결과.
 *
 * @param clauses 원문 조항 (읽는 순서대로)
 * @param terms 항목별 결과. 없는 항목은 읽지 못한(UNREAD) 것으로 본다
 */
public record ExtractionResult(List<Clause> clauses, Map<TermField, Term> terms) {

    public ExtractionResult {
        clauses = List.copyOf(clauses);
        terms = Map.copyOf(terms);
    }

    /**
     * 원문 조항.
     *
     * @param clauseNo 조항 번호 (제3조 등, 없으면 null)
     * @param pageNo 쪽 (1부터, 모르면 null)
     * @param text 원문
     */
    public record Clause(String clauseNo, Integer pageNo, String text) {}

    /**
     * 항목 하나.
     *
     * @param value AI가 읽은 값 (날짜 2027-01-02, 시각 09:00, 금액·숫자는 숫자만). 읽지 못하면 null
     * @param needsCheck 읽었지만 흐리거나 모호해 확인이 필요한지
     * @param clauseIndex 근거 조항의 clauses 안 위치 (0부터, 없으면 null)
     */
    public record Term(String value, boolean needsCheck, Integer clauseIndex) {}
}
