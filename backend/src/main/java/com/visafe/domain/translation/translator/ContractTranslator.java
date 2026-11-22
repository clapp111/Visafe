package com.visafe.domain.translation.translator;

/**
 * 계약서 조항과 항목을 근로자의 언어로 번역하고 항목마다 쉬운 설명을 붙인다.
 * 실제 LLM 구현 전까지는 {@link StubContractTranslator}를 쓴다.
 */
public interface ContractTranslator {

    /**
     * 조항과 항목을 번역한다.
     *
     * @param source 번역할 언어, 원문 조항, 값이 있는 항목
     * @return 조항 번역(원문 조항과 같은 순서)과 항목 번역·쉬운 설명. 번역하지 못하면 예외를 던진다
     */
    TranslationResult translate(TranslationSource source);
}
