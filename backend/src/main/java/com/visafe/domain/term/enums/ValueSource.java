package com.visafe.domain.term.enums;

/**
 * contract_term_annotations.value_source — 항목 값의 출처. 근로자 확인 화면의 형광펜 표시에 쓴다.
 */
public enum ValueSource {
    /** AI가 읽은 값 그대로 */
    AI,
    /** AI가 읽은 값을 고용주가 고침 */
    EMPLOYER_EDIT,
    /** AI가 읽지 못해 고용주가 직접 입력 */
    EMPLOYER_INPUT
}
