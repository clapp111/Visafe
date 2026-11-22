package com.visafe.domain.contract.enums;

/**
 * 계약 상세에서 로그인한 사람이 지금 할 수 있는 일. 화면은 이 값에 따라 버튼을 보여 준다.
 * (종료 요청·응답·취소는 계약 종료 기능(#16)에서 추가)
 */
public enum ContractAction {
    /** 고용주: 항목 검토·수정 (R4, R5) */
    EDIT_DRAFT,
    /** 고용주: 돌아온 계약 수정 시작 (R8) */
    START_REVISION,
    /** 고용주: AI 추출 다시 시도 (R3) */
    RETRY_EXTRACTION,
    /** 고용주: 번역 다시 시도 (R7) */
    RETRY_TRANSLATION,
    /** 근로자: 계약 내용 확인·응답 (W1, W2) */
    RESPOND_REVIEW,
    /** 당사자·관리자: 원본 파일 보기·내려받기 (M3) */
    DOWNLOAD_FILE
}
