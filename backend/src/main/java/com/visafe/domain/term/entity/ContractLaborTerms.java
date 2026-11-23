package com.visafe.domain.term.entity;

import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/**
 * C. 근로계약 항목 (contract_labor_terms). 제출 회차(contract_versions)와 1:1.
 * <p>
 * 계약서에 적힌 값 그대로를 저장한다(사업체명·근로자 성명 등). 계정 정보(companies·members)와 달라도 그대로 둔다.
 * 모든 항목은 AI가 읽지 못할 수 있어 null을 허용하며, 필수 여부는 제출 시 서비스에서 검사한다.
 * <p>
 * 항목 하나를 이름으로 읽고 바꿀 때는 {@link #getValue(TermField)}, {@link #changeValue(TermField, Object)}를 쓴다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "contract_labor_terms")
public class ContractLaborTerms extends BaseTimeEntity {

    @Id
    @Column(name = "contract_version_id")
    private Long contractVersionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id")
    private ContractVersion contractVersion;

    /** 사업체명 */
    @Column(name = "employer_name", length = 100)
    private String employerName;

    /** 대표자 */
    @Column(name = "representative_name", length = 50)
    private String representativeName;

    /** 사업장 주소 */
    @Column(name = "employer_address", length = 255)
    private String employerAddress;

    /** 근로자 성명 */
    @Column(name = "worker_name", length = 100)
    private String workerName;

    /** 국적 (ISO 3166-1) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "worker_nationality", length = 2)
    private String workerNationality;

    /** 계약 시작일 */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 계약 종료일 */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 근무 장소 */
    @Column(name = "work_place", length = 255)
    private String workPlace;

    /** 업무 내용 */
    @Column(name = "job_description", columnDefinition = "text")
    private String jobDescription;

    // 시각(TIME)은 기본 바인딩이 hibernate.jdbc.time_zone(UTC)으로 변환되어 JVM 시간대만큼 밀리므로, 변환 없이 그대로 저장한다.

    /** 시업 시각 */
    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    /** 종업 시각 */
    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    /** 휴게시간(분) */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "break_minutes")
    private Integer breakMinutes;

    /** 주 근무일 수 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "work_days_per_week")
    private Integer workDaysPerWeek;

    /** 근무일 원문 표현 */
    @Column(name = "work_days_text", length = 50)
    private String workDaysText;

    /** 주휴일 */
    @Column(name = "weekly_holiday", length = 50)
    private String weeklyHoliday;

    /** 월 기본급(원) */
    @Column(name = "monthly_base_wage")
    private Integer monthlyBaseWage;

    /** 임금 지급일 (1~31) */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "pay_day")
    private Integer payDay;

    /** 지급 방법 */
    @Column(name = "pay_method", length = 100)
    private String payMethod;

    /** 숙식 제공 */
    @Column(name = "accommodation", length = 255)
    private String accommodation;

    private ContractLaborTerms(ContractVersion contractVersion) {
        this.contractVersion = contractVersion;
    }

    /** 빈 항목으로 만든 뒤 {@link #changeValue}로 AI 추출 값을 채운다. */
    public static ContractLaborTerms empty(ContractVersion contractVersion) {
        return new ContractLaborTerms(contractVersion);
    }

    public Object getValue(TermField field) {
        return switch (field) {
            case EMPLOYER_NAME -> employerName;
            case REPRESENTATIVE_NAME -> representativeName;
            case EMPLOYER_ADDRESS -> employerAddress;
            case WORKER_NAME -> workerName;
            case WORKER_NATIONALITY -> workerNationality;
            case START_DATE -> startDate;
            case END_DATE -> endDate;
            case WORK_PLACE -> workPlace;
            case JOB_DESCRIPTION -> jobDescription;
            case WORK_START_TIME -> workStartTime;
            case WORK_END_TIME -> workEndTime;
            case BREAK_MINUTES -> breakMinutes;
            case WORK_DAYS_PER_WEEK -> workDaysPerWeek;
            case WORK_DAYS_TEXT -> workDaysText;
            case WEEKLY_HOLIDAY -> weeklyHoliday;
            case MONTHLY_BASE_WAGE -> monthlyBaseWage;
            case PAY_DAY -> payDay;
            case PAY_METHOD -> payMethod;
            case ACCOMMODATION -> accommodation;
        };
    }

    /**
     * 항목 값을 바꾼다. 값의 타입은 {@link TermField#getValueType()}과 같아야 한다.
     * 제출된 회차의 항목은 바꿀 수 없다.
     */
    public void changeValue(TermField field, Object value) {
        if (!contractVersion.isDraft()) {
            throw new IllegalStateException("제출된 회차의 항목은 바꿀 수 없습니다.");
        }
        if (value != null && !field.getValueType().isInstance(value)) {
            throw new IllegalArgumentException(
                    field.getColumnName() + " 값은 " + field.getValueType().getSimpleName() + " 타입이어야 합니다.");
        }
        switch (field) {
            case EMPLOYER_NAME -> employerName = (String) value;
            case REPRESENTATIVE_NAME -> representativeName = (String) value;
            case EMPLOYER_ADDRESS -> employerAddress = (String) value;
            case WORKER_NAME -> workerName = (String) value;
            case WORKER_NATIONALITY -> workerNationality = (String) value;
            case START_DATE -> startDate = (LocalDate) value;
            case END_DATE -> endDate = (LocalDate) value;
            case WORK_PLACE -> workPlace = (String) value;
            case JOB_DESCRIPTION -> jobDescription = (String) value;
            case WORK_START_TIME -> workStartTime = (LocalTime) value;
            case WORK_END_TIME -> workEndTime = (LocalTime) value;
            case BREAK_MINUTES -> breakMinutes = (Integer) value;
            case WORK_DAYS_PER_WEEK -> workDaysPerWeek = (Integer) value;
            case WORK_DAYS_TEXT -> workDaysText = (String) value;
            case WEEKLY_HOLIDAY -> weeklyHoliday = (String) value;
            case MONTHLY_BASE_WAGE -> monthlyBaseWage = (Integer) value;
            case PAY_DAY -> payDay = (Integer) value;
            case PAY_METHOD -> payMethod = (String) value;
            case ACCOMMODATION -> accommodation = (String) value;
        }
    }
}
