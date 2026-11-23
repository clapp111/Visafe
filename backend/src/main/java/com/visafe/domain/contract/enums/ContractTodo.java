package com.visafe.domain.contract.enums;

/**
 * 내 계약 목록에서 로그인한 사람이 해야 할 일. 할 일이 있는 계약을 먼저 보여 준다.
 */
public enum ContractTodo {
    /** 고용주: 근로자가 신고해 돌아온 계약을 고쳐야 함 (RETURNED) */
    FIX_REQUIRED,
    /** 고용주: 항목 검토를 마치고 제출해야 함 (REVIEWING) */
    REVIEW_INCOMPLETE,
    /** 고용주: AI 추출이 실패해 다시 시도해야 함 */
    EXTRACTION_FAILED,
    /** 고용주: 번역이 3회 모두 실패해 다시 시도해야 함 */
    TRANSLATION_FAILED,
    /** 근로자: 계약 내용을 확인하고 응답해야 함 (AWAITING_WORKER) */
    CONFIRM_REQUIRED,
    /** 양쪽: 상대방이 요청한 합의 종료에 동의·거절해야 함 */
    TERMINATION_RESPONSE
}
