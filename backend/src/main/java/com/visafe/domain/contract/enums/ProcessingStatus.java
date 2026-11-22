package com.visafe.domain.contract.enums;

/** 진행 중인 작업 상태 (응답의 processing.status). 끝난 작업은 processing이 null이다. */
public enum ProcessingStatus {
    /** 진행 중이거나 자동으로 다시 시도할 예정 */
    RUNNING,
    /** 실패해 사람이 다시 시도해야 함 (추출 R3, 번역 R7) 또는 블록체인 기록이 실패해 서버가 다시 시도 중 */
    FAILED
}
