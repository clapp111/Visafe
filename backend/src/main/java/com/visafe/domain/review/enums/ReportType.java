package com.visafe.domain.review.enums;

/**
 * review_report_items.report_type
 * <p>
 * MVP에서는 번역 신고를 받지 않아 항목 신고 하나뿐이다. (V3에서 DB CHECK도 ITEM_MISMATCH만 허용)
 */
public enum ReportType {
    /** 항목이 원본과 다름 (field_name 필수) */
    ITEM_MISMATCH
}
