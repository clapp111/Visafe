package com.visafe.domain.review.entity;

import com.visafe.domain.review.enums.ReportType;
import com.visafe.domain.review.enums.ReviewAction;
import com.visafe.domain.term.entity.TermFieldConverter;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * E. 신고 항목 (review_report_items) — 추가 전용.
 * 원본과 다른 항목(ITEM_MISMATCH)만 신고하며 field_name이 필수다. (번역 신고는 MVP에서 제외)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "review_report_items")
public class ReviewReportItem extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_response_id", nullable = false)
    private ReviewResponse reviewResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    /** 신고한 항목 */
    @Convert(converter = TermFieldConverter.class)
    @Column(name = "field_name", nullable = false, length = 50)
    private TermField fieldName;

    private ReviewReportItem(ReviewResponse reviewResponse, ReportType reportType, TermField fieldName) {
        if (reviewResponse.getAction() != ReviewAction.REPORTED) {
            throw new IllegalArgumentException("신고 응답에만 신고 항목을 붙일 수 있습니다.");
        }
        this.reviewResponse = reviewResponse;
        this.reportType = reportType;
        this.fieldName = fieldName;
    }

    public static ReviewReportItem itemMismatch(ReviewResponse reviewResponse, TermField fieldName) {
        return new ReviewReportItem(
                reviewResponse, ReportType.ITEM_MISMATCH, Objects.requireNonNull(fieldName, "fieldName"));
    }
}
