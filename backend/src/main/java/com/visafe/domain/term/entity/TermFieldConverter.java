package com.visafe.domain.term.entity;

import com.visafe.domain.term.enums.TermField;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link TermField} ↔ field_name 컬럼 값(contract_labor_terms 컬럼 이름) 변환.
 */
@Converter
public class TermFieldConverter implements AttributeConverter<TermField, String> {

    @Override
    public String convertToDatabaseColumn(TermField attribute) {
        return attribute == null ? null : attribute.getColumnName();
    }

    @Override
    public TermField convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TermField.fromColumnName(dbData);
    }
}
