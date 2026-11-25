package com.visafe.domain.chain.enums;

/**
 * chain_records.status
 */
public enum ChainRecordStatus {
    /** 전송 대기 (재시도 대기 포함) */
    PENDING,
    /** 트랜잭션 전송됨 (tx_hash, sent_at 있음) */
    SENT,
    /** 블록에 포함됨 (block_number, mined_at 있음) */
    MINED,
    /** 실패 (온체인에서 거절되어 다시 보내지 않음) */
    FAILED
}
