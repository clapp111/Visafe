package com.visafe.domain.review.answerer;

import java.util.List;

/**
 * 답할 질문.
 *
 * @param language 질문·답변 언어 (ISO 639-1, 예: vi)
 * @param question 질문
 * @param clauses 현재 회차의 원문 조항 (읽는 순서대로)
 */
public record QuestionSource(String language, String question, List<Clause> clauses) {

    public QuestionSource {
        clauses = List.copyOf(clauses);
    }

    /**
     * 원문 조항.
     *
     * @param clauseNo 조항 번호 (제3조 등, 없으면 null)
     * @param textKo 원문
     */
    public record Clause(String clauseNo, String textKo) {}
}
