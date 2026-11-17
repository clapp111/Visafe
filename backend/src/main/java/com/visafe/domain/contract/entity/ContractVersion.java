package com.visafe.domain.contract.entity;

import com.visafe.domain.contract.enums.ContractVersionStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * B. 제출 회차 (contract_versions).
 * 근로자 신고로 돌아와 다시 제출하면 새 회차를 만든다. 항목·번역·질문·블록체인 기록은 회차에 붙는다.
 * <p>
 * DRAFT일 때만 제출 정보(submitted_*, items_hash)가 비어 있다. (ck_contract_versions__submitted_fields)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "contract_versions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contract_versions__contract_id__version_no",
                    columnNames = {"contract_id", "version_no"})
        })
public class ContractVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /** 회차 번호 (1부터) */
    @Column(name = "version_no", nullable = false)
    private int versionNo;

    /** 기준 원본 파일 (같은 계약의 파일, DB 복합 FK로 검사) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_file_id", nullable = false)
    private ContractFile contractFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractVersionStatus status = ContractVersionStatus.DRAFT;

    /** 항목 해시 (제출 시 계산) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "items_hash", length = 66)
    private String itemsHash;

    /** 제출한 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_member_id")
    private Member submittedBy;

    /** 제출 일시 */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    private ContractVersion(Contract contract, int versionNo, ContractFile contractFile) {
        this.contract = contract;
        this.versionNo = versionNo;
        this.contractFile = contractFile;
    }

    public static ContractVersion create(Contract contract, int versionNo, ContractFile contractFile) {
        if (versionNo < 1) {
            throw new IllegalArgumentException("회차 번호는 1부터 시작합니다.");
        }
        if (!Entities.isSame(contractFile.getContract(), contract, Contract::getId)) {
            throw new IllegalArgumentException("다른 계약의 원본 파일은 지정할 수 없습니다.");
        }
        return new ContractVersion(contract, versionNo, contractFile);
    }

    /** 고용주가 항목 검토를 마치고 제출한다. */
    public void submit(Member submittedBy, String itemsHash, Instant submittedAt) {
        requireStatus(ContractVersionStatus.DRAFT);
        this.submittedBy = Objects.requireNonNull(submittedBy, "submittedBy");
        this.itemsHash = Objects.requireNonNull(itemsHash, "itemsHash");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
        this.status = ContractVersionStatus.SUBMITTED;
    }

    /** 근로자 신고(또는 기관 반려)로 고용주에게 돌려보낸다. */
    public void markReturned() {
        requireStatus(ContractVersionStatus.SUBMITTED);
        this.status = ContractVersionStatus.RETURNED;
    }

    public void confirm() {
        requireStatus(ContractVersionStatus.SUBMITTED);
        this.status = ContractVersionStatus.CONFIRMED;
    }

    /** 다음 회차가 제출되어 이 회차를 대체한다. */
    public void supersede() {
        if (!EnumSet.of(ContractVersionStatus.SUBMITTED, ContractVersionStatus.RETURNED)
                .contains(status)) {
            throw new IllegalStateException("제출되었거나 돌아온 회차만 대체할 수 있습니다. 현재: " + status);
        }
        this.status = ContractVersionStatus.SUPERSEDED;
    }

    public boolean isDraft() {
        return status == ContractVersionStatus.DRAFT;
    }

    private void requireStatus(ContractVersionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("회차 상태가 " + expected + "여야 합니다. 현재: " + status);
        }
    }
}
