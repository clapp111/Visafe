package com.visafe.domain.translation.translator;

import com.visafe.domain.term.enums.TermField;
import java.util.List;
import java.util.Map;

/**
 * 번역할 내용.
 *
 * @param language 번역 언어 (ISO 639-1, 예: vi)
 * @param clauses 원문 조항 (읽는 순서대로)
 * @param terms 값이 있는 항목의 값 (날짜 2027-01-02, 시각 09:00, 숫자 60). 빈 항목은 넣지 않는다
 */
public record TranslationSource(String language, List<Clause> clauses, Map<TermField, String> terms) {

    public TranslationSource {
        clauses = List.copyOf(clauses);
        terms = Map.copyOf(terms);
    }

    /**
     * 원문 조항.
     *
     * @param clauseNo 조항 번호 (제3조 등, 없으면 null)
     * @param textKo 원문
     */
    public record Clause(String clauseNo, String textKo) {}
}
