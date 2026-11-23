package com.visafe.domain.review.answerer;

import java.util.List;

/**
 * 질문에 대한 답.
 *
 * @param answer 근로자 언어로 쓴 답
 * @param referredToCenter 법률 판단이 필요해 상담센터(1350)를 안내하는지
 * @param citations 답의 근거가 된 조항
 */
public record QuestionAnswer(String answer, boolean referredToCenter, List<Citation> citations) {

    public QuestionAnswer {
        citations = List.copyOf(citations);
    }

    /**
     * 근거 조항.
     *
     * @param clauseIndex {@link QuestionSource#clauses()} 안 위치 (0부터)
     * @param quotedText 인용한 원문 문장, 없으면 조항 원문 전체를 쓴다
     */
    public record Citation(int clauseIndex, String quotedText) {}
}
