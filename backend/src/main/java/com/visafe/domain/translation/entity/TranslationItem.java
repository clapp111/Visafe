package com.visafe.domain.translation.entity;

import com.visafe.domain.term.entity.TermFieldConverter;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * D. 항목 번역·쉬운 설명 (translation_items). 번역본·항목마다 1줄.
 * easy_explanation_ko는 고용주 검토용이라 translation_hash 계산에서 뺀다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "translation_items",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_translation_items__translation_id__field_name",
                    columnNames = {"translation_id", "field_name"})
        })
public class TranslationItem extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    @Convert(converter = TermFieldConverter.class)
    @Column(name = "field_name", nullable = false, length = 50)
    private TermField fieldName;

    /** 번역된 값 */
    @Column(name = "translated_value", columnDefinition = "text")
    private String translatedValue;

    /** 모국어 쉬운 설명 */
    @Column(name = "easy_explanation", columnDefinition = "text")
    private String easyExplanation;

    /** 검토용 한국어 설명 (해시 제외) */
    @Column(name = "easy_explanation_ko", columnDefinition = "text")
    private String easyExplanationKo;

    @Builder
    private TranslationItem(
            Translation translation,
            TermField fieldName,
            String translatedValue,
            String easyExplanation,
            String easyExplanationKo) {
        this.translation = translation;
        this.fieldName = fieldName;
        this.translatedValue = translatedValue;
        this.easyExplanation = easyExplanation;
        this.easyExplanationKo = easyExplanationKo;
    }
}
