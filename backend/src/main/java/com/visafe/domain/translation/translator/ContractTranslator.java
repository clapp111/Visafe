package com.visafe.domain.translation.translator;

/**
 * 계약서 조항과 항목을 근로자의 언어로 번역하고 항목마다 쉬운 설명을 붙인다.
 * AI API 키가 있으면 Claude 구현을, 없으면 {@link StubContractTranslator}를 쓴다.
 */
public interface ContractTranslator {

    /**
     * 조항과 항목을 번역한다.
     *
     * @param source 번역할 언어, 원문 조항, 값이 있는 항목
     * @return 조항 번역(원문 조항과 같은 순서)과 항목 번역·쉬운 설명. 번역하지 못하면 예외를 던진다
     */
    TranslationResult translate(TranslationSource source);

    /**
     * 근로자가 모국어로 쓴 글을 한국어로 번역한다. (신고 사유를 고용주에게 보여 줄 때)
     *
     * @param text 번역할 글
     * @param language 글의 언어 (ISO 639-1, 예: vi)
     * @return 한국어 번역. 번역하지 못하면 예외를 던진다
     */
    String translateToKorean(String text, String language);
}
