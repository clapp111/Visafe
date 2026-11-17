package com.visafe.domain.contract.enums;

/**
 * contract_versions.status
 */
public enum ContractVersionStatus {
    /** 고용주 검토 중 (제출 정보 없음) */
    DRAFT,
    /** 제출됨 (번역·근로자 확인 진행) */
    SUBMITTED,
    /** 근로자 신고로 돌아옴 */
    RETURNED,
    /** 확정 (블록체인 기록 대상) */
    CONFIRMED,
    /** 다음 회차로 대체됨 */
    SUPERSEDED
}
