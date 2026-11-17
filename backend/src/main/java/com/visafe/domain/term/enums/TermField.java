package com.visafe.domain.term.enums;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 근로계약 항목. DB에는 {@code contract_labor_terms}의 컬럼 이름(columnName)으로 저장되며,
 * contract_term_annotations·contract_term_edit_logs·translation_items·review_report_items의
 * field_name이 이 값을 가리킨다. (각 테이블의 field_name CHECK와 목록이 같아야 한다)
 */
@Getter
@RequiredArgsConstructor
public enum TermField {
    EMPLOYER_NAME("employer_name", "사업체명", String.class),
    REPRESENTATIVE_NAME("representative_name", "대표자", String.class),
    EMPLOYER_ADDRESS("employer_address", "사업장 주소", String.class),
    WORKER_NAME("worker_name", "근로자 성명", String.class),
    WORKER_NATIONALITY("worker_nationality", "국적", String.class),
    START_DATE("start_date", "계약 시작일", LocalDate.class),
    END_DATE("end_date", "계약 종료일", LocalDate.class),
    WORK_PLACE("work_place", "근무 장소", String.class),
    JOB_DESCRIPTION("job_description", "업무 내용", String.class),
    WORK_START_TIME("work_start_time", "시업 시각", LocalTime.class),
    WORK_END_TIME("work_end_time", "종업 시각", LocalTime.class),
    BREAK_MINUTES("break_minutes", "휴게시간(분)", Integer.class),
    WORK_DAYS_PER_WEEK("work_days_per_week", "주 근무일 수", Integer.class),
    WORK_DAYS_TEXT("work_days_text", "근무일 원문 표현", String.class),
    WEEKLY_HOLIDAY("weekly_holiday", "주휴일", String.class),
    MONTHLY_BASE_WAGE("monthly_base_wage", "월 기본급(원)", Integer.class),
    PAY_DAY("pay_day", "임금 지급일", Integer.class),
    PAY_METHOD("pay_method", "지급 방법", String.class),
    ACCOMMODATION("accommodation", "숙식 제공", String.class);

    private static final Map<String, TermField> BY_COLUMN_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TermField::getColumnName, Function.identity()));

    /** contract_labor_terms 컬럼 이름 (DB 저장 값, items_hash 키) */
    private final String columnName;
    /** 화면 표시 이름 */
    private final String label;
    /** 값 타입 */
    private final Class<?> valueType;

    public static TermField fromColumnName(String columnName) {
        TermField field = BY_COLUMN_NAME.get(columnName);
        if (field == null) {
            throw new IllegalArgumentException("알 수 없는 근로계약 항목입니다: " + columnName);
        }
        return field;
    }
}
