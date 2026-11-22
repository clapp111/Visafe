package com.visafe.domain.term.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TermFieldTest {

    @Test
    void keyIsCamelCaseOfColumnName() {
        assertThat(TermField.BREAK_MINUTES.getKey()).isEqualTo("breakMinutes");
        assertThat(TermField.MONTHLY_BASE_WAGE.getKey()).isEqualTo("monthlyBaseWage");
        assertThat(TermField.EMPLOYER_NAME.getKey()).isEqualTo("employerName");
        assertThat(TermField.ACCOMMODATION.getKey()).isEqualTo("accommodation");
    }

    @Test
    void everyKeyFindsItsField() {
        for (TermField field : TermField.values()) {
            assertThat(TermField.fromKey(field.getKey())).contains(field);
        }
        assertThat(TermField.fromKey("break_minutes")).isEmpty();
        assertThat(TermField.fromKey("unknown")).isEmpty();
    }

    @Test
    void typesFollowValueClass() {
        assertThat(TermField.START_DATE.getType()).isEqualTo(TermValueType.DATE);
        assertThat(TermField.WORK_START_TIME.getType()).isEqualTo(TermValueType.TIME);
        assertThat(TermField.PAY_DAY.getType()).isEqualTo(TermValueType.INTEGER);
        assertThat(TermField.PAY_METHOD.getType()).isEqualTo(TermValueType.STRING);
    }

    @Test
    void optionalFieldsMatchApiSpecAppendixA() {
        assertThat(Arrays.stream(TermField.values()).filter(field -> !field.isRequired()))
                .containsExactly(
                        TermField.WORKER_NATIONALITY,
                        TermField.END_DATE,
                        TermField.WORK_DAYS_TEXT,
                        TermField.ACCOMMODATION);
    }
}
