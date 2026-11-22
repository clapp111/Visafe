package com.visafe.domain.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.visafe.domain.term.enums.TermField;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TermValueParserTest {

    @Test
    void parsesEachTypeAndTrims() {
        assertThat(TermValueParser.parse(TermField.EMPLOYER_NAME, " 한빛정밀 ")).isEqualTo("한빛정밀");
        assertThat(TermValueParser.parse(TermField.START_DATE, "2027-01-02")).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(TermValueParser.parse(TermField.WORK_START_TIME, "09:00")).isEqualTo(LocalTime.of(9, 0));
        assertThat(TermValueParser.parse(TermField.MONTHLY_BASE_WAGE, "2000000"))
                .isEqualTo(2_000_000);
        assertThat(TermValueParser.parse(TermField.WORKER_NATIONALITY, "VN")).isEqualTo("VN");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "BREAK_MINUTES|-1",
                "BREAK_MINUTES|1441",
                "WORK_DAYS_PER_WEEK|0",
                "WORK_DAYS_PER_WEEK|8",
                "PAY_DAY|32",
                "MONTHLY_BASE_WAGE|-100",
                "MONTHLY_BASE_WAGE|2,000,000",
                "WORKER_NATIONALITY|vn",
                "WORKER_NATIONALITY|VNM",
                "START_DATE|2027/01/02",
                "WORK_START_TIME|9시",
                "EMPLOYER_NAME|'   '"
            })
    void rejectsValuesThatBreakDatabaseRules(TermField field, String raw) {
        assertThatThrownBy(() -> TermValueParser.parse(field, raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTextLongerThanColumn() {
        assertThat(TermValueParser.parse(TermField.REPRESENTATIVE_NAME, "가".repeat(50)))
                .isEqualTo("가".repeat(50));
        assertThatThrownBy(() -> TermValueParser.parse(TermField.REPRESENTATIVE_NAME, "가".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(TermValueParser.parse(TermField.JOB_DESCRIPTION, "가".repeat(1000)))
                .isEqualTo("가".repeat(1000));
    }

    @Test
    void nullIsRejected() {
        assertThatThrownBy(() -> TermValueParser.parse(TermField.PAY_METHOD, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
