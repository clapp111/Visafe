package com.visafe.domain.authority.entity;

import com.visafe.domain.authority.enums.ReviewDecision;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.enums.MemberRole;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * G. 기관 검토 (authority_reviews) — 선택 단계. requires_authority_review가 true인 계약만 쓴다.
 * 회차마다 검토 1건이며, 검토자는 ADMIN, 반려에는 사유가 필요하다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "authority_reviews")
public class AuthorityReview extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false, unique = true)
    private ContractVersion contractVersion;

    /** 검토자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_member_id", nullable = false)
    private Member reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private ReviewDecision decision;

    /** 반려 사유 */
    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    private AuthorityReview(ContractVersion contractVersion, Member reviewer, ReviewDecision decision, String reason) {
        if (!reviewer.hasRole(MemberRole.ADMIN)) {
            throw new IllegalArgumentException("기관 검토는 ADMIN 회원만 할 수 있습니다.");
        }
        this.contractVersion = contractVersion;
        this.reviewer = reviewer;
        this.decision = decision;
        this.reason = reason;
    }

    public static AuthorityReview approve(ContractVersion contractVersion, Member reviewer, String comment) {
        return new AuthorityReview(contractVersion, reviewer, ReviewDecision.APPROVED, comment);
    }

    public static AuthorityReview reject(ContractVersion contractVersion, Member reviewer, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려에는 사유가 필요합니다.");
        }
        return new AuthorityReview(contractVersion, reviewer, ReviewDecision.REJECTED, reason);
    }
}
