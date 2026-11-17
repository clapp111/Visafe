package com.visafe.domain.contract.entity;

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
import org.hibernate.annotations.Immutable;

/**
 * C. 원문 조항 (contract_clauses). 원본 전문을 조항 단위로 읽어 저장한다.
 * 전문 번역(translation_clauses), 질문하기 근거 인용, 항목 근거 조항에 쓴다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(
        name = "contract_clauses",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contract_clauses__contract_file_id__seq",
                    columnNames = {"contract_file_id", "seq"})
        })
public class ContractClause extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원본 파일 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_file_id", nullable = false)
    private ContractFile contractFile;

    /** 읽는 순서 (1부터) */
    @Column(name = "seq", nullable = false)
    private int seq;

    /** 조항 번호 (제3조 등) */
    @Column(name = "clause_no", length = 20)
    private String clauseNo;

    /** 쪽 */
    @Column(name = "page_no")
    private Integer pageNo;

    /** 원문 (OCR) */
    @Column(name = "text_ko", nullable = false, columnDefinition = "text")
    private String textKo;

    private ContractClause(ContractFile contractFile, int seq, String clauseNo, Integer pageNo, String textKo) {
        this.contractFile = contractFile;
        this.seq = seq;
        this.clauseNo = clauseNo;
        this.pageNo = pageNo;
        this.textKo = textKo;
    }

    public static ContractClause create(
            ContractFile contractFile, int seq, String clauseNo, Integer pageNo, String textKo) {
        if (seq < 1) {
            throw new IllegalArgumentException("조항 순서는 1부터 시작합니다.");
        }
        return new ContractClause(contractFile, seq, clauseNo, pageNo, textKo);
    }
}
