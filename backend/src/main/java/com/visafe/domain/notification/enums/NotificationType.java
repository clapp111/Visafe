package com.visafe.domain.notification.enums;

/**
 * notifications.type — 화면 오른쪽 위 알림 종류. DB는 대문자 스네이크 형식만 검사하므로 필요한 종류는 여기에 추가한다.
 */
public enum NotificationType {
    /** 근로자: 확인할 계약이 도착함 */
    REVIEW_REQUESTED,
    /** 고용주: 근로자가 신고해 계약이 돌아옴 (수정 필요) */
    CONTRACT_RETURNED,
    /** 고용주: 번역에 최종 실패함 */
    TRANSLATION_FAILED,
    /** 양쪽: 계약이 확정됨 (확인번호 발급) */
    CONTRACT_CONFIRMED,
    /** 상대방: 종료 요청이 도착함 */
    TERMINATION_REQUESTED,
    /** 요청자: 종료 요청에 응답함 */
    TERMINATION_RESPONDED,
    /** 양쪽: 계약이 종료됨 */
    CONTRACT_TERMINATED,
    /** 고용주: 기관 검토 결과 */
    AUTHORITY_REVIEWED
}
