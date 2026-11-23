package com.visafe.domain.review.answerer;

/**
 * 근로자의 질문에 이 계약서 조항만 근거로 답한다. AI API 키가 있으면 Claude 구현을, 없으면 {@link StubContractQuestionAnswerer}를 쓴다.
 */
public interface ContractQuestionAnswerer {

    /**
     * 질문에 답한다.
     *
     * @param source 질문 언어, 질문, 원문 조항
     * @return 근로자 언어로 쓴 답, 상담센터 안내 여부, 근거 조항. 답하지 못하면 예외를 던진다
     */
    QuestionAnswer answer(QuestionSource source);
}
