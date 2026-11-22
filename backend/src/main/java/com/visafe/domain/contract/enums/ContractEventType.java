package com.visafe.domain.contract.enums;

/**
 * contract_events.event_type — 진행 상황·확인 이력·기관 검토 이력 화면이 함께 쓰는 이벤트 종류.
 * <p>
 * DB는 대문자 스네이크 형식({@code ^[A-Z_]+$})만 검사하므로 필요한 이벤트는 여기에 추가한다.
 * 이름은 events_hash 계산에 들어가므로 한 번 기록된 이름은 바꾸지 않는다.
 */
public enum ContractEventType {
    // 등록·검토
    CONTRACT_CREATED,
    FILE_UPLOADED,
    /** AI 추출 시작 (처음, 원본 다시 올리기, 다시 시도 모두). 진행 상태와 시도 횟수 계산에 쓴다. */
    EXTRACTION_STARTED,
    EXTRACTION_COMPLETED,
    EXTRACTION_FAILED,
    TERM_EDITED,
    SUBMITTED,

    // 번역
    TRANSLATION_COMPLETED,
    TRANSLATION_FAILED,

    // 근로자 확인
    WORKER_UNDERSTOOD,
    WORKER_REPORTED,

    // 기관 검토 (선택 단계)
    AUTHORITY_REVIEW_REQUESTED,
    AUTHORITY_APPROVED,
    AUTHORITY_REJECTED,

    // 블록체인 기록·확정
    CHAIN_RECORD_REQUESTED,
    CHAIN_RECORD_FAILED,
    CONFIRMED,

    // 종료
    TERMINATION_REQUESTED,
    TERMINATION_AGREED,
    TERMINATION_DECLINED,
    TERMINATION_CANCELED,
    TERMINATED
}
