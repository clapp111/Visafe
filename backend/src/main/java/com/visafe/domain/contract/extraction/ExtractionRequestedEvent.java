package com.visafe.domain.contract.extraction;

/**
 * AI 추출 요청. 원본을 올린 트랜잭션이 커밋된 뒤 {@link ContractExtractionWorker}가 받는다.
 *
 * @param contractId 계약 ID
 * @param versionId 추출 결과를 저장할 회차 ID
 * @param fileId 읽을 원본 파일 ID
 */
public record ExtractionRequestedEvent(Long contractId, Long versionId, Long fileId) {}
