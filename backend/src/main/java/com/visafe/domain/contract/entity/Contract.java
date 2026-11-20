package com.visafe.domain.contract.entity;

import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.DocType;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.enums.MemberRole;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * B. 계약 (contracts).
 * <ul>
 *   <li>stage만 저장하고 status는 DB 생성 컬럼으로 계산한다. 코드에서는 {@link #getStatus()}가 stage에서 바로 계산한다.</li>
 *   <li>RECORDING·DONE·ENDED는 {@link #startRecording}·{@link #confirm}·{@link #terminate}로만 들어간다.
 *       확인번호·확정 일시·종료 일시를 stage와 함께 바꿔야 CHECK(ck_contracts__confirmation_no_stage,
 *       ck_contracts__confirmed_fields, ck_contracts__terminated_fields)를 지킬 수 있다.</li>
 *   <li>확인번호는 블록체인 기록을 시작할 때(RECORDING) 정한다. anchor_key가 keccak256(확인번호)라서,
 *       기록 중에 서버가 재시작되어도 같은 번호로 다시 시도해야 하기 때문이다.</li>
 *   <li>확정된 계약은 수정하지 않는다. 조건이 바뀌면 previousContract를 가진 새 계약을 만든다.</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "contracts",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_contracts__contract_no", columnNames = "contract_no"),
            @UniqueConstraint(name = "uq_contracts__confirmation_no", columnNames = "confirmation_no")
        })
public class Contract extends BaseTimeEntity {

    static final String STATUS_EXPRESSION =
            "CASE stage WHEN 'DONE' THEN 'CONFIRMED' WHEN 'ENDED' THEN 'TERMINATED' ELSE 'IN_PROGRESS' END";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 내부 계약 번호 (C-2026-04817) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "contract_no", nullable = false, length = 12)
    private String contractNo;

    /** 공개 확인번호 (VS-7K3F-9QXM), 확정 시 발급 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "confirmation_no", length = 12)
    private String confirmationNo;

    /** 사업주 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** 등록한 고용주 계정 (사업주 소속, DB 복합 FK로 검사) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_member_id", nullable = false)
    private Member employer;

    /** 근로자 계정 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_member_id", nullable = false)
    private Member worker;

    /** 대체한 이전 계약 (조건 변경 시) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id")
    private Contract previousContract;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private DocType docType = DocType.LABOR_CONTRACT;

    /** 번역 언어 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "target_language", nullable = false, length = 2)
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private ContractStage stage = ContractStage.UPLOADED;

    /**
     * 공개 상태 (DB 생성 컬럼). JPQL 조회 조건용이며, 읽을 때는 {@link #getStatus()}를 쓴다.
     */
    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @GeneratedColumn(STATUS_EXPRESSION)
    @Column(name = "status", length = 20)
    private ContractStatus status;

    /** 현재 제출 회차 (DB에서는 지연 검사 FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_contract_version_id")
    private ContractVersion currentVersion;

    /** 기관 검토 필요 여부 (선택 단계) */
    @Column(name = "requires_authority_review", nullable = false)
    private boolean requiresAuthorityReview;

    /** 확정 일시 */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /** 종료 일시 */
    @Column(name = "terminated_at")
    private Instant terminatedAt;

    private Contract(
            String contractNo,
            Company company,
            Member employer,
            Member worker,
            String targetLanguage,
            Contract previousContract,
            boolean requiresAuthorityReview) {
        this.contractNo = contractNo;
        this.company = company;
        this.employer = employer;
        this.worker = worker;
        this.targetLanguage = targetLanguage;
        this.previousContract = previousContract;
        this.requiresAuthorityReview = requiresAuthorityReview;
    }

    public static Contract create(
            String contractNo,
            Company company,
            Member employer,
            Member worker,
            String targetLanguage,
            Contract previousContract,
            boolean requiresAuthorityReview) {
        Objects.requireNonNull(company, "company");
        if (!employer.hasRole(MemberRole.EMPLOYER)
                || !Entities.isSame(employer.getCompany(), company, Company::getId)) {
            throw new IllegalArgumentException("고용주는 계약 사업주 소속의 EMPLOYER 회원이어야 합니다.");
        }
        if (!worker.hasRole(MemberRole.WORKER)) {
            throw new IllegalArgumentException("근로자는 WORKER 회원이어야 합니다.");
        }
        if (previousContract != null && previousContract.getStatus() != ContractStatus.CONFIRMED) {
            throw new IllegalArgumentException("확정된 계약만 새 계약으로 대체할 수 있습니다.");
        }
        return new Contract(
                contractNo, company, employer, worker, targetLanguage, previousContract, requiresAuthorityReview);
    }

    /** 공개 상태. stage에서 계산하므로 저장 전에도 항상 정확하다. */
    public ContractStatus getStatus() {
        return stage.toStatus();
    }

    /** RECORDING·DONE·ENDED를 제외한 단계로 이동한다. */
    public void changeStage(ContractStage next) {
        if (next == ContractStage.RECORDING || next == ContractStage.DONE || next == ContractStage.ENDED) {
            throw new IllegalArgumentException(
                    "RECORDING·DONE·ENDED는 startRecording()·confirm()·terminate()로만 바꿀 수 있습니다.");
        }
        if (stage == ContractStage.AWAITING_WORKER
                && next == ContractStage.AUTHORITY_REVIEW
                && !requiresAuthorityReview) {
            throw new IllegalStateException("기관 검토 대상이 아닌 계약입니다.");
        }
        requireTransition(next);
        this.stage = next;
    }

    /**
     * 확인번호를 정하고 블록체인 기록 단계(RECORDING)로 들어간다.
     * 근로자 확인 직후(기관 검토 없음) 또는 기관 승인 직후에 부른다.
     */
    public void startRecording(String confirmationNo) {
        if (stage == ContractStage.AWAITING_WORKER && requiresAuthorityReview) {
            throw new IllegalStateException("기관 검토를 거쳐야 하는 계약입니다.");
        }
        requireTransition(ContractStage.RECORDING);
        this.confirmationNo = Objects.requireNonNull(confirmationNo, "confirmationNo");
        this.stage = ContractStage.RECORDING;
    }

    /** 블록체인 기록이 블록에 포함되면 확정한다. 확인번호는 {@link #startRecording}에서 정한 값을 그대로 쓴다. */
    public void confirm(Instant confirmedAt) {
        requireTransition(ContractStage.DONE);
        if (confirmationNo == null) {
            throw new IllegalStateException("확인번호 없이 확정할 수 없습니다.");
        }
        this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
        this.stage = ContractStage.DONE;
    }

    /** 계약을 종료한다. 확인번호·확정 일시는 그대로 남는다. */
    public void terminate(Instant terminatedAt) {
        requireTransition(ContractStage.ENDED);
        this.terminatedAt = Objects.requireNonNull(terminatedAt, "terminatedAt");
        this.stage = ContractStage.ENDED;
    }

    public void changeCurrentVersion(ContractVersion version) {
        if (!Entities.isSame(version.getContract(), this, Contract::getId)) {
            throw new IllegalArgumentException("다른 계약의 회차는 현재 회차로 지정할 수 없습니다.");
        }
        this.currentVersion = version;
    }

    public boolean isParty(Member member) {
        return Entities.isSame(employer, member, Member::getId) || Entities.isSame(worker, member, Member::getId);
    }

    private void requireTransition(ContractStage next) {
        if (!stage.canTransitionTo(next)) {
            throw new IllegalStateException("계약 단계를 " + stage + "에서 " + next + "(으)로 바꿀 수 없습니다.");
        }
    }
}
