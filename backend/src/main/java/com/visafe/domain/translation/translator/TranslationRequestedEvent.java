package com.visafe.domain.translation.translator;

/**
 * 번역 요청. 제출(R6)이나 번역 다시 시도(R7) 트랜잭션이 커밋된 뒤 {@link ContractTranslationWorker}가 받는다.
 *
 * @param contractId 계약 ID
 * @param translationId 진행할 번역본 ID
 */
public record TranslationRequestedEvent(Long contractId, Long translationId) {}
