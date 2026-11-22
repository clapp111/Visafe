package com.visafe.domain.term.service;

import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.config.MinimumWageProperties;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 월 기본급을 시급으로 바꿔 계약 시작 연도의 최저임금과 비교한다.
 * <p>
 * 월 소정근로시간 = (주 소정근로시간 + 주휴시간) × 365 ÷ 7 ÷ 12를 반올림한 값이다. (주 40시간이면 209시간)
 * 주 소정근로시간은 (종업 − 시업 − 휴게) × 주 근무일 수이며 40시간을 넘지 않는다.
 * 근로시간 항목이 비어 있거나 계산할 수 없으면 주 40시간으로 본다. 경고는 저장하지 않고 읽을 때마다 계산한다.
 */
@Component
@RequiredArgsConstructor
public class MinimumWageChecker {

    private static final double FULL_TIME_WEEKLY_HOURS = 40;
    /** 주휴시간을 주는 주 소정근로시간 하한 */
    private static final double WEEKLY_HOLIDAY_MIN_HOURS = 15;

    private final MinimumWageProperties minimumWageProperties;

    /**
     * 최저임금에 못 미치는지 확인한다.
     *
     * @param terms 근로계약 항목
     * @return 못 미치면 계산 결과, 월 기본급·시작일이 없거나 그 연도 최저임금 설정이 없거나 충분하면 빈 값
     */
    public Optional<Shortfall> check(ContractLaborTerms terms) {
        if (!(terms.getValue(TermField.MONTHLY_BASE_WAGE) instanceof Integer wage)
                || !(terms.getValue(TermField.START_DATE) instanceof LocalDate start)) {
            return Optional.empty();
        }
        Integer minimumWage = minimumWageProperties.hourly().get(start.getYear());
        if (minimumWage == null) {
            return Optional.empty();
        }
        double weeklyHours = Math.min(weeklyHours(terms), FULL_TIME_WEEKLY_HOURS);
        double weeklyHolidayHours = weeklyHours >= WEEKLY_HOLIDAY_MIN_HOURS ? weeklyHours / 5 : 0;
        int monthlyHours = (int) Math.round((weeklyHours + weeklyHolidayHours) * 365 / 7 / 12);
        int hourlyWage = wage / monthlyHours;
        if (hourlyWage >= minimumWage) {
            return Optional.empty();
        }
        return Optional.of(new Shortfall(weeklyHours, monthlyHours, hourlyWage, minimumWage, start.getYear()));
    }

    /** (종업 − 시업 − 휴게) × 주 근무일 수. 계산할 수 없으면 주 40시간. 종업이 시업보다 이르면 다음 날로 본다. */
    public static double weeklyHours(ContractLaborTerms terms) {
        if (!(terms.getValue(TermField.WORK_START_TIME) instanceof LocalTime start)
                || !(terms.getValue(TermField.WORK_END_TIME) instanceof LocalTime end)
                || !(terms.getValue(TermField.BREAK_MINUTES) instanceof Integer breakMinutes)
                || !(terms.getValue(TermField.WORK_DAYS_PER_WEEK) instanceof Integer days)) {
            return FULL_TIME_WEEKLY_HOURS;
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            minutes += 24 * 60;
        }
        long dailyMinutes = minutes - breakMinutes;
        return dailyMinutes > 0 ? dailyMinutes * days / 60.0 : FULL_TIME_WEEKLY_HOURS;
    }

    /**
     * 최저임금 미달 계산 결과.
     *
     * @param weeklyHours 주 소정근로시간
     * @param monthlyHours 월 소정근로시간
     * @param hourlyWage 환산 시급(원, 버림)
     * @param minimumWage 그 연도 최저임금 시급(원)
     * @param year 기준 연도 (계약 시작일의 연도)
     */
    public record Shortfall(double weeklyHours, int monthlyHours, int hourlyWage, int minimumWage, int year) {

        /** 고용주 검토 화면에 보여 줄 문구. */
        public String message() {
            DecimalFormat won = new DecimalFormat("#,###");
            DecimalFormat hours = new DecimalFormat("0.#");
            return "주 %s시간(월 %d시간) 기준 환산 시급 약 %s원으로, %d년 최저임금 시급 %s원보다 낮습니다."
                    .formatted(
                            hours.format(weeklyHours),
                            monthlyHours,
                            won.format(hourlyWage),
                            year,
                            won.format(minimumWage));
        }

        public Map<String, Object> detail() {
            return Map.of("hourlyWage", hourlyWage, "minimumWage", minimumWage, "year", year);
        }
    }
}
