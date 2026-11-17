package com.visafe.domain.review.enums;

/**
 * review_report_items.report_type
 */
public enum ReportType {
    /** 항목이 원본과 다름 (field_name 필수) */
    ITEM_MISMATCH,
    /** 번역이 이상해요 (field_name 없음) */
    TRANSLATION
}
