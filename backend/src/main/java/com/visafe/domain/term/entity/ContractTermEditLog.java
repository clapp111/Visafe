package com.visafe.domain.term.entity;

import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
 * C. 항목 수정 이력 (contract_term_edit_logs) — 추가 전용. DB 트리거가 수정·삭제를 막는다.
 * 값은 문자열 표기로 남기며, 수정 전후 값이 같으면 남기지 않는다. (ck_contract_term_edit_logs__changed)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "contract_term_edit_logs")
public class ContractTermEditLog extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    @Convert(converter = TermFieldConverter.class)
    @Column(name = "field_name", nullable = false, length = 50)
    private TermField fieldName;

    /** 수정 전 */
    @Column(name = "before_value", columnDefinition = "text")
    private String beforeValue;

    /** 수정 후 */
    @Column(name = "after_value", columnDefinition = "text")
    private String afterValue;

    /** 수정한 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edited_by_member_id", nullable = false)
    private Member editedBy;

    private ContractTermEditLog(
            ContractVersion contractVersion,
            TermField fieldName,
            String beforeValue,
            String afterValue,
            Member editedBy) {
        this.contractVersion = contractVersion;
        this.fieldName = fieldName;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.editedBy = editedBy;
    }

    public static ContractTermEditLog of(
            ContractVersion contractVersion,
            TermField fieldName,
            String beforeValue,
            String afterValue,
            Member editedBy) {
        if (Objects.equals(beforeValue, afterValue)) {
            throw new IllegalArgumentException("값이 바뀌지 않은 수정은 기록하지 않습니다.");
        }
        return new ContractTermEditLog(contractVersion, fieldName, beforeValue, afterValue, editedBy);
    }
}
