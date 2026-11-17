package com.visafe.domain.term.enums;

/**
 * contract_term_annotations.warning_code — 항목 경고. 화면 문구는 서버에서 코드로 만든다.
 * DB에 CHECK가 없으므로 필요한 경고는 여기에 추가한다. (VARCHAR(30) 이내)
 */
public enum WarningCode {
    /** 최저임금 미달 */
    MIN_WAGE,
    /** AI가 읽었지만 확인이 필요함 (흐림·모호한 표현 등) */
    NEEDS_CHECK
}
