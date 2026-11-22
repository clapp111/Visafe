package com.visafe.domain.term;

import com.visafe.domain.term.enums.TermField;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * 항목 값 문자열을 항목 타입(문자열, 날짜, 시각, 정수)으로 바꾸고 DB 제약(contract_labor_terms의 길이·CHECK)과 같은 규칙으로 검사한다.
 * 종료일 ≥ 시작일처럼 두 항목을 함께 보는 검사는 하지 않는다.
 */
public final class TermValueParser {

    private TermValueParser() {}

    /**
     * 값 문자열을 항목 타입으로 바꾼다.
     *
     * @param field 항목
     * @param raw 값 문자열 (앞뒤 공백 무시)
     * @return 항목 타입으로 바꾼 값
     * @throws IllegalArgumentException 비어 있거나 형식·범위가 틀린 경우 (메시지에 이유)
     */
    public static Object parse(TermField field, String raw) {
        String value = raw == null ? "" : raw.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("값이 비어 있습니다.");
        }
        return switch (field) {
            case EMPLOYER_NAME, WORKER_NAME, PAY_METHOD -> text(value, 100);
            case REPRESENTATIVE_NAME, WORK_DAYS_TEXT, WEEKLY_HOLIDAY -> text(value, 50);
            case EMPLOYER_ADDRESS, WORK_PLACE, ACCOMMODATION -> text(value, 255);
            case JOB_DESCRIPTION -> value;
            case WORKER_NATIONALITY -> nationality(value);
            case START_DATE, END_DATE -> date(value);
            case WORK_START_TIME, WORK_END_TIME -> time(value);
            case BREAK_MINUTES -> integer(value, 0, 1440);
            case WORK_DAYS_PER_WEEK -> integer(value, 1, 7);
            case MONTHLY_BASE_WAGE -> integer(value, 0, Integer.MAX_VALUE);
            case PAY_DAY -> integer(value, 1, 31);
        };
    }

    private static String text(String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(maxLength + "자를 넘을 수 없습니다.");
        }
        return value;
    }

    private static String nationality(String value) {
        if (!value.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("국적은 ISO 2자리 대문자여야 합니다.");
        }
        return value;
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식(2027-01-02)이 아닙니다.", e);
        }
    }

    private static LocalTime time(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("시각 형식(09:00)이 아닙니다.", e);
        }
    }

    private static Integer integer(String value, int min, int max) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("정수가 아닙니다.", e);
        }
        if (number < min || number > max) {
            throw new IllegalArgumentException(min + " 이상 " + max + " 이하여야 합니다.");
        }
        return number;
    }
}
