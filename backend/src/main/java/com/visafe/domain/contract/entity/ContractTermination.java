package com.visafe.domain.contract.entity;

import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
import com.visafe.domain.member.entity.Member;
import com.visafe.global.common.BaseTimeEntity;
import com.visafe.global.common.Entities;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F. 계약 종료 (contract_terminations).
 * <ul>
 *   <li>MUTUAL(합의 종료)만 요청자·요청 일시가 있고 REQUESTED → AGREED/DECLINED/CANCELED로 진행한다.</li>
 *   <li>EXPIRED·REPLACED는 요청 없이 곧바로 AGREED로 만든다. (ck_contract_terminations__auto_agreed)</li>
 *   <li>계약당 진행 중(REQUESTED·AGREED)인 종료는 하나뿐이다. (uq_contract_terminations_active)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "contract_terminations")
public class ContractTermination extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 10)
    private TerminationReasonType reasonType;

    /** 사유 */
    @Column(name = "reason_text", columnDefinition = "text")
    private String reasonText;

    /** 종료일 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TerminationStatus status = TerminationStatus.REQUESTED;

    /** 요청한 사람 (합의 종료만) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_member_id")
    private Member requestedBy;

    /** 요청 일시 */
    @Column(name = "requested_at")
    private Instant requestedAt;

    /** 동의·거절한 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_member_id")
    private Member respondedBy;

    /** 응답 일시 */
    @Column(name = "responded_at")
    private Instant respondedAt;

    private ContractTermination(
            Contract contract,
            TerminationReasonType reasonType,
            String reasonText,
            LocalDate endDate,
            TerminationStatus status) {
        this.contract = contract;
        this.reasonType = reasonType;
        this.reasonText = reasonText;
        this.endDate = endDate;
        this.status = status;
    }

    /** 계약 당사자 한쪽이 합의 종료를 요청한다. */
    public static ContractTermination requestMutual(
            Contract contract, Member requester, String reasonText, LocalDate endDate, Instant requestedAt) {
        if (!contract.isParty(requester)) {
            throw new IllegalArgumentException("계약 당사자만 종료를 요청할 수 있습니다.");
        }
        ContractTermination termination = new ContractTermination(
                contract, TerminationReasonType.MUTUAL, reasonText, endDate, TerminationStatus.REQUESTED);
        termination.requestedBy = requester;
        termination.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        return termination;
    }

    /** 기간 만료 종료 (요청·동의 없이 곧바로 AGREED). */
    public static ContractTermination expired(Contract contract, LocalDate endDate) {
        return new ContractTermination(
                contract, TerminationReasonType.EXPIRED, null, endDate, TerminationStatus.AGREED);
    }

    /** 새 계약 확정으로 이전 계약을 대체 종료 (곧바로 AGREED). */
    public static ContractTermination replaced(Contract previousContract, LocalDate endDate, String reasonText) {
        return new ContractTermination(
                previousContract, TerminationReasonType.REPLACED, reasonText, endDate, TerminationStatus.AGREED);
    }

    public void agree(Member responder, Instant respondedAt) {
        respond(responder, respondedAt, TerminationStatus.AGREED);
    }

    public void decline(Member responder, Instant respondedAt) {
        respond(responder, respondedAt, TerminationStatus.DECLINED);
    }

    /** 요청자가 요청을 취소한다. */
    public void cancel() {
        requireRequested();
        this.status = TerminationStatus.CANCELED;
    }

    private void respond(Member responder, Instant respondedAt, TerminationStatus result) {
        requireRequested();
        if (!contract.isParty(responder) || Entities.isSame(requestedBy, responder, Member::getId)) {
            throw new IllegalArgumentException("요청하지 않은 계약 당사자만 응답할 수 있습니다.");
        }
        this.respondedBy = responder;
        this.respondedAt = Objects.requireNonNull(respondedAt, "respondedAt");
        this.status = result;
    }

    private void requireRequested() {
        if (reasonType != TerminationReasonType.MUTUAL || status != TerminationStatus.REQUESTED) {
            throw new IllegalStateException("요청 중인 합의 종료만 처리할 수 있습니다. 현재: " + reasonType + "/" + status);
        }
    }
}
