package com.visafe.domain.term.enums;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 근로계약 항목. DB에는 {@code contract_labor_terms}의 컬럼 이름(columnName)으로 저장되며,
 * contract_term_annotations·contract_term_edit_logs·translation_items·review_report_items의
 * field_name이 이 값을 가리킨다. (각 테이블의 field_name CHECK와 목록이 같아야 한다)
 * API에서는 컬럼 이름의 camelCase를 키로 쓴다. ({@code break_minutes} → {@code breakMinutes})
 */
@Getter
@RequiredArgsConstructor
public enum TermField {
    EMPLOYER_NAME("employer_name", "사업체명", String.class, true),
    REPRESENTATIVE_NAME("representative_name", "대표자", String.class, true),
    EMPLOYER_ADDRESS("employer_address", "사업장 주소", String.class, true),
    WORKER_NAME("worker_name", "근로자 성명", String.class, true),
    WORKER_NATIONALITY("worker_nationality", "국적", String.class, false),
    START_DATE("start_date", "계약 시작일", LocalDate.class, true),
    END_DATE("end_date", "계약 종료일", LocalDate.class, false),
    WORK_PLACE("work_place", "근무 장소", String.class, true),
    JOB_DESCRIPTION("job_description", "업무 내용", String.class, true),
    WORK_START_TIME("work_start_time", "시업 시각", LocalTime.class, true),
    WORK_END_TIME("work_end_time", "종업 시각", LocalTime.class, true),
    BREAK_MINUTES("break_minutes", "휴게시간(분)", Integer.class, true),
    WORK_DAYS_PER_WEEK("work_days_per_week", "주 근무일 수", Integer.class, true),
    WORK_DAYS_TEXT("work_days_text", "근무일 원문 표현", String.class, false),
    WEEKLY_HOLIDAY("weekly_holiday", "주휴일", String.class, true),
    MONTHLY_BASE_WAGE("monthly_base_wage", "월 기본급(원)", Integer.class, true),
    PAY_DAY("pay_day", "임금 지급일", Integer.class, true),
    PAY_METHOD("pay_method", "지급 방법", String.class, true),
    ACCOMMODATION("accommodation", "숙식 제공", String.class, false);

    private static final Map<String, TermField> BY_COLUMN_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TermField::getColumnName, Function.identity()));

    private static final Map<String, TermField> BY_KEY =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(TermField::getKey, Function.identity()));

    /** contract_labor_terms 컬럼 이름 (DB 저장 값, items_hash 키) */
    private final String columnName;
    /** 화면 표시 이름 */
    private final String label;
    /** 값 타입 */
    private final Class<?> valueType;
    /** 제출 시 필수 여부 (API 명세 부록 A) */
    private final boolean required;

    public static TermField fromColumnName(String columnName) {
        TermField field = BY_COLUMN_NAME.get(columnName);
        if (field == null) {
            throw new IllegalArgumentException("알 수 없는 근로계약 항목입니다: " + columnName);
        }
        return field;
    }

    /** API 키(camelCase)로 항목을 찾는다. */
    public static Optional<TermField> fromKey(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    /** API에서 쓰는 키. 컬럼 이름의 camelCase다. */
    public String getKey() {
        StringBuilder key = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                key.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return key.toString();
    }

    /** API에서 쓰는 값 타입 이름. */
    public TermValueType getType() {
        if (valueType == LocalDate.class) {
            return TermValueType.DATE;
        }
        if (valueType == LocalTime.class) {
            return TermValueType.TIME;
        }
        if (valueType == Integer.class) {
            return TermValueType.INTEGER;
        }
        return TermValueType.STRING;
    }
}
