package com.visafe.domain.review.entity;

import com.visafe.domain.contract.entity.ContractClause;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * E. 답변 근거 조항 (review_question_citations). "제3조(2쪽)"처럼 원문 조항을 인용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "review_question_citations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_review_question_citations__question_clause",
                    columnNames = {"review_question_id", "contract_clause_id"})
        })
public class ReviewQuestionCitation extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_question_id", nullable = false)
    private ReviewQuestion reviewQuestion;

    /** 인용 조항 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_clause_id", nullable = false)
    private ContractClause contractClause;

    /** 인용 문장 */
    @Column(name = "quoted_text", columnDefinition = "text")
    private String quotedText;

    private ReviewQuestionCitation(ReviewQuestion reviewQuestion, ContractClause contractClause, String quotedText) {
        this.reviewQuestion = reviewQuestion;
        this.contractClause = contractClause;
        this.quotedText = quotedText;
    }

    public static ReviewQuestionCitation of(
            ReviewQuestion reviewQuestion, ContractClause contractClause, String quotedText) {
        return new ReviewQuestionCitation(reviewQuestion, contractClause, quotedText);
    }
}
