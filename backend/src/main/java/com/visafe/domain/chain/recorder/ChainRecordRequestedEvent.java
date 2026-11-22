package com.visafe.domain.chain.recorder;

/**
 * 블록체인 기록 요청. 기록을 시작한 트랜잭션이 커밋된 뒤 {@link ChainRecordWorker}가 받는다.
 *
 * @param contractId 계약 ID
 * @param chainRecordId 보낼 기록 ID
 */
public record ChainRecordRequestedEvent(Long contractId, Long chainRecordId) {}
