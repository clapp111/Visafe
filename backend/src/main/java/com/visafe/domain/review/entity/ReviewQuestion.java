package com.visafe.domain.review.entity;

import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.member.entity.Member;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * E. 질문·답변 (review_questions). 답변과 답변 일시는 함께 채워진다. (ck_review_questions__answered_fields)
 * 답변 근거 조항은 {@link ReviewQuestionCitation}으로 저장한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "review_questions")
public class ReviewQuestion extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    /** 질문한 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asked_by_member_id", nullable = false)
    private Member askedBy;

    /** 언어 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "language", nullable = false, length = 2)
    private String language;

    /** 질문 */
    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    /** 답변 */
    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    /** 상담센터(1350) 안내 여부 */
    @Column(name = "referred_to_center", nullable = false)
    private boolean referredToCenter;

    /** 답변 일시 */
    @Column(name = "answered_at")
    private Instant answeredAt;

    private ReviewQuestion(ContractVersion contractVersion, Member askedBy, String language, String question) {
        this.contractVersion = contractVersion;
        this.askedBy = askedBy;
        this.language = language;
        this.question = question;
    }

    public static ReviewQuestion ask(
            ContractVersion contractVersion, Member askedBy, String language, String question) {
        return new ReviewQuestion(contractVersion, askedBy, language, question);
    }

    public void answer(String answer, boolean referredToCenter, Instant answeredAt) {
        if (isAnswered()) {
            throw new IllegalStateException("이미 답변한 질문입니다.");
        }
        this.answer = Objects.requireNonNull(answer, "answer");
        this.answeredAt = Objects.requireNonNull(answeredAt, "answeredAt");
        this.referredToCenter = referredToCenter;
    }

    public boolean isAnswered() {
        return answer != null;
    }
}
