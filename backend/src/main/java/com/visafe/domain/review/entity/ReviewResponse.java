package com.visafe.domain.review.entity;

import com.visafe.domain.member.entity.Member;
import com.visafe.domain.review.enums.ReviewAction;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * E. 근로자 응답 (review_responses) — 추가 전용. DB 트리거가 수정·삭제를 막는다.
 * 근로자가 확인한 번역본(translation)을 가리키며, 회차는 번역본에서 정해진다.
 * 응답자가 해당 계약의 근로자인지는 DB 트리거(trg_review_responses__worker)도 검사한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "review_responses")
public class ReviewResponse extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 보고 확인한 번역본 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    /** 응답한 근로자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_member_id", nullable = false)
    private Member worker;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 12)
    private ReviewAction action;

    /** 모두 읽음 체크 */
    @Column(name = "read_all_checked", nullable = false)
    private boolean readAllChecked;

    /** 신고 사유 */
    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    private ReviewResponse(
            Translation translation, Member worker, ReviewAction action, boolean readAllChecked, String reason) {
        this.translation = translation;
        this.worker = worker;
        this.action = action;
        this.readAllChecked = readAllChecked;
        this.reason = reason;
    }

    /** "이해했습니다" — 모두 읽음 체크가 있어야 한다. */
    public static ReviewResponse understood(Translation translation, Member worker) {
        return new ReviewResponse(translation, worker, ReviewAction.UNDERSTOOD, true, null);
    }

    /** "원본과 다릅니다" 신고. 신고 항목은 {@link ReviewReportItem}으로 따로 저장한다. */
    public static ReviewResponse reported(
            Translation translation, Member worker, boolean readAllChecked, String reason) {
        return new ReviewResponse(translation, worker, ReviewAction.REPORTED, readAllChecked, reason);
    }

    public boolean isUnderstood() {
        return action == ReviewAction.UNDERSTOOD;
    }
}
