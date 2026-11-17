package com.visafe.domain.contract.enums;

/**
 * contract_terminations.reason_type
 */
public enum TerminationReasonType {
    /** 기간 만료 (자동 종료) */
    EXPIRED,
    /** 합의 종료 (요청 → 상대방 동의) */
    MUTUAL,
    /** 새 계약으로 대체 (새 계약 확정과 같은 트랜잭션에서 자동 종료) */
    REPLACED
}
