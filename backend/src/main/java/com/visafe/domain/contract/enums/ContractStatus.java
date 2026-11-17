package com.visafe.domain.contract.enums;

/**
 * contracts.status — 공개 상태. DB 생성 컬럼으로 stage에서 계산되며 직접 저장하지 않는다.
 */
public enum ContractStatus {
    IN_PROGRESS,
    CONFIRMED,
    TERMINATED
}
