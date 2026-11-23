package com.visafe.domain.authority.dto.request;

/** 기관 확인 대기 목록의 보기. */
public enum AdminContractView {
    /** 기관 검토를 기다리는 계약 (stage AUTHORITY_REVIEW), 근로자 확인이 오래된 순 */
    PENDING,
    /** 승인한 검토, 최근 순 */
    APPROVED,
    /** 반려한 검토, 최근 순 */
    REJECTED,
    /** 승인한 뒤 종료된 계약, 최근 종료 순 */
    TERMINATED
}
