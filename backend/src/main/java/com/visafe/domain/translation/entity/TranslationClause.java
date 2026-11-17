package com.visafe.domain.translation.entity;

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
 * D. 조항 번역 (translation_clauses). 번역본마다 원문 조항 하나에 번역 1개.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "translation_clauses",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_translation_clauses__translation_id__contract_clause_id",
                    columnNames = {"translation_id", "contract_clause_id"})
        })
public class TranslationClause extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    /** 원문 조항 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_clause_id", nullable = false)
    private ContractClause contractClause;

    /** 번역문 */
    @Column(name = "translated_text", nullable = false, columnDefinition = "text")
    private String translatedText;

    private TranslationClause(Translation translation, ContractClause contractClause, String translatedText) {
        this.translation = translation;
        this.contractClause = contractClause;
        this.translatedText = translatedText;
    }

    public static TranslationClause of(Translation translation, ContractClause contractClause, String translatedText) {
        return new TranslationClause(translation, contractClause, translatedText);
    }
}
