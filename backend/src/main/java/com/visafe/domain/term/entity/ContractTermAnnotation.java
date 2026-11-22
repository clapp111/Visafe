package com.visafe.domain.term.entity;

import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.term.enums.ExtractionStatus;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.global.common.BaseTimeEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * C. 항목 부가 정보 (contract_term_annotations). 회차·항목마다 최대 1줄.
 * AI가 읽은 값, 값 출처(고용주 수정 여부), 읽지 못함·경고, 근거 조항을 담는다.
 * <p>
 * 읽지 못한(UNREAD) 항목에는 ai_value가 없다. (ck_contract_term_annotations__unread_has_no_ai_value)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "contract_term_annotations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contract_term_annotations__contract_version_id__field_name",
                    columnNames = {"contract_version_id", "field_name"})
        })
public class ContractTermAnnotation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    @Convert(converter = TermFieldConverter.class)
    @Column(name = "field_name", nullable = false, length = 50)
    private TermField fieldName;

    /** AI가 읽은 값 (문자열 표기) */
    @Column(name = "ai_value", columnDefinition = "text")
    private String aiValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_source", nullable = false, length = 20)
    private ValueSource valueSource = ValueSource.AI;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 10)
    private ExtractionStatus extractionStatus = ExtractionStatus.READ;

    /** 경고 코드 (문구는 서버에서 생성) */
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_code", length = 30)
    private WarningCode warningCode;

    /** 근거 조항 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_clause_id")
    private ContractClause contractClause;

    private ContractTermAnnotation(
            ContractVersion contractVersion,
            TermField fieldName,
            String aiValue,
            ExtractionStatus extractionStatus,
            ContractClause contractClause) {
        this.contractVersion = contractVersion;
        this.fieldName = fieldName;
        this.aiValue = aiValue;
        this.extractionStatus = extractionStatus;
        this.contractClause = contractClause;
    }

    /** AI가 값을 읽은 항목. */
    public static ContractTermAnnotation read(
            ContractVersion contractVersion, TermField fieldName, String aiValue, ContractClause contractClause) {
        return new ContractTermAnnotation(contractVersion, fieldName, aiValue, ExtractionStatus.READ, contractClause);
    }

    /** AI가 읽지 못한 항목. */
    public static ContractTermAnnotation unread(ContractVersion contractVersion, TermField fieldName) {
        return new ContractTermAnnotation(contractVersion, fieldName, null, ExtractionStatus.UNREAD, null);
    }

    /** 돌아온 계약의 새 회차로 부가 정보(AI 값, 값 출처, 읽음 여부, 경고, 근거 조항)를 그대로 옮긴다. */
    public ContractTermAnnotation copyTo(ContractVersion target) {
        ContractTermAnnotation copy =
                new ContractTermAnnotation(target, fieldName, aiValue, extractionStatus, contractClause);
        copy.valueSource = valueSource;
        copy.warningCode = warningCode;
        return copy;
    }

    /**
     * 고용주가 값을 고쳤거나 직접 입력했음을 표시한다. AI 값으로 되돌렸으면 다시 AI로 본다.
     * 고용주가 확인한 항목이므로 AI의 확인 필요(NEEDS_CHECK) 표시는 지운다.
     */
    public void markEditedByEmployer(boolean sameAsAiValue) {
        if (sameAsAiValue) {
            this.valueSource = ValueSource.AI;
        } else {
            this.valueSource = extractionStatus == ExtractionStatus.UNREAD
                    ? ValueSource.EMPLOYER_INPUT
                    : ValueSource.EMPLOYER_EDIT;
        }
        if (warningCode == WarningCode.NEEDS_CHECK) {
            this.warningCode = null;
        }
    }

    public void changeWarning(WarningCode warningCode) {
        this.warningCode = warningCode;
    }

    public boolean isUnread() {
        return extractionStatus == ExtractionStatus.UNREAD;
    }
}
