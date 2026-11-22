package com.visafe.domain.term.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.config.MinimumWageProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MinimumWageCheckerTest {

    private final MinimumWageChecker checker =
            new MinimumWageChecker(new MinimumWageProperties(Map.of(2026, 10_320, 2027, 10_700)));

    @Test
    void apiSpecExample() {
        ContractLaborTerms terms = terms(2026, 2_000_000);
        workHours(terms, "09:00", "18:00", 60, 5);

        MinimumWageChecker.Shortfall shortfall = checker.check(terms).orElseThrow();

        assertThat(shortfall.monthlyHours()).isEqualTo(209);
        assertThat(shortfall.hourlyWage()).isEqualTo(9_569);
        assertThat(shortfall.message()).isEqualTo("주 40시간(월 209시간) 기준 환산 시급 약 9,569원으로, 2026년 최저임금 시급 10,320원보다 낮습니다.");
        assertThat(shortfall.detail())
                .containsEntry("hourlyWage", 9_569)
                .containsEntry("minimumWage", 10_320)
                .containsEntry("year", 2026);
    }

    @Test
    void comparesWithMinimumWageOfStartYear() {
        ContractLaborTerms terms = terms(2027, 2_200_000);

        // 2,200,000 ÷ 209 = 10,526원: 2026년(10,320원)은 넘지만 2027년(10,700원)에는 못 미친다.
        assertThat(checker.check(terms))
                .hasValueSatisfying(
                        shortfall -> assertThat(shortfall.minimumWage()).isEqualTo(10_700));
        assertThat(checker.check(terms(2026, 2_200_000))).isEmpty();
    }

    @Test
    void missingWorkHoursMeanFullTime() {
        assertThat(checker.check(terms(2026, 2_000_000)))
                .hasValueSatisfying(
                        shortfall -> assertThat(shortfall.monthlyHours()).isEqualTo(209));
    }

    @Test
    void partTimeUsesItsOwnHours() {
        ContractLaborTerms terms = terms(2026, 1_000_000);
        workHours(terms, "09:00", "13:00", 0, 5);

        // 주 20시간 + 주휴 4시간 → 월 104시간, 1,000,000 ÷ 104 = 9,615원
        MinimumWageChecker.Shortfall shortfall = checker.check(terms).orElseThrow();
        assertThat(shortfall.monthlyHours()).isEqualTo(104);
        assertThat(shortfall.hourlyWage()).isEqualTo(9_615);
        assertThat(shortfall.message()).startsWith("주 20시간(월 104시간)");
    }

    @Test
    void weeklyHoursAreCappedAtFortyAndOvernightShiftCounts() {
        ContractLaborTerms longDays = terms(2026, 2_000_000);
        workHours(longDays, "08:00", "20:00", 60, 6);
        ContractLaborTerms night = terms(2026, 2_000_000);
        workHours(night, "22:00", "07:00", 60, 5);

        assertThat(checker.check(longDays).orElseThrow().weeklyHours()).isEqualTo(40);
        assertThat(checker.check(night).orElseThrow().weeklyHours()).isEqualTo(40);
    }

    @Test
    void noWarningWithoutWageStartDateOrConfiguredYear() {
        assertThat(checker.check(terms(2030, 1_000_000))).isEmpty();
        ContractLaborTerms noWage = terms(2026, 0);
        noWage.changeValue(TermField.MONTHLY_BASE_WAGE, null);
        assertThat(checker.check(noWage)).isEmpty();
        ContractLaborTerms noStart = terms(2026, 1_000_000);
        noStart.changeValue(TermField.START_DATE, null);
        assertThat(checker.check(noStart)).isEmpty();
    }

    private static ContractLaborTerms terms(int startYear, int monthlyWage) {
        ContractLaborTerms terms =
                ContractLaborTerms.empty(ContractFixtures.uploaded().getCurrentVersion());
        terms.changeValue(TermField.START_DATE, LocalDate.of(startYear, 1, 2));
        terms.changeValue(TermField.MONTHLY_BASE_WAGE, monthlyWage);
        return terms;
    }

    private static void workHours(ContractLaborTerms terms, String start, String end, int breakMinutes, int days) {
        terms.changeValue(TermField.WORK_START_TIME, LocalTime.parse(start));
        terms.changeValue(TermField.WORK_END_TIME, LocalTime.parse(end));
        terms.changeValue(TermField.BREAK_MINUTES, breakMinutes);
        terms.changeValue(TermField.WORK_DAYS_PER_WEEK, days);
    }
}
